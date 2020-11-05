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

import org.apache.http.*;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.annotation.NotThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.ProxyConnectException;
import org.kpax.winfoom.pac.PacScriptEvaluator;
import org.kpax.winfoom.proxy.processor.ClientConnectionProcessor;
import org.kpax.winfoom.proxy.processor.ConnectionProcessorSelector;
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
public final class ClientConnection implements StreamSource, AutoCloseable {

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

    private final ProxyBlacklist proxyBlacklist;

    private final ConnectionProcessorSelector connectionProcessorSelector;

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

    private final ListIterator<ProxyInfo> proxyInfoIterator;

    /**
     * Constructor.<br>
     * Has the responsibility of parsing the request and initiate various objects.
     * <p><b>The response should be committed before throwing any exception.</b></p>
     *
     * @param socket
     * @param proxyConfig
     * @param connectionProcessorSelector
     * @param proxyBlacklist
     * @throws IOException
     * @throws HttpException
     */
    private ClientConnection(final Socket socket,
                             final ProxyConfig proxyConfig,
                             final ConnectionProcessorSelector connectionProcessorSelector,
                             final PacScriptEvaluator pacScriptEvaluator,
                             final ProxyBlacklist proxyBlacklist)
            throws Exception {
        this.socket = socket;
        this.proxyConfig = proxyConfig;

        // Set the streams
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.proxyBlacklist = proxyBlacklist;
        this.connectionProcessorSelector = connectionProcessorSelector;

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

        if (proxyConfig.isAutoConfig()) {
            URI requestUri = getRequestUri();
            logger.debug("Extracted URI from request {}", requestUri);
            try {
                List<ProxyInfo> proxies = pacScriptEvaluator.findProxyForURL(requestUri);
                logger.debug("Proxies: {}", proxies);
                List<ProxyInfo> nonBlacklistedProxies = proxyBlacklist.removeBlacklistedProxies(proxies);
                logger.debug("NonBlacklistedProxies: {}", nonBlacklistedProxies);
                this.proxyInfoIterator = nonBlacklistedProxies.listIterator();
            } catch (Exception e) {
                writeErrorResponse(
                        HttpStatus.SC_INTERNAL_SERVER_ERROR,
                        "Proxy Auto Config file error: " + e.getMessage());
                throw e;
            }
            if (!this.proxyInfoIterator.hasNext()) {
                writeErrorResponse(
                        HttpStatus.SC_BAD_GATEWAY,
                        "Proxy Auto Config error: no available proxy server!");
                throw new IllegalStateException("All proxy servers are blacklisted!");
            }
        } else {

            // Manual proxy case
            HttpHost proxyHost = proxyConfig.getProxyType().isDirect() ? null :
                    new HttpHost(proxyConfig.getProxyHost(), proxyConfig.getProxyPort());
            logger.debug("Manual case, proxy host: {}", proxyHost);
            this.proxyInfoIterator = Collections.singletonList(new ProxyInfo(proxyConfig.getProxyType(), proxyHost)).
                    listIterator();
        }
    }

    /**
     * @return the input stream of the client's socket
     */
    @NotNull
    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the output stream of the client's socket
     */
    @NotNull
    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @return the session input buffer used to parse the request into a {@link HttpRequest} instance
     */
    @NotNull
    public SessionInputBufferImpl getSessionInputBuffer() {
        return sessionInputBuffer;
    }

    /**
     * @return the HTTP request.
     */
    @NotNull
    public HttpRequest getRequest() {
        return request;
    }

    /**
     * @return the request URI extracted from the request line.
     */
    @NotNull
    public URI getRequestUri() {
        return requestUri;
    }

    /**
     * Write an object to the output stream using CRLF format.
     *
     * @param obj the object
     * @throws IOException
     */
    public void write(@NotNull Object obj) throws IOException {
        outputStream.write(ObjectFormat.toCrlf(obj));
    }

    /**
     * Write an empty line to the output stream using CRLF format.
     *
     * @throws IOException
     */
    public void writeln() throws IOException {
        outputStream.write(ObjectFormat.CRLF.getBytes());
    }

    /**
     * Write a simple response with only the status line and date header, followed by an empty line.
     * <p><b>This method commits the response.</b></p>
     *
     * @param statusCode the request's status code.
     */
    public void writeErrorResponse(int statusCode) {
        writeErrorResponse(statusCode, null);
    }

