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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.proxy.ClientConnection;
import org.kpax.winfoom.proxy.HttpClientBuilderFactory;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.kpax.winfoom.proxy.RepeatableHttpEntity;
import org.kpax.winfoom.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

/**
 * Process any type of non-CONNECT request for any type of proxy.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 4/13/2020
 */
@ThreadSafe
@Component
class NonConnectClientConnectionProcessor extends ClientConnectionProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private HttpClientBuilderFactory clientBuilderFactory;

    @Override
    void handleRequest(final ClientConnection clientConnection, final ProxyInfo proxyInfo)
            throws IOException {
        final HttpRequest request = clientConnection.getRequest();
        if (clientConnection.isFirstProcessing()) {
            logger.debug("Prepare the clientConnection for request");
            // Prepare the request for execution:
            // remove some headers, fix VIA header and set a proper entity
            if (request instanceof HttpEntityEnclosingRequest) {
                logger.debug("Set enclosing entity");
                RepeatableHttpEntity entity = new RepeatableHttpEntity(request,
                        clientConnection.getSessionInputBuffer(),
                        proxyConfig.getTempDirectory(),
                        systemConfig.getInternalBufferLength());
                clientConnection.registerAutoCloseable(entity);

                Header transferEncoding = request.getFirstHeader(HTTP.TRANSFER_ENCODING);
                if (transferEncoding != null
                        && StringUtils.containsIgnoreCase(transferEncoding.getValue(), HTTP.CHUNK_CODING)) {
                    logger.debug("Mark entity as chunked");
                    entity.setChunked(true);

                    // Apache HttpClient adds a Transfer-Encoding header's chunk directive
                    // so remove or strip the existent one from chunk directive
                    request.removeHeader(transferEncoding);
                    String nonChunkedTransferEncoding = HttpUtils.stripChunked(transferEncoding.getValue());
                    if (StringUtils.isNotEmpty(nonChunkedTransferEncoding)) {
                        request.addHeader(
                                HttpUtils.createHttpHeader(HttpHeaders.TRANSFER_ENCODING,
                                        nonChunkedTransferEncoding));
                        logger.debug("Add chunk-striped request header");
                    } else {
                        logger.debug("Remove transfer encoding chunked request header");
                    }

                }
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            } else {
                logger.debug("No enclosing entity");
            }

            // Remove banned headers
            List<String> bannedHeaders = request instanceof HttpEntityEnclosingRequest ?
                    HttpUtils.ENTITY_BANNED_HEADERS : HttpUtils.DEFAULT_BANNED_HEADERS;
            for (Header header : request.getAllHeaders()) {
                if (bannedHeaders.contains(header.getName())) {
                    request.removeHeader(header);
                    logger.debug("Request header {} removed", header);
                } else {
                    logger.debug("Allow request header {}", header);
                }
            }

            // Add a Via header and remove the existent one(s)
            Header viaHeader = request.getFirstHeader(HttpHeaders.VIA);
            request.removeHeaders(HttpHeaders.VIA);
            request.setHeader(HttpUtils.createViaHeader(clientConnection.getRequestLine().getProtocolVersion(),
                    viaHeader));
        }

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
            try (CloseableHttpResponse response = httpClient.execute(target, request, context)) {
                try {
                    StatusLine statusLine = response.getStatusLine();
                    logger.debug("Write status line: {}", statusLine);
                    clientConnection.write(statusLine);
                    clientConnection.write(HttpUtils.createViaHeader(
                            clientConnection.getRequestLine().getProtocolVersion(),
                            response.getFirstHeader(HttpHeaders.VIA)));

                    response.removeHeaders(HttpHeaders.VIA);
                    response.removeHeaders(HttpHeaders.PROXY_AUTHENTICATE);

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

    @Override
    void handleError(ClientConnection clientConnection, ProxyInfo proxyInfo, Exception e) throws ProxyConnectException {
        if (e instanceof HttpHostConnectException) {
            if (e.getCause() instanceof ConnectException) {
                throw new ProxyConnectException(e.getMessage(), e);
            } else {
                clientConnection.writeErrorResponse(HttpStatus.SC_GATEWAY_TIMEOUT, e.getMessage());
            }
        } else {
            // Generic error
            clientConnection.writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
