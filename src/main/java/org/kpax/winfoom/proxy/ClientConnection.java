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
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.NotThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.HeaderDateGenerator;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;
import org.kpax.winfoom.util.ObjectFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * It encapsulates a client's connection.
 * <p>It provides various methods for exchanging data with the client,
 * also some information about the state of the request's processing.
 * <p><b>Note:</b> This class doesn't have the responsibility to close the underlying socket.
 *
 * @author Eugen Covaci
 */
@NotThreadSafe
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
final class ClientConnection implements AutoCloseable {

    /**
     * These headers will be removed from client's response if there is an enclosing
     * entity.
     */
    private static final List<String> ENTITY_BANNED_HEADERS = Arrays.asList(
            HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CONTENT_ENCODING,
            HttpHeaders.PROXY_AUTHORIZATION);

    /**
     * These headers will be removed from client's response if there is no enclosing
     * entity (it means the request has no body).
     */
    private static final List<String> DEFAULT_BANNED_HEADERS = Collections.singletonList(
            HttpHeaders.PROXY_AUTHORIZATION);


    private final Logger logger = LoggerFactory.getLogger(ClientConnection.class);

    /**
     * The list of {@link AutoCloseable}s to be closed when this instance's {@link #close()} method is called.
     */
    private final Set<AutoCloseable> autoCloseables = new HashSet<>();

    /**
     * The underlying socket.
     */
    private final Socket socket;

    /**
     * The socket's input stream.
     */
    private final InputStream inputStream;

    /**
     * The socket's output stream.
     */
    private final OutputStream outputStream;

    /**
     * Used for request parsing also by the {@link org.kpax.winfoom.proxy.RepeatableHttpEntity} class.
     */
    private final SessionInputBufferImpl sessionInputBuffer;

    /**
     * The parsed {@link HttpRequest}.
     */
    private final HttpRequest httpRequest;

    /**
     * The request line. We keep a reference here to avoid multiple calling the {@link HttpRequest#getRequestLine()}
     */
    private final RequestLine requestLine;

    /**
     * The request URI extracted from the request line.
     */
    private final URI requestUri;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private HttpClientBuilderFactory clientBuilderFactory;

    /**
     * Whether the request is prepared (it means the request headers are set, also the request entity - if any)<br>
     * Only makes sense for non-CONNECT HTTP requests.
     */
    private boolean requestPrepared;

