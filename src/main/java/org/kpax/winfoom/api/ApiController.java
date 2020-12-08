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

import com.fasterxml.jackson.databind.*;
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.entity.*;
import org.apache.http.impl.bootstrap.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.kpax.winfoom.api.auth.*;
import org.kpax.winfoom.api.dto.*;
import org.kpax.winfoom.api.json.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.exception.*;
import org.kpax.winfoom.proxy.*;
import org.kpax.winfoom.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import javax.annotation.*;
import java.io.*;
import java.lang.reflect.*;

/**
 * Open a server and map various request handlers.
 */
@Profile({"!gui & !test"})
@Component
public class ApiController implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HttpServer apiServer;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private ProxyController proxyController;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @PostConstruct
    private void init() throws IOException {
        Credentials credentials = new ApiCredentials(proxyConfig.getApiToken());
        logger.info("Register API request handlers");
        apiServer = ServerBootstrap.bootstrap().setListenerPort(proxyConfig.getApiPort()).
                registerHandler("/start",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'start' command received");
                                try {
                                    proxyConfig.validate();
                                    proxyController.start();
                                    response.setEntity(new StringEntity("The local proxy server has been started"));
                                } catch (InvalidProxySettingsException e) {
                                    response.setEntity(new StringEntity("Invalid configuration: " + e.getMessage()));
                                } catch (Exception e) {
                                    logger.error("Error on starting local proxy server", e);
                                    response.setEntity(new StringEntity("Failed to start the local proxy: " + e.getMessage()));
                                }
                            }
                        }).
                registerHandler("/stop",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'stop' command received");
                                if (proxyController.isRunning()) {
                                    try {
                                        proxyController.stop();
                                        response.setEntity(new StringEntity("The local proxy server is now stopped"));
                                    } catch (Exception e) {
                                        logger.error("Error on stopping local proxy server", e);
                                        response.setEntity(new StringEntity("Failed to stop the local proxy: " + e.getMessage()));
                                    }
                                } else {
                                    response.setEntity(new StringEntity("Already stopped, nothing to do"));
                                }
                            }
                        }).
                registerHandler("/status",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'status' command received");
                                response.setEntity(new StringEntity(String.format("The local proxy server is %s",
                                        proxyController.isRunning() ? "up" : "stopped")));
                            }
                        }).
                registerHandler("/validate",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'validate' command received");
                                boolean running = proxyController.isRunning();
                                if (running) {
                                    try {
                                        applicationContext.getBean(ProxyValidator.class).testProxy();
                                        response.setEntity(new StringEntity("The configuration is valid"));
                                    } catch (InvalidProxySettingsException e) {
                                        logger.debug("Invalid proxy settings", e);
                                        response.setEntity(new StringEntity("The configuration is invalid: " + e.getMessage()));
                                    } catch (IOException e) {
                                        logger.error("Failed to validate proxy settings", e);
                                        response.setEntity(new StringEntity("Error on validation the configuration: " + e.getMessage()));
                                    }
                                } else {
                                    response.setEntity(new StringEntity("The local proxy server is down, you need to start it before validating configuration"));
                                }
                            }
                        }).
                registerHandler("/autodetect",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'autodetect' command received");
                                boolean running = proxyController.isRunning();
                                if (!running) {
                                    try {
                                        boolean result = proxyConfig.autoDetect();
                                        if (result) {
                                            response.setEntity(new StringEntity("Autodetect succeeded"));
                                        } else {
                                            response.setEntity(new StringEntity("No proxy configuration found on your system"));
                                        }
                                    } catch (Exception e) {
                                        logger.error("Error on autodetect proxy settings", e);
                                        response.setEntity(new StringEntity("Error on auto-detecting the configuration: " + e.getMessage()));
                                    }
                                } else {
                                    response.setEntity(new StringEntity("The local proxy server is up, you need to stop it before auto-detecting configuration"));
                                }
                            }
                        }).
                registerHandler("/config",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'config get' command received");
                                try {
                                    response.setEntity(new StringEntity(new ObjectMapper().
                                            configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).
                                            writerWithDefaultPrettyPrinter().
                                            withView(Views.getViewForType((ProxyConfig.Type) proxyConfig.getProxyType())).
                                            writeValueAsString(proxyConfig)));
                                } catch (Exception e) {
                                    logger.error("Error on serializing proxy configuration", e);
                                    response.setEntity(new StringEntity("Failed to get proxy configuration: " + e.getMessage()));
                                }
                            }

                            @Override
                            public void doPost(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'config post' command received");
                                boolean running = proxyController.isRunning();
                                if (running) {
                                    response.setEntity(new StringEntity("The local proxy server is up, you need to stop it before applying configuration"));
                                } else {
                                    if (request instanceof BasicHttpEntityEnclosingRequest) {
                                        BasicHttpEntityEnclosingRequest entityEnclosingRequest = (BasicHttpEntityEnclosingRequest) request;
                                        try {
                                            ProxyConfigDto proxyConfigDto = new ObjectMapper().
                                                    readValue(entityEnclosingRequest.getEntity().getContent(),
                                                            ProxyConfigDto.class);
                                            proxyConfigDto.validate();
                                            BeanUtils.copyNonNullProperties(proxyConfigDto, proxyConfig);
                                            response.setEntity(new StringEntity("Proxy configuration applied"));
                                        } catch (IOException e) {
                                            logger.error("Error on parsing JSON", e);
                                            response.setEntity(new StringEntity("Failed to parse JSON: " + e.getMessage()));
                                        } catch (InvalidProxySettingsException e) {
                                            logger.error("Invalid JSON", e);
                                            response.setEntity(new StringEntity("Invalid JSON: " + e.getMessage()));
                                        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                                            logger.error("Error on applying proxy configuration", e);
                                            response.setEntity(new StringEntity("Failed to apply proxy configuration: " + e.getMessage()));
                                        }
                                    } else {
                                        response.setEntity(new StringEntity("Failed to apply proxy configuration: no JSON found"));
                                    }
                                }
                            }
                        }).
                registerHandler("/settings",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'settings' command received");
                                try {
                                    response.setEntity(new StringEntity(new ObjectMapper().
                                            configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).
                                            writerWithDefaultPrettyPrinter().
                                            withView(Views.getSettingsView()).
                                            writeValueAsString(proxyConfig)));
                                } catch (Exception e) {
                                    logger.error("Error on serializing proxy settings", e);
                                    response.setEntity(new StringEntity("Failed to get proxy settings: " + e.getMessage()));
                                }
                            }

                            @Override
                            public void doPost(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'settings post' command received");
                                boolean running = proxyController.isRunning();
                                if (running) {
                                    response.setEntity(new StringEntity("The local proxy server is up, you need to stop it before applying configuration"));
                                } else {
                                    if (request instanceof BasicHttpEntityEnclosingRequest) {
                                        BasicHttpEntityEnclosingRequest entityEnclosingRequest = (BasicHttpEntityEnclosingRequest) request;
                                        try {
                                            ProxyConfigDto proxyConfigDto = new ObjectMapper().
                                                    readValue(entityEnclosingRequest.getEntity().getContent(),
                                                            ProxyConfigDto.class);
                                            proxyConfigDto.validate();
                                            BeanUtils.copyNonNullProperties(proxyConfigDto, proxyConfig);
                                            response.setEntity(new StringEntity("Proxy settings applied"));
                                        } catch (IOException e) {
                                            logger.error("Error on parsing JSON", e);
                                            response.setEntity(new StringEntity("Failed to parse JSON: " + e.getMessage()));
                                        } catch (InvalidProxySettingsException e) {
                                            logger.error("Invalid JSON", e);
                                            response.setEntity(new StringEntity("Invalid JSON: " + e.getMessage()));
                                        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                                            logger.error("Error on applying proxy settings", e);
                                            response.setEntity(new StringEntity("Failed to apply proxy settings: " + e.getMessage()));
                                        }
                                    } else {
                                        response.setEntity(new StringEntity("Failed to apply proxy settings: no JSON found"));
                                    }
                                }
                            }
                        }).

                registerHandler("/shutdown",
                        new GenericHttpRequestHandler(credentials) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'shutdown' command received");
                                applicationContext.close();
                                response.setEntity(new StringEntity("Application has been shutdown"));
                            }
                        }).create();
        apiServer.start();
    }


    @Override
    public void close() {
        logger.debug("Stop the api server");
        apiServer.stop();
    }
}
