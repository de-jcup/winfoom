/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.kpax.winfoom.pac;

import org.apache.commons.io.IOUtils;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.MissingResourceException;
import org.kpax.winfoom.exception.PacFileException;
import org.kpax.winfoom.exception.PacScriptException;
import org.kpax.winfoom.proxy.ProxyBlacklist;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.functional.DoubleExceptionSingletonSupplier;
import org.kpax.winfoom.util.functional.Resetable;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@ThreadSafe
@Order(3)
@Component
public class DefaultPacScriptEvaluator implements PacScriptEvaluator, Resetable {

    private final Logger logger = LoggerFactory.getLogger(DefaultPacScriptEvaluator.class);

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private DefaultPacHelperMethods pacHelperMethods;

    @Autowired
    private ProxyBlacklist proxyBlacklist;

    private final DoubleExceptionSingletonSupplier<PacScriptEngine, PacFileException, IOException> scriptEngineSupplier =
            new DoubleExceptionSingletonSupplier<PacScriptEngine, PacFileException, IOException>(this::createScriptEngine);

    private final SingletonSupplier<String> helperJSScriptSupplier = new SingletonSupplier<>(() -> {
        try {
            return IOUtils.toString(getClass().getClassLoader().
                    getResourceAsStream("javascript/pacFunctions.js"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new MissingResourceException("pacFunctions.js not found in classpath", e);
        }
    });

    /**
     * Load and parse the PAC script file.
     *
     * @return the {@link DefaultPacScriptEvaluator} instance.
     * @throws IOException
     */
    private String loadScript() throws IOException {
        URL url = proxyConfig.getProxyPacFileLocationAsURL();
        Assert.state(url != null, "No proxy PAC file location found");
        logger.info("Get PAC file from: {}", url);
        try (InputStream inputStream = url.openStream()) {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            logger.info("PAC content: {}", content);
            return content;
        }
    }

    private PacScriptEngine createScriptEngine() throws PacFileException, IOException {
        String pacSource = loadScript();
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");
            Assert.notNull(engine, "Nashorn engine not found");

            String[] allowedGlobals =
                    ("Object,Function,Array,String,Date,Number,BigInt,"
                            + "Boolean,RegExp,Math,JSON,NaN,Infinity,undefined,"
                            + "isNaN,isFinite,parseFloat,parseInt,encodeURI,"
                            + "encodeURIComponent,decodeURI,decodeURIComponent,eval,"
                            + "escape,unescape,"
                            + "Error,EvalError,RangeError,ReferenceError,SyntaxError,"
                            + "TypeError,URIError,ArrayBuffer,Int8Array,Uint8Array,"
                            + "Uint8ClampedArray,Int16Array,Uint16Array,Int32Array,"
                            + "Uint32Array,Float32Array,Float64Array,BigInt64Array,"
                            + "BigUint64Array,DataView,Map,Set,WeakMap,"
                            + "WeakSet,Symbol,Reflect,Proxy,Promise,SharedArrayBuffer,"
                            + "Atomics,console,performance,"
                            + "arguments").split(",");

            Object cleaner = engine.eval("(function(allowed) {\n"
                    + "   var names = Object.getOwnPropertyNames(this);\n"
                    + "   MAIN: for (var i = 0; i < names.length; i++) {\n"
                    + "     for (var j = 0; j < allowed.length; j++) {\n"
                    + "       if (names[i] === allowed[j]) {\n"
                    + "         continue MAIN;\n"
                    + "       }\n"
                    + "     }\n"
                    + "     delete this[names[i]];\n"
                    + "   }\n"
                    + "})");
            try {
                ((Invocable) engine).invokeMethod(cleaner, "call", null, allowedGlobals);
            } catch (NoSuchMethodException ex) {
                throw new ScriptException(ex);
            }

            engine.eval(pacSource);

            try {
                ((Invocable) engine).invokeMethod(engine.eval(helperJSScriptSupplier.get()), "call", null, pacHelperMethods);
            } catch (NoSuchMethodException ex) {
                throw new ScriptException(ex);
            }

            return new PacScriptEngine(engine);
        } catch (ScriptException e) {
            throw new PacFileException(e);
        }
    }

    @Override
    public List<ProxyInfo> findProxyForURL(URI uri) throws PacScriptException, PacFileException, IOException {
        PacScriptEngine scriptEngine = scriptEngineSupplier.get();
        try {
            Object obj = scriptEngine.findProxyForURL(HttpUtils.toStrippedURLStr(uri), uri.getHost());
            String proxyLine = Objects.toString(obj, null);
            logger.debug("proxyLine [{}]", proxyLine);
            return HttpUtils.parsePacProxyLine(proxyLine);
        } catch (Exception ex) {
            if (ex.getCause() != null) {
                if (ex.getCause() instanceof ClassNotFoundException) {
                    // Is someone trying to break out of the sandbox ?
                    logger.warn("The downloaded PAC script is attempting to access Java class [{}] " +
                            "which may be a sign of maliciousness. " +
                            "You should investigate this with your network administrator.", ex.getCause().getMessage());
                }
            }
            // other unforeseen errors
            throw new PacScriptException("Error when executing PAC script function: " + scriptEngine.jsMainFunction, ex);
        }
    }

    @Override
    public List<ProxyInfo> findActiveProxyForURL(URI uri) throws PacScriptException, PacFileException, IOException {
        List<ProxyInfo> proxies = findProxyForURL(uri);
        logger.debug("All proxies: {}", proxies);
        return proxyBlacklist.removeBlacklistedProxies(proxies);
    }

    private boolean isJsFunctionAvailable(ScriptEngine eng, String functionName) {
        // We want to test if the function is there, but without actually
        // invoking it.
        try {
            Object typeofCheck = eng.eval("(function(name) { return typeof this[name]; })");
            Object type = ((Invocable) eng).invokeMethod(typeofCheck, "call", null, functionName);
            return "function".equals(type);
        } catch (NoSuchMethodException | ScriptException ex) {
            logger.warn("Error on testing if the function is there", ex);
            return false;
        }
    }

    @Override
    public void close() {
        logger.debug("Reset the scriptEngineSupplier");
        scriptEngineSupplier.reset();
    }

    private class PacScriptEngine {
        private final Invocable invocable;
        private final String jsMainFunction;

        PacScriptEngine(ScriptEngine scriptEngine) throws PacFileException {
            this.invocable = (Invocable) scriptEngine;
            if (isJsFunctionAvailable(scriptEngine, PacScriptEvaluator.IPV6_AWARE_PAC_MAIN_FUNCTION)) {
                this.jsMainFunction = PacScriptEvaluator.IPV6_AWARE_PAC_MAIN_FUNCTION;
            } else if (isJsFunctionAvailable(scriptEngine, PacScriptEvaluator.STANDARD_PAC_MAIN_FUNCTION)) {
                this.jsMainFunction = PacScriptEvaluator.STANDARD_PAC_MAIN_FUNCTION;
            } else {
                throw new PacFileException("Function " + PacScriptEvaluator.STANDARD_PAC_MAIN_FUNCTION +
                        " or " + PacScriptEvaluator.IPV6_AWARE_PAC_MAIN_FUNCTION + " not found in PAC Script.");
            }
        }

        Object findProxyForURL(String url, String host) throws ScriptException, NoSuchMethodException {
            return invocable.invokeFunction(jsMainFunction, url, host);
        }
    }

}
