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
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.NotThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.InputOutputs;
import org.kpax.winfoom.util.ObjectFormat;
import org.kpax.winfoom.util.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * It encapsulates a client's connection.
 * <p>It provides various methods for exchanging data with the client,
 * also some information about the state of the request's processing.
 * <p><b>Note:</b> This class doesn't have the responsibility to close the underlying socket.
 *
 * @author Eugen Covaci
 */
@NotThreadSafe
final class ClientConnection implements StreamSource, AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(ClientConnection.class);

    /**
     * The list of {@link AutoCloseable}s to be closed when this instance's {@link #close()} method is called.
     */
    private final Set<AutoCloseable> autoCloseables = new HashSet<>();

    /**
     * The underlying socket.
     */
    private final Socket socket;

    private final ProxyConfig proxyConfig;

    private final SystemConfig systemConfig;

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
    private final HttpRequest request;

    /**
     * The request URI extracted from the request line.
     */
    private final URI requestUri;

    private boolean prepareAttempted;

    /**
     * Constructor.<br>
     * Has the responsibility of parsing the request.
     *
     * @param socket       the underlying socket.
     * @param proxyConfig
     * @param systemConfig
     * @throws IOException
     * @throws HttpException
     */
    ClientConnection(final Socket socket,
                     final ProxyConfig proxyConfig,
                     final SystemConfig systemConfig) throws IOException, HttpException {
        this.socket = socket;
        this.proxyConfig = proxyConfig;
        this.systemConfig = systemConfig;

        // Set the streams
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();

        // Parse the request
        try {
            this.sessionInputBuffer = new SessionInputBufferImpl(
                    new HttpTransportMetricsImpl(),
                    InputOutputs.DEFAULT_BUFFER_SIZE,
                    InputOutputs.DEFAULT_BUFFER_SIZE,
                    MessageConstraints.DEFAULT,
                    StandardCharsets.UTF_8.newDecoder());
            this.sessionInputBuffer.bind(this.inputStream);
            this.request = new DefaultHttpRequestParser(this.sessionInputBuffer).parse();
            try {
                this.requestUri = HttpUtils.parseRequestUri(this.request.getRequestLine());
            } catch (URISyntaxException e) {
                throw new HttpException("Invalid request uri", e);
            }
        } catch (Exception e) {
            if (e instanceof HttpException) {
                // Most likely a bad request
                // even though might not always be the case
                writeErrorResponse(HttpStatus.SC_BAD_REQUEST, e.getMessage());
            } else {
                writeErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            throw e;
        }
    }

    /**
     * @return the input stream of the client's socket
     */
    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the output stream of the client's socket
     */
    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @return the session input buffer used to parse the request into a {@link HttpRequest} instance
     */
    SessionInputBufferImpl getSessionInputBuffer() {
        return sessionInputBuffer;
    }

    /**
     * @return the HTTP request.
     */
    HttpRequest getRequest() {
        return request;
    }

    /**
     * Prepare the request for execution, if hasn't been already prepared.
     *
     * @return the HTTP request, prepared for execution.
     */
    HttpRequest getPreparedRequest() throws IOException {
        if (!prepareAttempted) {
            prepareAttempted = true;
            logger.debug("Prepare the clientConnection for request");
            // Prepare the request for execution:
            // remove some headers, fix VIA header and set a proper entity
            if (request instanceof HttpEntityEnclosingRequest) {
                logger.debug("Set enclosing entity");
                RepeatableHttpEntity entity = new RepeatableHttpEntity(request,
                        this.sessionInputBuffer,
                        proxyConfig.getTempDirectory(),
                        systemConfig.getInternalBufferLength());
                registerAutoCloseable(entity);

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
            request.setHeader(HttpUtils.createViaHeader(getRequestLine().getProtocolVersion(),
                    viaHeader));
        }
        return request;
    }

    boolean isPrepareAttempted() {
        return prepareAttempted;
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
     * Write a simple response with only the status line with protocol version 1.1 and date header,
     * followed by an empty line.
     *
     * @param statusCode the status code.
     * @param e          the message of this error becomes the reasonPhrase from the status line.
     */
/*    void writeErrorResponse(int statusCode, Exception e) {
        writeErrorResponse(statusCode, e.getMessage());
    }*/

    /**
     * Write a simple response with only the status line and date header, followed by an empty line.
     *
     * @param statusCode the request's status code.
     */
    void writeErrorResponse(int statusCode) {
        writeErrorResponse(statusCode, null);
    }

    /**
     * Write a simple response with only the status line and date header, followed by an empty line.
     *
     * @param statusCode   the request's status code.
     * @param reasonPhrase the request's reason code
     */
    void writeErrorResponse(int statusCode, String reasonPhrase) {
        try {
            write(HttpUtils.toStatusLine(request != null ? request.getProtocolVersion() : HttpVersion.HTTP_1_1, statusCode, reasonPhrase));
            write(HttpUtils.createHttpHeader(HTTP.DATE_HEADER, HttpUtils.getCurrentDate()));
            writeln();
        } catch (Exception ex) {
            logger.debug("Error on writing error response", ex);
        }
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

    boolean isConnect() {
        return HttpUtils.HTTP_CONNECT.equalsIgnoreCase(request.getRequestLine().getMethod());
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
        return request.getRequestLine();
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
