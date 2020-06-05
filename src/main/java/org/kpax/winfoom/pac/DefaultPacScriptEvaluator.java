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
package org.kpax.winfoom.pac;

import org.apache.commons.io.IOUtils;
import org.kpax.winfoom.exception.PacFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DefaultPacScriptEvaluator implements PacScriptEvaluator {

    /**
     * Main entry point to JavaScript PAC script as defined by Netscape.
     * This is JavaScript function name {@code FindProxyForURL()}.
     */
    public static final String STANDARD_PAC_MAIN_FUNCTION = "FindProxyForURL";

    /**
     * Main entry point to JavaScript PAC script for IPv6 support,
     * as defined by Microsoft.
     * This is JavaScript function name {@code FindProxyForURLEx()}.
     */
    public static final String IPV6_AWARE_PAC_MAIN_FUNCTION = "FindProxyForURLEx";

    private final Logger logger = LoggerFactory.getLogger(DefaultPacScriptEvaluator.class);

    private final PacScriptEngine scriptEngine;

    private final boolean preferIPv6Addresses;

    public DefaultPacScriptEvaluator(String pacSourceCode, boolean preferIPv6Addresses) throws PacFileException {
        this.scriptEngine = getScriptEngine(pacSourceCode);
        this.preferIPv6Addresses = preferIPv6Addresses;
    }

    private PacScriptEngine getScriptEngine(String pacSource) throws PacFileException {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");
            if (engine == null) {
                throw new PacFileException("Nashorn engine not found");
            }
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

            String helperJSScript;
            try {
                helperJSScript = IOUtils.toString(getClass().getClassLoader().
                        getResourceAsStream("javascript/pacFunctions.js"), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new PacFileException("pacFunctions.js not found in classpath", e);
            }

            logger.debug("PAC Helper JavaScript :\n{}", helperJSScript);
            try {
                ((Invocable) engine).invokeMethod(engine.eval(helperJSScript), "call", null, new DefaultPacHelperMethods(preferIPv6Addresses));
            } catch (NoSuchMethodException ex) {
                throw new ScriptException(ex);
            }

            return new PacScriptEngine(engine);
        } catch (ScriptException e) {
            throw new PacFileException(e);
        }
    }

    @Override
    public String findProxyForURL(URI uri) throws PacFileException {
        try {
            Object obj = scriptEngine.findProxyForURL(PacUtils.toStrippedURLStr(uri), uri.getHost());
            return Objects.toString(obj, null);
        } catch (Exception ex) {
            if (ex.getCause() != null) {
                if (ex.getCause() instanceof ClassNotFoundException) {
                    // Is someone trying to break out of the sandbox ?
                    logger.warn("The downloaded PAC script is attempting to access Java class ''{}'' " +
                            "which may be a sign of maliciousness. " +
                            "You should investigate this with your network administrator.", ex.getCause().getMessage());
                }
            }
            // other unforeseen errors
            throw new PacFileException("Error when executing PAC script function " + scriptEngine.jsMainFunction, ex);
        }
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

    private class PacScriptEngine {
        private final Invocable invocable;
        private final String jsMainFunction;

        PacScriptEngine(ScriptEngine scriptEngine) throws PacFileException {
            this.invocable = (Invocable) scriptEngine;
            if (isJsFunctionAvailable(scriptEngine, IPV6_AWARE_PAC_MAIN_FUNCTION)) {
                this.jsMainFunction = IPV6_AWARE_PAC_MAIN_FUNCTION;
            } else if (isJsFunctionAvailable(scriptEngine, STANDARD_PAC_MAIN_FUNCTION)) {
                this.jsMainFunction = STANDARD_PAC_MAIN_FUNCTION;
            } else {
                throw new PacFileException("Function " + STANDARD_PAC_MAIN_FUNCTION +
                        " or " + IPV6_AWARE_PAC_MAIN_FUNCTION + " not found in PAC Script.");
            }
        }

        Object findProxyForURL(String url, String host) throws ScriptException, NoSuchMethodException {
            return invocable.invokeFunction(jsMainFunction, url, host);
        }
    }

}
