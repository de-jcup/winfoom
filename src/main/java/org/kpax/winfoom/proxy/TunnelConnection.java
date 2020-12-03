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
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.protocol.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.routing.RouteInfo.*;
import org.apache.http.entity.*;
import org.apache.http.impl.*;
import org.apache.http.impl.auth.HttpAuthenticator;
import org.apache.http.impl.auth.*;
import org.apache.http.impl.auth.win.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.impl.execchain.TunnelRefusedException;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;
import org.kpax.winfoom.annotation.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.io.*;
import java.net.*;

/**
 * Establish a tunnel via a HTTP proxy.<br>
 * It is an adaptation of {@link org.apache.http.impl.client.ProxyClient}
 *
 * @author Eugen Covaci
 */
@ThreadSafe
@Component
public class TunnelConnection {

    private final Logger logger = LoggerFactory.getLogger(TunnelConnection.class);

    @Autowired
    private CredentialsProvider credentialsProvider;

    @Autowired
    private SystemConfig systemConfig;

    private ConnectionReuseStrategy reuseStrategy;
    private Registry<AuthSchemeProvider> authSchemeRegistry;
    private HttpProcessor httpProcessor;
    private HttpRequestExecutor requestExec;
    private ProxyAuthenticationStrategy proxyAuthStrategy;
    private HttpAuthenticator authenticator;

    @PostConstruct
    void init() {
        this.reuseStrategy = new DefaultConnectionReuseStrategy();
        this.authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                .register(AuthSchemes.NTLM, new WindowsNTLMSchemeFactory(null))
                .register(AuthSchemes.SPNEGO, new WindowsNegotiateSchemeFactory(null))
                .build();
        this.httpProcessor = new ImmutableHttpProcessor(new RequestTargetHost(),
                new RequestClientConnControl(), new RequestUserAgent());
        this.requestExec = new HttpRequestExecutor();
        this.proxyAuthStrategy = new ProxyAuthenticationStrategy();
        this.authenticator = new HttpAuthenticator();
    }

    public Tunnel open(final HttpHost proxy, final HttpHost target,
                       final ProtocolVersion protocolVersion)
            throws IOException, HttpException {
        Args.notNull(proxy, "Proxy host");
        Args.notNull(target, "Target host");

        AuthState proxyAuthState = new AuthState();

        HttpHost host = target;
        if (host.getPort() <= 0) {
            host = new HttpHost(host.getHostName(), 80, host.getSchemeName());
        }
        final HttpRoute route = new HttpRoute(host, RequestConfig.DEFAULT.getLocalAddress(),
                proxy, false, TunnelType.TUNNELLED, LayerType.PLAIN);
        final ManagedHttpClientConnection connection = ManagedHttpClientConnectionFactory.INSTANCE.create(route,
                ConnectionConfig.DEFAULT);
        final HttpContext context = new BasicHttpContext();
        final HttpRequest connect = new BasicHttpRequest(HttpUtils.HTTP_CONNECT, host.toHostString(), protocolVersion);

        // Populate the execution context
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, target);
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, connection);
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, connect);
        context.setAttribute(HttpClientContext.HTTP_ROUTE, route);
        context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, proxyAuthState);
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, credentialsProvider);
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, RequestConfig.DEFAULT);
        context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, authSchemeRegistry);

        requestExec.preProcess(connect, httpProcessor, context);

        HttpResponse response;
        while (true) {
            if (!connection.isOpen()) {
                Socket socket = new Socket(proxy.getHostName(), proxy.getPort());
                systemConfig.configureSocket(socket);
                connection.bind(socket);
            }

            authenticator.generateAuthResponse(connect, proxyAuthState, context);
            response = requestExec.execute(connect, connection, context);

            final int status = response.getStatusLine().getStatusCode();
            logger.debug("Tunnel status code: {}", status);
            if (status < HttpStatus.SC_OK) {
                throw new HttpException("Unexpected response to CONNECT request: " + response.getStatusLine());
            }

            if (authenticator.isAuthenticationRequested(
                    proxy, response, proxyAuthStrategy, proxyAuthState, context)) {
                if (authenticator.handleAuthChallenge(
                        proxy, response, proxyAuthStrategy, proxyAuthState, context)) {
                    // Retry request
                    if (reuseStrategy.keepAlive(response, context)) {
                        // Consume response content
                        logger.debug("Now consume entity");
                        EntityUtils.consume(response.getEntity());
                    } else {
                        logger.debug("Close tunnel connection");
                        InputOutputs.close(connection);
                    }
                    // discard previous auth header
                    connect.removeHeaders(AUTH.PROXY_AUTH_RESP);
                } else {
                    break;
                }
            } else {
                break;
            }

        }

        final int status = response.getStatusLine().getStatusCode();
        logger.debug("Tunnel final status code: {}", status);

        if (status > HttpUtils.MAX_HTTP_SUCCESS_CODE) { // Error case

            // Buffer response content
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                response.setEntity(new BufferedHttpEntity(entity));
            }
            logger.debug("Close tunnel connection");
            InputOutputs.close(connection);
            throw new TunnelRefusedException("CONNECT refused by proxy: " + response.getStatusLine(), response);
        }

        return new Tunnel(connection, response);
    }

}
