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

package org.kpax.winfoom.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HTTP;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.springframework.util.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Various utility methods, HTTP related.
 *
 * @author Eugen Covaci
 */
public final class HttpUtils {

    /**
     * The CONNECT HTTP method.
     */
    public static final String HTTP_CONNECT = "CONNECT";

    /**
     * The context's key for the SOCKS proxy.
     */
    public static final String SOCKS_ADDRESS = "socks.address";

    /**
     * The maximum value of a success HTTP code.
     */
    public static final int MAX_HTTP_SUCCESS_CODE = 299;

    private HttpUtils() {
    }

    /**
     * Parse a {@link String} value into an {@link URI} instance.
     * <p>Some URI may contain non-standard encoded Unicode characters (%uxxxx format).
     * We replace them with the standard encoded equivalent.
     *
     * @param uri the input value.
     * @return the {@link URI} instance.
     * @throws URISyntaxException
     */
    public static URI toUri(String uri) throws URISyntaxException {
        Assert.notNull(uri, "uri cannot be null");
        StringBuilder stringBuilder = new StringBuilder(uri);

        // Replace the dangling ? character
        if (stringBuilder.charAt(uri.length() - 1) == '?') {
            stringBuilder.deleteCharAt(uri.length() - 1);
        }

        // Search for %uxxxx groups and replace
        // them with the standard encoded equivalent
        int index = stringBuilder.indexOf("%u");
        while (index > -1) {
            try {
                String substring = stringBuilder.substring(index, index + 6).replaceAll("%u", "\\\\u");
                String encoded = URLEncoder.encode(StringEscapeUtils.unescapeJava(substring), StandardCharsets.UTF_8);
                stringBuilder.replace(index, index + 6, encoded);
                index = stringBuilder.indexOf("%u", index + 6);
            } catch (Exception e) {// Possible IndexOutOfBoundExceptions
                throw new URISyntaxException(uri, e.getMessage());
            }
        }
        return new URI(stringBuilder.toString());
    }

    /**
     * Extract an {@link URI} instance from a {@link RequestLine}.<br>
     * For CONNECT request, the request's URI looks like: <i>host:port</i>
     * while for other requests it looks like: <i>http://host:port/path?params</i>
     * hence we parse it differently.
     *
     * @param requestLine the request line.
     * @return the extracted {@link URI} instance.
     * @throws URISyntaxException
     */
    public static URI parseRequestUri(RequestLine requestLine) throws URISyntaxException {
        try {
            if (HTTP_CONNECT.equalsIgnoreCase(requestLine.getMethod())) {
                return new URI(HttpHost.create(requestLine.getUri()).toURI());
            } else {
                return toUri(requestLine.getUri());
            }
        } catch (Exception e) {
            if (e instanceof URISyntaxException) {
                throw e;
            }
            throw new URISyntaxException(requestLine.getUri(), e.getMessage());
        }
    }

    /**
     * Remove the {@code chunked} word from a comma separated sequence of words.
     *
     * @param value a comma separated sequence of words.
     * @return a comma separated sequence of words with the {@code chunked} word removed.
     */
    public static String stripChunked(String value) {
        return Arrays.stream(value.split(",")).map(String::trim)
                .filter((item) -> !HTTP.CHUNK_CODING.equalsIgnoreCase(item))
                .collect(Collectors.joining(","));
    }

    /**
     * Wrap the first header into an {@link Optional}.
     *
     * @param request the request
     * @param name    the header's name
     * @return an {@link Optional} containing the header.
     */
    public static Optional<Header> getFirstHeader(HttpRequest request, String name) {
        return Optional.ofNullable(request.getFirstHeader(name));
    }

    /**
     * Get the first request header for a certain header name.
     * @param request the {@link HttpRequest} instance
     * @param name the header's name
     * @return an {@link Optional} containing the header
     */
    public static Optional<String> getFirstHeaderValue(HttpRequest request, String name) {
        return getFirstHeader(request, name).map(NameValuePair::getValue);
    }

    /**
     * Get the request Content-length header's value.
     *
     * @param request the HTTP request.
     * @return the request's Content-length header's value or {@code -1} when missing.
     */
    public static long getContentLength(HttpRequest request) {
        return getFirstHeaderValue(request, HttpHeaders.CONTENT_LENGTH).map(Long::parseLong).orElse(-1L);
    }

    /**
     * Create a {@link BasicHeader} instance.
     *
     * @param name  the header's name.
     * @param value the header's value.
     * @return a new {@link BasicHeader} instance.
     */
    public static Header createHttpHeader(String name, String value) {
        return new BasicHeader(name, value);
    }

    public static void tuneSocket(final Socket socket, int bufferSize) throws SocketException {
        socket.setTcpNoDelay(true);
        socket.setReceiveBufferSize(bufferSize);
        socket.setSendBufferSize(bufferSize);
    }

    /**
     * Create a {@link BasicStatusLine} instance with HTTP/1.1 version and no reason code.
     *
     * @see #toStatusLine(ProtocolVersion, int, String)
     */
    public static BasicStatusLine toStatusLine(int httpCode) {
        return toStatusLine(HttpVersion.HTTP_1_1, httpCode, null);
    }

    /**
     * Create a {@link BasicStatusLine} instance with no reason code
     *
     * @see #toStatusLine(ProtocolVersion, int, String)
     */
    public static BasicStatusLine toStatusLine(ProtocolVersion protocolVersion, int httpCode) {
        return toStatusLine(protocolVersion, httpCode, null);
    }

