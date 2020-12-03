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
