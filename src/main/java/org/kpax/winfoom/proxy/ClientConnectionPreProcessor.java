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

package org.kpax.winfoom.proxy;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.protocol.HTTP;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ClientConnectionPreProcessor {

    private final Logger logger = LoggerFactory.getLogger(ClientConnectionPreProcessor.class);

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private SystemConfig systemConfig;

    /**
     * Prepare a {@link ClientConnection} for execution.
     *
     * @param clientConnection the client's connection
     * @param proxyType        the proxy type
     * @throws IOException
     */
    public void prepare(final ClientConnection clientConnection, final ProxyType proxyType) throws IOException {
        logger.debug("Prepare the clientConnection for request");
        if (!clientConnection.isConnect()) {
            // Prepare the request for execution:
            // remove some headers, fix VIA header and set a proper entity
            HttpRequest request = clientConnection.getHttpRequest();
            if (request instanceof HttpEntityEnclosingRequest) {
                logger.debug("Set enclosing entity");
                AbstractHttpEntity entity = new RepeatableHttpEntity(request,
                        clientConnection.getSessionInputBuffer(),
                        proxyConfig.getTempDirectory(),
                        systemConfig.getInternalBufferLength());
                clientConnection.registerAutoCloseable((RepeatableHttpEntity) entity);

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
    }

}
