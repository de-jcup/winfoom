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


import org.apache.http.HttpException;
import org.apache.http.RequestLine;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.pac.PacScriptEvaluator;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;
import org.kpax.winfoom.util.InputOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

/**
 * Responsible for handling client's connection.
 */
@ThreadSafe
@Component
public class ClientConnectionHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private PacScriptEvaluator pacScriptEvaluator;

    @Autowired
    private ProxyBlacklist proxyBlacklist;

    @Autowired
    private ConnectionProcessorSelector connectionProcessorSelector;

    /**
     * Process the client connection with each available proxy.<br>
     * Un un-responding to connect proxy is blacklisted only if it is not the last
     * one available.<br>
     * <b>Note:</b> If the {@link ClientConnection} is successfully created,
     * a proper response <i>must</i> be sent to the client.
     *
     * @param socket the client's socket
     * @throws IOException
     * @throws HttpException
     */
    public void handleConnection(final Socket socket)
            throws Exception {
        ClientConnection.ClientConnectionBuilder clientConnectionBuilder =
                new ClientConnection.ClientConnectionBuilder().
                        withSocket(socket).
                        withProxyConfig(proxyConfig).
                        withSystemConfig(systemConfig).
                        withConnectionProcessorSelector(connectionProcessorSelector);
        if (proxyConfig.isAutoConfig()) {
            clientConnectionBuilder.withPacScriptEvaluator(pacScriptEvaluator);
            clientConnectionBuilder.withProxyBlacklist(proxyBlacklist);
        }

        final ClientConnection clientConnection = clientConnectionBuilder.build();
        RequestLine requestLine = clientConnection.getRequestLine();
        logger.debug("Handle request: {}", requestLine);
        try {
            clientConnection.process();
        } finally {
            InputOutputs.close(clientConnection);
        }
        logger.debug("Done handling request: {}", requestLine);
    }

}
