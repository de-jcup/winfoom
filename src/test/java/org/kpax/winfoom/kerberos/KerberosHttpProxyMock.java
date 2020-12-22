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

package org.kpax.winfoom.kerberos;


import io.netty.buffer.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.kerby.kerberos.kerb.*;
import org.apache.kerby.kerberos.kerb.identity.backend.*;
import org.apache.kerby.kerberos.kerb.server.*;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.*;
import org.pac4j.core.credentials.*;
import org.pac4j.kerberos.credentials.*;
import org.pac4j.kerberos.credentials.authenticator.*;
import org.springframework.core.io.*;
import org.springframework.util.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

public class KerberosHttpProxyMock implements AutoCloseable {

    private final int proxyPort;

    private final String domain;

    private final List<UsernamePasswordCredentials> credentials;

    private HttpProxyServer proxyServer;

    private HttpProxyServerBootstrap proxyServerBootstrap;

    private SimpleKdcServer simpleKdcServer;

    private SunJaasKerberosTicketValidator ticketValidator;

    private KerberosHttpProxyMock(int proxyPort,
                                  String domain,
                                  List<UsernamePasswordCredentials> credentials,
                                  SimpleKdcServer simpleKdcServer)
            throws KrbException {
        this.proxyPort = proxyPort;
        this.domain = domain;
        this.credentials = credentials;
        this.proxyServerBootstrap = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withName("KerberosHttpProxy")
                .withFiltersSource(getFiltersSource())
                .withTransparent(true)
                .withAllowLocalOnly(false);

        this.simpleKdcServer = simpleKdcServer;
        System.out.println(" *** isRenewableAllowed: " + simpleKdcServer.getKdcConfig().isRenewableAllowed());
        this.simpleKdcServer.init();

        this.ticketValidator = new SunJaasKerberosTicketValidator();
        this.ticketValidator.setServicePrincipal("HTTP/" + domain + "@EXAMPLE.COM");
        this.ticketValidator.setKeyTabLocation(new FileSystemResource(new File("src/test/resources/test.keytab")));
        this.ticketValidator.setDebug(true);
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

        System.out.println("Start KDC server");
        simpleKdcServer.start();

        System.out.println("Add the principals");
        simpleKdcServer.createPrincipal("HTTP/" + domain + "@EXAMPLE.COM");
        for (UsernamePasswordCredentials credential : credentials) {
            simpleKdcServer.createPrincipal(credential.getUsername(), credential.getPassword());
        }
        System.out.println("KDC server started and listening on port: " + simpleKdcServer.getKdcPort());
        System.out.println("Export credentials");
        simpleKdcServer.exportPrincipals(new File("src/test/resources/test.keytab"));

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
                            Optional<KerberosCredentials> extract = extractKrbCredentials(authentication);

                            if (extract.isPresent()) {
                                System.out.println("Proxy-Authorization header is present, now validate");
                                try {
                                    validate(extract.get());
                                    System.out.println("The credentials are valid");
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                    return generateProxyAuthenticationRequiredResponse();
                                }
                            } else {
                                System.out.println("Proxy-Authorization header is not present");
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
        response.headers().set(HttpHeaders.PROXY_AUTHENTICATE, Arrays.asList("Kerberos"));
        response.headers().set(HttpHeaders.DATE, ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaders.CONNECTION, "Keep-Alive");
        return response;
    }

    private Optional<KerberosCredentials> extractKrbCredentials(String authHeader) {
        if (StringUtils.isEmpty(authHeader) || !(authHeader.startsWith("Negotiate ") ||
                authHeader.startsWith("Kerberos "))) {
            // "Authorization" header do not indicate Kerberos mechanism yet,
            // so the extractor shouldn't throw an exception
            return Optional.empty();
        }
        byte[] base64Token = authHeader.substring(authHeader.indexOf(" ") + 1).getBytes(StandardCharsets.UTF_8);
        byte[] kerberosTicket = Base64.getDecoder().decode(base64Token);
        return Optional.of(new KerberosCredentials(kerberosTicket));
    }

    private void validate(KerberosCredentials credentials) {
        KerberosTicketValidation ticketValidation = this.ticketValidator.validateTicket(credentials.getKerberosTicket());
        String subject = ticketValidation.username();
        System.out.println("Successfully validated " + subject);
    }

    public void stop() throws KrbException {
        if (proxyServer != null) {
            proxyServer.abort();
        }
        simpleKdcServer.stop();
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    public static void main(String[] args) {
        try {
            KerberosHttpProxyMock httpProxyMock = new KerberosHttpProxyMockBuilder().
                    withDomain("auth.example.com").build();
            httpProxyMock.start();
            synchronized (httpProxyMock) {
                httpProxyMock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class KerberosHttpProxyMockBuilder {

        public static final int DEFAULT_PROXY_PORT = 3128;
        public static final int DEFAULT_KDC_PORT = 54284;
        public static final long DEFAULT_MINIMUM_TICKET_LIFETIME = 600L;
        public static final long DEFAULT_MAXIMUM_TICKET_LIFETIME = 3600L;
        public static final long DEFAULT_MAXIMUM_RENEWABLE_LIFETIME = 10 * 3600L;
        public static final String DEFAULT_USERNAME = "winfoom";
        public static final String DEFAULT_PASSWORD = "1234";
        public static final String DEFAULT_REALM = "EXAMPLE.COM";
        public static final String DEFAULT_DOMAIN = "localhost";

        private int proxyPort = DEFAULT_PROXY_PORT;
        private String domain = DEFAULT_DOMAIN;

        private List<UsernamePasswordCredentials> credentials =
                Collections.singletonList(new UsernamePasswordCredentials(DEFAULT_USERNAME, DEFAULT_PASSWORD));

        private KdcConfig kdcConfig = getDefaultKdcConfig();

        private SimpleKdcServer simpleKdcServer = getDefaultSimpleKdcServer();

        public KerberosHttpProxyMockBuilder() throws KrbException, UnknownHostException {
        }

        public KerberosHttpProxyMockBuilder withProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public KerberosHttpProxyMockBuilder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public KerberosHttpProxyMockBuilder withCredentials(List<UsernamePasswordCredentials> credentials) {
            this.credentials = credentials;
            return this;
        }

        public static KdcConfig getDefaultKdcConfig() {
            return new KdcConfig() {
                @Override
                public long getMaximumTicketLifetime() {
                    return DEFAULT_MAXIMUM_TICKET_LIFETIME;
                }

                @Override
                public long getMinimumTicketLifetime() {
                    return DEFAULT_MINIMUM_TICKET_LIFETIME;
                }

                @Override
                public boolean isRenewableAllowed() {
                    return true;
                }

                @Override
                public long getMaximumRenewableLifetime() {
                    return DEFAULT_MAXIMUM_RENEWABLE_LIFETIME;
                }
            };
        }

        public static SimpleKdcServer getDefaultSimpleKdcServer() throws KrbException, UnknownHostException {
            SimpleKdcServer simpleKdcServer = new SimpleKdcServer(getDefaultKdcConfig(), new BackendConfig());
            simpleKdcServer.enableDebug();
            simpleKdcServer.setKdcTcpPort(DEFAULT_KDC_PORT);
            simpleKdcServer.setAllowTcp(true);
            simpleKdcServer.setAllowUdp(false);
            simpleKdcServer.setKdcPort(DEFAULT_KDC_PORT);
            simpleKdcServer.setWorkDir(new File("src/test/resources"));
            simpleKdcServer.setKdcHost("0.0.0.0");
            return simpleKdcServer;
        }

        public KerberosHttpProxyMockBuilder withKdcConfig(KdcConfig kdcConfig) {
            this.kdcConfig = kdcConfig;
            return this;
        }

        public KerberosHttpProxyMockBuilder withSimpleKdcServer(SimpleKdcServer simpleKdcServer) {
            this.simpleKdcServer = simpleKdcServer;
            return this;
        }

        public KerberosHttpProxyMock build() throws KrbException {
            Assert.notNull(credentials, "credentials cannot be null");
            return new KerberosHttpProxyMock(proxyPort, domain, credentials, simpleKdcServer);
        }
    }
}
