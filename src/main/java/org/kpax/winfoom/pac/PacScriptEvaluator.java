/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kpax.winfoom.pac;

import org.kpax.winfoom.exception.PacFileException;

import java.net.URI;

/**
 * PAC Script evaluator.
 */
public interface PacScriptEvaluator {

    /**
     * Returns the proxy/proxies appropriate to use for the given URI.
     *
     * <p>
     * The method calls the JavaScript {@code FindProxyForURL(url, host)}
     * function in the PAC script (or alternatively the
     * {@code FindProxyForURLEx(url, host)} function), parses the result and
     * returns it as a prioritized list of proxies.
     *
     * @param uri URI to get proxies for.
     * @return
     * @throws PacFileException when the result from the JavaScript function
     *                          cannot be interpreted.
     */
    String findProxyForURL(URI uri) throws PacFileException;

}