    /**
     * Create a {@link BasicStatusLine} instance with HTTP/1.1 version.
     *
     * @see #toStatusLine(ProtocolVersion, int, String)
     */
    public static BasicStatusLine toStatusLine(int httpCode, String reasonPhrase) {
        return toStatusLine(HttpVersion.HTTP_1_1, httpCode, reasonPhrase);
    }

    /**
     * Create a {@link BasicStatusLine} instance.
     *
     * @param protocolVersion the HTTP version
     * @param httpCode        the HTTP code
     * @param reasonPhrase    the HTTP reason phrase
     * @return a new {@link BasicStatusLine} instance.
     */
    public static BasicStatusLine toStatusLine(ProtocolVersion protocolVersion, int httpCode, String reasonPhrase) {
        Assert.notNull(protocolVersion, "protocolVersion cannot be null");
        return new BasicStatusLine(protocolVersion, httpCode,
                StringUtils.isEmpty(reasonPhrase) ?
                        EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH) : reasonPhrase);
    }

    /**
     * Validate a port value.
     *
     * @param port the port value.
     * @return {@code true} iff the port value is between 1-65535.
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port < 65536;
    }

    /**
     * Parse the HTTP request's  to extract a {@link ContentType} instance from Content-Type header.
     *
     * @param request the HTTP request.
     * @return the {@link ContentType} instance.
     */
    public static ContentType getContentType(HttpRequest request) {
        Header contentTypeHeader = request.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        Assert.isTrue(contentTypeHeader != null, "No Content-Type header found");
        String[] tokens = contentTypeHeader.getValue().split(";");
        Assert.isTrue(tokens.length > 0, "Wrong content-type format");
        String contentType = tokens[0].trim();
        String charset = null;
        if (tokens.length > 1) {
            for (int i = 1; i < tokens.length; i++) {
                tokens[i] = tokens[i].trim();
                if (tokens[i].startsWith("charset=")) {
                    charset = tokens[i].substring("charset=".length());
                }
            }
        }
        return ContentType.create(contentType, charset);
    }

    /**
     * Write all the entity content into a dummy output stream.
     *
     * @param httpEntity the entity to be consumed.
     * @throws IOException
     */
    public static void consumeEntity(HttpEntity httpEntity) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        httpEntity.writeTo(outStream);
        outStream.flush();
    }


    /**
     * Wrap the text inside {@code <html></html>} tag.
     *
     * @param text the text to wrap.
     * @return the wrapped text.
     */
    public static String toHtml(String text) {
        return new StringBuilder("<html>").append(text).append("</html>").toString();
    }

    /**
     * Quite a dirty hack.<br>
     * Using Java Reflection, call the {@code java.net.SocksSocketImpl#setV4()} method.
     *
     * @param socket the SOCKS socket to be marked as version 4.
     * @throws UnsupportedOperationException
     */
    public static void setSocks4(final Socket socket) throws UnsupportedOperationException {
        try {
            Field implField = Socket.class.getDeclaredField("impl");
            implField.setAccessible(true);
            Object impl = implField.get(socket);
            Class<?> implType = impl.getClass();
            Method setV4 = implType.getDeclaredMethod("setV4");
            setV4.setAccessible(true);
            setV4.invoke(impl);
        } catch (Exception e) {
            throw new UnsupportedOperationException("SOCKS 4 not supported by the JVM", e);
        }
    }

    /**
     * Parse the proxy line returned by PAC proxy script.
     *
     * @param proxyLine the proxy line.
     * @return the list of {@link ProxyInfo}s.
     */
    public static List<ProxyInfo> parsePacProxyLine(String proxyLine) {
        if (StringUtils.isBlank(proxyLine)) {
            return Collections.singletonList(new ProxyInfo(ProxyInfo.PacType.DIRECT));
        }
        List<ProxyInfo> proxyInfos = new ArrayList<>();
        for (String s : proxyLine.split(";")) {
            String[] split = s.trim().split("\\s+");
            Assert.isTrue(split.length > 0, String.format("Invalid proxy line [%s]: empty", proxyLine));
            ProxyInfo.PacType type = ProxyInfo.PacType.valueOf(split[0].trim());
            if (type == ProxyInfo.PacType.DIRECT) {
                proxyInfos.add(new ProxyInfo(ProxyInfo.PacType.DIRECT));
            } else {
                Assert.isTrue(split.length > 1, String.format("Invalid proxy line [%s]: proxy host:port required", proxyLine));
                proxyInfos.add(new ProxyInfo(type, HttpHost.create(split[1].trim())));
            }
        }
        return Collections.unmodifiableList(proxyInfos);
    }

    public static boolean isConnectionRefused(Exception e) {
        return e instanceof SocketException && e.getMessage().startsWith("Connection refused");
    }

    /**
     * Check whether this exception signals an aborted client's connection.
     *
     * @param e the error to check on
     * @return {@code true} iff the error signals an aborted client's connection.
     */
    public static boolean isConnectionAborted(Exception e) {
        return e instanceof SocketException
                && StringUtils.startsWithIgnoreCase(e.getMessage(), "Software caused connection abort");
    }

    /**
     * Create a new Via {@link Header} by inserting the winfoom info.
     *
     * @param version   the HTTP version
     * @param viaHeader the existent Via {@link Header} (nullable)
     * @return a new Via {@link Header}
     */
    public static Header createViaHeader(final ProtocolVersion version, final Header viaHeader) {
        Assert.notNull(version, "version cannot be null");
        String value = String.format("%s.%s winfoom", version.getMajor(), version.getMinor())
                + (viaHeader != null ? ", " + viaHeader.getValue() : "");
        return new BasicHeader(HttpHeaders.VIA, value);
    }

}
