/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;

import org.kpax.winfoom.exception.PacFileException;
import org.kpax.winfoom.exception.PacScriptException;
import org.kpax.winfoom.pac.PacScriptEvaluator;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.functional.SupplierSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/**
 * Responsible for loading the PAC script file and executing the {@code findProxyForURL} method.
 */
@Component
class ProxyAutoConfig {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * The implementation of a PAC script evaluator.
     */
    private SupplierSingleton<PacScriptEvaluator> pacScriptEvaluatorSupplier =
            SupplierSingleton.of(() -> applicationContext.getBean(PacScriptEvaluator.class));

    /**
     * Call the {@code findProxyForURL} method within the PAC script file
     *
     * @param uri the request URI.
     * @return the list of {@link ProxyInfo}.
     * @throws PacFileException
     */
    List<ProxyInfo> findProxyForURL(URI uri) throws PacScriptException {
        String proxyLine = pacScriptEvaluatorSupplier.get().findProxyForURL(uri);
        logger.debug("proxyLine [{}]", proxyLine);
        return HttpUtils.parsePacProxyLine(proxyLine);
    }

    SupplierSingleton<PacScriptEvaluator> getPacScriptEvaluatorSupplier() {
        return pacScriptEvaluatorSupplier;
    }

    void reset() {
        pacScriptEvaluatorSupplier.reset();
    }
}