    /**
     * Constructor.<br>
     * Has the responsibility of parsing the request.
     *
     * @param socket the underlying socket.
     * @throws IOException
     * @throws HttpException
     */
    ClientConnection(Socket socket) throws IOException, HttpException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.sessionInputBuffer = new SessionInputBufferImpl(
                new HttpTransportMetricsImpl(),
                InputOutputs.DEFAULT_BUFFER_SIZE,
                InputOutputs.DEFAULT_BUFFER_SIZE,
                MessageConstraints.DEFAULT,
                StandardCharsets.UTF_8.newDecoder());
        this.sessionInputBuffer.bind(this.inputStream);
        this.httpRequest = new DefaultHttpRequestParser(this.sessionInputBuffer).parse();
        this.requestLine = httpRequest.getRequestLine();
        try {
            this.requestUri = HttpUtils.parseRequestUri(this.requestLine);
        } catch (URISyntaxException e) {
            throw new HttpException("Invalid request uri", e);
        }
    }

    /**
     * @return the input stream of the client's socket
     */
    InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the output stream of the client's socket
     */
    OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @return the session input buffer used to parse the request into a {@link HttpRequest} instance
     */
    SessionInputBufferImpl getSessionInputBuffer() {
        return sessionInputBuffer;
    }

    /**
     * @return the request URI extracted from the request line.
     */
    URI getRequestUri() {
        return requestUri;
    }

    /**
     * Write an object to the output stream using CRLF format.
     *
     * @param obj the object
     * @throws IOException
     */
    void write(Object obj) throws IOException {
        outputStream.write(ObjectFormat.toCrlf(obj));
    }

    /**
     * Write an empty line to the output stream using CRLF format.
     *
     * @throws IOException
     */
    void writeln() throws IOException {
        outputStream.write(ObjectFormat.CRLF.getBytes());
    }

    /**
     * Write a simple response with only the status line with protocol version 1.1, followed by an empty line.
     *
     * @param statusCode the status code.
     * @param e          the message of this error becomes the reasonPhrase from the status line.
     */
    void writeErrorResponse(int statusCode, Exception e) {
        writeErrorResponse(HttpVersion.HTTP_1_1, statusCode, e);
    }

    /**
     * Write a simple response with only the status line followed by an empty line.
     *
     * @param protocolVersion the request's HTTP version.
     * @param statusCode      the request's status code.
     * @param reasonPhrase    the request's reason code
     */
    void writeErrorResponse(ProtocolVersion protocolVersion, int statusCode, String reasonPhrase) {
        try {
            write(HttpUtils.toStatusLine(protocolVersion, statusCode, reasonPhrase));
            write(HttpUtils.createHttpHeader(HTTP.DATE_HEADER, new HeaderDateGenerator().getCurrentDate()));
            writeln();
        } catch (Exception ex) {
            logger.debug("Error on writing error response", ex);
        }
    }

    /**
     * Write a simple response with only the status line, followed by an empty line.
     *
     * @param protocolVersion the request's HTTP version.
     * @param statusCode      the request's status code.
     * @param e               the message of this error becomes the reasonPhrase from the status line.
     */
    void writeErrorResponse(ProtocolVersion protocolVersion, int statusCode, Exception e) {
        Assert.notNull(e, "Exception cannot be null");
        writeErrorResponse(protocolVersion, statusCode, e.getMessage());
    }

    /**
     * Write the response to the output stream as it is.
     *
     * @param httpResponse the HTTP response
     * @throws Exception
     */
    void writeHttpResponse(final HttpResponse httpResponse) throws Exception {
        StatusLine statusLine = httpResponse.getStatusLine();
        logger.debug("Write statusLine {}", statusLine);
        write(statusLine);

        logger.debug("Write headers");
        for (Header header : httpResponse.getAllHeaders()) {
            write(header);
        }

        // Empty line between headers and the body
        writeln();

        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            logger.debug("Write entity content");
            entity.writeTo(outputStream);
        }
        EntityUtils.consume(entity);
    }

    /**
     * Whether the request has been marked as prepared for execution.
     *
     * @return <code>true</code> iff the request has been marked as prepared.
     */
    boolean isRequestPrepared() {
        return requestPrepared;
    }

    /**
     * Prepare for remote request execution.
     * <p>Preparation can only occur once, this method does nothing if the request is already prepared.</p>
     *
     * @throws IOException
     */
    private void prepareRequest(ProxyInfo proxyInfo) throws IOException {
        // Prepare the request for execution
        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            AbstractHttpEntity entity;
            logger.debug("Set enclosing entity");
            if (proxyInfo.getType().isSocks()) {

                // There is no need for caching since
                // SOCKS communication is one step only
                entity = new InputStreamEntity(inputStream,
                        HttpUtils.getContentLength(httpRequest),
                        HttpUtils.getContentType(httpRequest));
            } else {
                entity = new RepeatableHttpEntity(httpRequest, sessionInputBuffer,
                        proxyConfig.getTempDirectory(),
                        systemConfig.getInternalBufferLength());
                registerAutoCloseable((RepeatableHttpEntity) entity);
            }

            Header transferEncoding = httpRequest.getFirstHeader(HTTP.TRANSFER_ENCODING);
            if (transferEncoding != null
                    && StringUtils.containsIgnoreCase(transferEncoding.getValue(), HTTP.CHUNK_CODING)) {
                logger.debug("Mark entity as chunked");
                entity.setChunked(true);

                // Apache HttpClient adds a Transfer-Encoding header's chunk directive
                // so remove or strip the existent one from chunk directive
                httpRequest.removeHeader(transferEncoding);
                String nonChunkedTransferEncoding = HttpUtils.stripChunked(transferEncoding.getValue());
                if (StringUtils.isNotEmpty(nonChunkedTransferEncoding)) {
                    httpRequest.addHeader(
                            HttpUtils.createHttpHeader(HttpHeaders.TRANSFER_ENCODING,
                                    nonChunkedTransferEncoding));
                    logger.debug("Add chunk-striped request header");
                } else {
                    logger.debug("Remove transfer encoding chunked request header");
                }

            }
            ((HttpEntityEnclosingRequest) httpRequest).setEntity(entity);
        } else {
            logger.debug("No enclosing entity");
        }

        // Remove banned headers
        List<String> bannedHeaders = httpRequest instanceof HttpEntityEnclosingRequest ?
                ENTITY_BANNED_HEADERS : DEFAULT_BANNED_HEADERS;
        for (Header header : httpRequest.getAllHeaders()) {
            if (bannedHeaders.contains(header.getName())) {
                httpRequest.removeHeader(header);
                logger.debug("Request header {} removed", header);
            } else {
                logger.debug("Allow request header {}", header);
            }
        }

        // Add a Via header and remove the existent one(s)
        Header viaHeader = httpRequest.getFirstHeader(HttpHeaders.VIA);
        httpRequest.removeHeaders(HttpHeaders.VIA);
        httpRequest.setHeader(HttpUtils.createViaHeader(requestLine.getProtocolVersion(),
                viaHeader));
    }

    /**
     * Execute the {@link #httpRequest}.
     * <p><b>Only makes sense for non-CONNECT requests.</b>
     *
     * @param proxyInfo            the proxy information
     * @param httpResponseCallback called with {@link CloseableHttpResponse} response
     * @throws IOException
     */
    void executeNonConnectRequest(final ProxyInfo proxyInfo, HttpResponseCallback httpResponseCallback) throws IOException {
        HttpHost target = new HttpHost(requestUri.getHost(),
                requestUri.getPort(),
                requestUri.getScheme());

        HttpClientContext context = HttpClientContext.create();
        if (proxyInfo.getType().isSocks()) {
            InetSocketAddress proxySocketAddress = new InetSocketAddress(proxyInfo.getProxyHost().getHostName(),
                    proxyInfo.getProxyHost().getPort());
            context.setAttribute(HttpUtils.SOCKS_ADDRESS, proxySocketAddress);
        }

        if (!requestPrepared) {
            prepareRequest(proxyInfo);
            this.requestPrepared = true;
        }

        try (CloseableHttpClient httpClient = clientBuilderFactory.createClientBuilder(proxyInfo).build()) {
            CloseableHttpResponse response = httpClient.execute(target, httpRequest, context);
            httpResponseCallback.processResponse(response);
        }
    }

    /**
     * @return {@code true} iff the underlying socket is closed.
     */
    boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * @return the request's line
     */
    RequestLine getRequestLine() {
        return requestLine;
    }

    /**
     * Register an {@link AutoCloseable} for later closing.
     *
     * @param autoCloseable the {@link AutoCloseable} to be closed.
     * @return {@code true} if the specified element isn't already registered
     */
    boolean registerAutoCloseable(AutoCloseable autoCloseable) {
        return autoCloseables.add(autoCloseable);
    }


    @Override
    public void close() {
        autoCloseables.forEach(InputOutputs::close);
    }
}