    /**
     * Write a simple response with only the status line and date header, followed by an empty line.
     * <p><b>This method commits the response.</b></p>
     *
     * @param statusCode   the request's status code.
     * @param reasonPhrase the request's reason code
     */
    public void writeErrorResponse(int statusCode, String reasonPhrase) {
        try {
            write(HttpUtils.toStatusLine(request != null ? request.getProtocolVersion() : HttpVersion.HTTP_1_1,
                    statusCode, reasonPhrase));
            write(HttpUtils.createHttpHeader(HTTP.DATE_HEADER, HttpUtils.getCurrentDate()));
            writeln();
        } catch (Exception ex) {
            logger.debug("Error on writing error response", ex);
        }
    }

    /**
     * Write the response to the output stream as it is.
     * <p><b>This method commits the response.</b></p>
     *
     * @param httpResponse the HTTP response
     * @throws Exception
     */
    public void writeHttpResponse(@NotNull final HttpResponse httpResponse) throws Exception {
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

    public boolean isConnect() {
        return HttpUtils.HTTP_CONNECT.equalsIgnoreCase(request.getRequestLine().getMethod());
    }

    /**
     * @return {@code true} iff the underlying socket is closed.
     */
    public boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * @return the request's line
     */
    public RequestLine getRequestLine() {
        return request.getRequestLine();
    }

    /**
     * Register an {@link AutoCloseable} for later closing.
     *
     * @param autoCloseable the {@link AutoCloseable} to be closed.
     * @return {@code true} if the specified element isn't already registered
     */
    public boolean registerAutoCloseable(final AutoCloseable autoCloseable) {
        return autoCloseables.add(autoCloseable);
    }

    public boolean isFirstProcessing() {
        return proxyInfoIterator.previousIndex() < 1;
    }

    /**
     * Delegate the request processing to an appropriate {@link ClientConnectionProcessor}
     * and process the client connection with each available proxy.<br>
     * The remote proxy is blacklisted if it is not available.<br>
     * <p><b>This method must always commit the response.</b></p>
     */
    void process() {
        while (proxyInfoIterator.hasNext()) {
            ProxyInfo proxyInfo = proxyInfoIterator.next();
            ClientConnectionProcessor connectionProcessor = connectionProcessorSelector.selectConnectionProcessor(
                    isConnect(), proxyInfo);
            logger.debug("Using connectionProcessor: {}", connectionProcessor);
            try {
                connectionProcessor.process(this, proxyInfo);
                break;
            } catch (ProxyConnectException e) {
                logger.debug("Proxy connect error", e);
                if (proxyConfig.isAutoConfig()) {
                    proxyBlacklist.blacklist(proxyInfo);
                }
                if (proxyInfoIterator.hasNext()) {
                    logger.debug("Failed to connect to proxy: {}, retry with the next one", proxyInfo);
                } else {
                    logger.debug("Failed to connect to proxy: {}, send the error response", proxyInfo);

                    // Cannot connect to the remote proxy,
                    // commit a response with 502 error code
                    writeErrorResponse(HttpStatus.SC_BAD_GATEWAY, e.getMessage());
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        autoCloseables.forEach(InputOutputs::close);
    }

    @Override
    public String toString() {
        return "ClientConnection{" +
                "requestUri=" + requestUri +
                '}';
    }

    static class ClientConnectionBuilder {

        private Socket socket;
        private ProxyConfig proxyConfig;
        private PacScriptEvaluator pacScriptEvaluator;
        private ProxyBlacklist proxyBlacklist;
        private ConnectionProcessorSelector connectionProcessorSelector;

        ClientConnectionBuilder withSocket(Socket socket) {
            this.socket = socket;
            return this;
        }

        ClientConnectionBuilder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        ClientConnectionBuilder withPacScriptEvaluator(PacScriptEvaluator pacScriptEvaluator) {
            this.pacScriptEvaluator = pacScriptEvaluator;
            return this;
        }

        ClientConnectionBuilder withProxyBlacklist(ProxyBlacklist proxyBlacklist) {
            this.proxyBlacklist = proxyBlacklist;
            return this;
        }

        ClientConnectionBuilder withConnectionProcessorSelector(ConnectionProcessorSelector connectionProcessorSelector) {
            this.connectionProcessorSelector = connectionProcessorSelector;
            return this;
        }

        ClientConnection build()
                throws Exception {
            return new ClientConnection(socket, proxyConfig, connectionProcessorSelector, pacScriptEvaluator, proxyBlacklist);
        }
    }
}
