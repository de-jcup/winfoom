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

package org.kpax.winfoom.proxy.processor;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.proxy.ClientConnection;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Process a {@link ClientConnection} with a certain {@link ProxyInfo}.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 4/13/2020
 */
public abstract class ClientConnectionProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Process the client's connection. That is:<br>
     * <ul>
     * <li>Prepare the client's request to make a remote HTTP request through the proxy or direct.</li>
     * <li>Make the remote HTTP request.</li>
     * <li>Give back to the client the resulted response.</li>
     * </ul>
     *
     * @param clientConnection the {@link ClientConnection} instance.
     * @param proxyInfo        The {@link ProxyInfo} used to make the remote HTTP request.
     * @param processingIndex
     * @throws HttpException         if a HTTP exception has occurred
     * @throws IOException           if an input/output error occurs
     */
    abstract void handleRequest(final ClientConnection clientConnection, final ProxyInfo proxyInfo, int processingIndex)
            throws IOException, HttpException;

    /**
     * Handle the exception thrown by {@link #handleRequest(ClientConnection, ProxyInfo, int)} method.
     * <p>The implementations are free to overwrite this method as needed.</p>
     * <p><b>Note: This method must commit the response or throw a {@link ProxyConnectException}</b></p>
     *
     * @param clientConnection the {@link ClientConnection} instance.
     * @param proxyInfo        The {@link ProxyInfo} used to make the remote HTTP request.
     * @param e                The exception thrown by {@link #handleRequest(ClientConnection, ProxyInfo, int)} method
     * @throws ProxyConnectException
     */
    void handleError(final ClientConnection clientConnection, final ProxyInfo proxyInfo, final Exception e) throws ProxyConnectException {
        clientConnection.writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * Call the {@link #handleRequest(ClientConnection, ProxyInfo, int)} method then {@link #handleError(ClientConnection, ProxyInfo, Exception)}
     * if an exception occurs.
     *
     * @param clientConnection the {@link ClientConnection} instance.
     * @param proxyInfo        The {@link ProxyInfo} used to make the remote HTTP request.
     * @param processingIndex
     * @throws ProxyConnectException
     */
    public final void process(final ClientConnection clientConnection, final ProxyInfo proxyInfo, int processingIndex) throws ProxyConnectException {
        logger.debug("Process {} for {}", clientConnection, proxyInfo);
        try {
            handleRequest(clientConnection, proxyInfo, processingIndex);
        } catch (Exception e) {
            logger.debug("Error on handling request", e);
            handleError(clientConnection, proxyInfo, e);
        }
    }
}
