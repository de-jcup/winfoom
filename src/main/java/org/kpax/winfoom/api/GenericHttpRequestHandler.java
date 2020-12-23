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

package org.kpax.winfoom.api;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.entity.*;
import org.apache.http.protocol.*;
import org.kpax.winfoom.annotation.*;
import org.kpax.winfoom.proxy.*;
import org.kpax.winfoom.util.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * The base class for all API request handlers.
 * <p>It provides basic authentication and authorization.
 */
public class GenericHttpRequestHandler implements HttpRequestHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Credentials credentials;

    private final ProxyExecutorService executorService;

    private final int requestTimeout;

    public GenericHttpRequestHandler(@NotNull Credentials credentials, ProxyExecutorService executorService, int requestTimeout) {
        this.credentials = credentials;
        this.executorService = executorService;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        logger.debug("Received request {}", request);
        boolean isAuthorized = handleAuthorization(request, response, context);
        if (isAuthorized) {
            Future<Object> future = executorService.submit(() -> {
                String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
                switch (method) {
                    case "GET":
                        doGet(request, response, context);
                        break;
                    case "POST":
                        doPost(request, response, context);
                        break;
                    case "PUT":
                        doPut(request, response, context);
                        break;
                    case "DELETE":
                        doDelete(request, response, context);
                        break;
                    default:
                        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                        response.setReasonPhrase(String.format("No handler found for %s method", method));
                        break;
                }
                return null;
            });

            try {
                future.get(requestTimeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.debug("Request execution interrupted", e);
                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                response.setEntity(new StringEntity("Command execution interrupted"));
            } catch (ExecutionException e) {
                Throwables.throwIfMatches(e.getCause(), HttpException.class);
                Throwables.throwIfMatches(e.getCause(), IOException.class);
                Throwables.throwIfMatches(e.getCause(), RuntimeException.class);
                logger.debug("Error on executing request", e);
                response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                response.setEntity(new StringEntity("Error on executing command: " + e.getMessage()));

            } catch (TimeoutException e) {
                logger.debug("Request timeout", e);
                response.setStatusCode(HttpStatus.SC_REQUEST_TIMEOUT);
                response.setEntity(new StringEntity("Command timeout"));
            }
        }
    }

    private boolean handleAuthorization(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        boolean isAuthorized = false;
        try {
            isAuthorized = HttpUtils.verifyBasicAuth(request, credentials.getUserPrincipal().getName());
            if (!isAuthorized) {
                response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                response.setEntity(new StringEntity("Incorrect user/password"));
            }
        } catch (AuthenticationException e) {
            logger.warn("Command authorization error", e);
            response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
            response.setEntity(new StringEntity(e.getMessage()));
        }
        logger.debug("Is request authorized? {}", isAuthorized);
        return isAuthorized;
    }

    public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
            throws IOException {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for GET method");
    }

    public void doPost(HttpRequest request, HttpResponse response, HttpContext context)
            throws IOException {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for POST method");
    }

    public void doPut(HttpRequest request, HttpResponse response, HttpContext context) {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for PUT method");
    }

    public void doDelete(HttpRequest request, HttpResponse response, HttpContext context) {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for DELETE method");
    }

}
