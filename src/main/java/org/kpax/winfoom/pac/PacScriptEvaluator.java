/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.pac;

import org.kpax.winfoom.exception.PacFileException;
import org.kpax.winfoom.exception.PacScriptException;
import org.kpax.winfoom.proxy.ProxyInfo;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * PAC Script evaluator and executor.
 */
public interface PacScriptEvaluator {

    /**
     * Main entry point to JavaScript PAC script as defined by Netscape.
     * This is JavaScript function name {@code FindProxyForURL()}.
     */
    String STANDARD_PAC_MAIN_FUNCTION = "FindProxyForURL";
    /**
     * Main entry point to JavaScript PAC script for IPv6 support,
     * as defined by Microsoft.
     * This is JavaScript function name {@code FindProxyForURLEx()}.
     */
    String IPV6_AWARE_PAC_MAIN_FUNCTION = "FindProxyForURLEx";

    /**
     * <p>
     * The method calls the JavaScript {@code FindProxyForURL(url, host)}
     * function in the PAC script (or alternatively the
     * {@code FindProxyForURLEx(url, host)} function).
     *
     * @param uri URI to get proxies for.
     * @return The {@link ProxyInfo} list.
     * @throws PacScriptException when something does wrong with the JavaScript function's call.
     */
    List<ProxyInfo> findProxyForURL(URI uri) throws PacScriptException, PacFileException, IOException;

}
