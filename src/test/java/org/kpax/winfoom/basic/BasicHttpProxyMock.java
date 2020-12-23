/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.basic;


import io.netty.buffer.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.kerby.kerberos.kerb.*;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.*;
import org.pac4j.core.credentials.*;
import org.springframework.util.*;

import java.net.*;
import java.nio.charset.*;
import java.util.*;

public class BasicHttpProxyMock implements AutoCloseable {

    private final int proxyPort;

    private final List<UsernamePasswordCredentials> credentials;

    private HttpProxyServer proxyServer;

    private HttpProxyServerBootstrap proxyServerBootstrap;

    private BasicHttpProxyMock(int proxyPort,
                               List<UsernamePasswordCredentials> credentials)
            throws KrbException {
        this.proxyPort = proxyPort;
        this.credentials = credentials;
        this.proxyServerBootstrap = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withName("BasicHttpProxy")
                .withFiltersSource(getFiltersSource())
                .withTransparent(true)
                .withAllowLocalOnly(false);

    }

    public int getProxyPort() {
        return proxyPort;
    }

    public List<UsernamePasswordCredentials> getCredentials() {
        return credentials;
    }

    public void start() throws KrbException {
        System.out.println("Start the Web server");
        proxyServer = proxyServerBootstrap.start();
        System.out.println("Web server started and listening on port: " + proxyServer.getListenAddress().getPort());
    }

    private HttpFiltersSource getFiltersSource() {
        return new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {
                return new HttpFiltersAdapter(originalRequest) {
                    @Override
                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                        if (httpObject instanceof HttpRequest) {
                            HttpRequest request = (HttpRequest) httpObject;
                            String authentication = request.headers().get(HttpHeaders.PROXY_AUTHORIZATION);
                            Optional<UsernamePasswordCredentials> extract = extractBasicCredentials(authentication);
                            if (extract.isPresent() && credentials.contains(extract.get())) {
                                System.out.println("Proxy-Authorization header is present and valid");
                            } else {
                                System.out.println("Proxy-Authorization header is not present or invalid");
                                return generateProxyAuthenticationRequiredResponse();
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }

    private HttpResponse generateProxyAuthenticationRequiredResponse() {
        String body = "<!DOCTYPE HTML \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>" + "Proxy authentication required" + "</title>\n"
                + "</head><body>\n"
                + "This proxy requires authentication"
                + "</body></html>\n";
        byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED, content);
        response.headers().set(HttpHeaders.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaders.PROXY_AUTHENTICATE, Arrays.asList("Basic"));
        response.headers().set(HttpHeaders.DATE, ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaders.CONNECTION, "Keep-Alive");
        return response;
    }

    private Optional<UsernamePasswordCredentials> extractBasicCredentials(String authHeader) {
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Basic ")) {
            // "Authorization" header do not indicate Kerberos mechanism yet,
            // so the extractor shouldn't throw an exception
            return Optional.empty();
        }
        byte[] base64Token = authHeader.substring(authHeader.indexOf(" ") + 1).getBytes(StandardCharsets.UTF_8);
        byte[] basicTicket = Base64.getDecoder().decode(base64Token);
        String[] userPassword = new String(basicTicket).split(":");

        if (userPassword.length == 2) {
            return Optional.of(new UsernamePasswordCredentials(userPassword[0], userPassword[1]));
        }
        return Optional.empty();
    }

    public void stop() throws KrbException {
        if (proxyServer != null) {
            proxyServer.abort();
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    public static void main(String[] args) {
        try {
            BasicHttpProxyMock httpProxyMock = new BasicHttpProxyMockBuilder().build();
            httpProxyMock.start();
            synchronized (httpProxyMock) {
                httpProxyMock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class BasicHttpProxyMockBuilder {

        public static final int DEFAULT_PROXY_PORT = 3128;
        public static final String DEFAULT_USERNAME = "winfoom";
        public static final String DEFAULT_PASSWORD = "1234";

        private int proxyPort = DEFAULT_PROXY_PORT;

        private List<UsernamePasswordCredentials> credentials =
                Collections.singletonList(new UsernamePasswordCredentials(DEFAULT_USERNAME, DEFAULT_PASSWORD));

        public BasicHttpProxyMockBuilder() throws KrbException, UnknownHostException {
        }

        public BasicHttpProxyMockBuilder withProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public BasicHttpProxyMockBuilder withCredentials(List<UsernamePasswordCredentials> credentials) {
            this.credentials = credentials;
            return this;
        }

        public BasicHttpProxyMock build() throws KrbException {
            Assert.notNull(credentials, "credentials cannot be null");
            return new BasicHttpProxyMock(proxyPort, credentials);
        }
    }
}
