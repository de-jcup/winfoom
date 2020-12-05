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
import org.kpax.winfoom.util.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

/**
 * The base class for all API request handlers.
 * <p>It provides basic authentication and authorization.
 */
public class GenericHttpRequestHandler implements HttpRequestHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Credentials credentials;

    public GenericHttpRequestHandler(@NotNull Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        boolean isAuthorized = handleAuthorization(request, response, context);
        if (isAuthorized) {
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                doGet(request, response, context);
            } else if ("POST".equals(method)) {
                doPost(request, response, context);
            } else if ("PUT".equals(method)) {
                doPut(request, response, context);
            } else if ("DELETE".equals(method)) {
                doDelete(request, response, context);
            } else {
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                response.setReasonPhrase(String.format("No handler found for %s method", method));
            }
        }
    }

    private boolean handleAuthorization(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        boolean result = false;
        try {
            result = HttpUtils.verifyBasicAuth(request, credentials.getUserPrincipal().getName());
            if (!result) {
                response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                response.setEntity(new StringEntity("Incorrect user/password"));
            }
        } catch (AuthenticationException e) {
            logger.warn("Command authorization error", e);
            response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
            response.setEntity(new StringEntity(e.getMessage()));
        }
        return result;
    }

    public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for GET method");
    }

    public void doPost(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for POST method");
    }

    public void doPut(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for PUT method");
    }

    public void doDelete(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_NOT_FOUND);
        response.setReasonPhrase("No handler found for DELETE method");
    }

}
