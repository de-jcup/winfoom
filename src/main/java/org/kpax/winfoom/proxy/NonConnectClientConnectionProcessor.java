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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Process any type of non-CONNECT request for any type of proxy.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 4/13/2020
 */
@ThreadSafe
@Component
class NonConnectClientConnectionProcessor implements ClientConnectionProcessor {

    private final Logger logger = LoggerFactory.getLogger(NonConnectClientConnectionProcessor.class);

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private HttpClientBuilderFactory clientBuilderFactory;

    @Override
    public void process(final ClientConnection clientConnection, final ProxyInfo proxyInfo)
            throws IOException {
        logger.debug("Handle non-connect request");
        try (CloseableHttpClient httpClient = clientBuilderFactory.createClientBuilder(proxyInfo).build()) {
            URI uri = clientConnection.getRequestUri();
            HttpHost target = new HttpHost(uri.getHost(),
                    uri.getPort(),
                    uri.getScheme());

            HttpClientContext context = HttpClientContext.create();
            if (proxyInfo.getType().isSocks()) {
                InetSocketAddress proxySocketAddress = new InetSocketAddress(proxyInfo.getProxyHost().getHostName(),
                        proxyInfo.getProxyHost().getPort());
                context.setAttribute(HttpUtils.SOCKS_ADDRESS, proxySocketAddress);
            }

            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(target, clientConnection.getHttpRequest(), context)) {
                try {
                    StatusLine statusLine = response.getStatusLine();
                    logger.debug("Write status line: {}", statusLine);
                    clientConnection.write(statusLine);
                    clientConnection.write(HttpUtils.createViaHeader(
                            clientConnection.getRequestLine().getProtocolVersion(),
                            response.getFirstHeader(HttpHeaders.VIA)));
                    response.removeHeaders(HttpHeaders.VIA);
                    for (Header header : response.getAllHeaders()) {
                        if (HttpHeaders.TRANSFER_ENCODING.equals(header.getName())) {

                            // Strip 'chunked' from Transfer-Encoding header's value
                            // since the response is not chunked
                            String nonChunkedTransferEncoding = HttpUtils.stripChunked(header.getValue());
                            if (StringUtils.isNotEmpty(nonChunkedTransferEncoding)) {
                                clientConnection.write(
                                        HttpUtils.createHttpHeader(HttpHeaders.TRANSFER_ENCODING,
                                                nonChunkedTransferEncoding));
                                logger.debug("Add chunk-striped header response");
                            } else {
                                logger.debug("Remove transfer encoding chunked header response");
                            }
                        } else {
                            logger.debug("Write response header: {}", header);
                            clientConnection.write(header);
                        }
                    }

                    // Empty line marking the end
                    // of header's section
                    clientConnection.writeln();

                    // Now write the request body, if any
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        logger.debug("Start writing entity content");
                        entity.writeTo(clientConnection.getOutputStream());
                        logger.debug("End writing entity content");

                        // Make sure the entity is fully consumed
                        EntityUtils.consume(entity);
                    }

                } catch (Exception e) {
                    logger.debug("Error on handling non CONNECT response", e);
                }
            }
        }
    }

}
