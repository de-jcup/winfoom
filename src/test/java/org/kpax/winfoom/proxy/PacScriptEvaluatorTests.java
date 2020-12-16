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
import org.apache.http.entity.*;
import org.apache.http.impl.bootstrap.*;
import org.apache.http.protocol.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.kpax.winfoom.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.exception.*;
import org.kpax.winfoom.pac.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.context.*;
import org.springframework.test.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = FoomApplicationTest.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PacScriptEvaluatorTests {
    @MockBean
    private ProxyConfig proxyConfig;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PacScriptEvaluator pacScriptEvaluator;

    @Autowired
    private ProxyController proxyController;

    private HttpServer remoteServer;

    @BeforeAll
    void beforeAll() throws IOException {
        remoteServer = ServerBootstrap.bootstrap().registerHandler("/pacFile", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) {
                response.setEntity(new InputStreamEntity(getClass().getClassLoader().getResourceAsStream("proxy-simple.pac")));
            }
        }).create();
        remoteServer.start();
    }

    @Test
    void loadPacFileContent_validLocalFile_NoError() throws Exception {
        when(proxyConfig.getProxyPacFileLocationAsURL()).thenReturn(getClass().getClassLoader().getResource("proxy-simple.pac"));
        proxyController.callStopListeners();
        pacScriptEvaluator.onStart();
        pacScriptEvaluator.findProxyForURL(new URI("http://google.com"));
    }

    @Test
    void loadPacFileContent_validRemoteFile_NoError() throws Exception {
        when(proxyConfig.getProxyPacFileLocationAsURL()).thenReturn(new URL("http://localhost:" + remoteServer.getLocalPort() + "/pacFile"));
        proxyController.callStopListeners();
        pacScriptEvaluator.onStart();
        pacScriptEvaluator.findProxyForURL(new URI("http://google.com"));
    }


    @Test
    void loadPacFileContent_invalidLocalFile_PacFileException() throws Exception {
        when(proxyConfig.getProxyPacFileLocationAsURL()).thenReturn(getClass().getClassLoader().getResource("proxy-invalid.pac"));
        proxyController.callStopListeners();
        Assertions.assertThrows(PacFileException.class, () -> {
            pacScriptEvaluator.onStart();
            pacScriptEvaluator.findProxyForURL(new URI("http://google.com"));
        });
    }

    @Test
    void findProxyForURL_AllHelperMethods_NoError()
            throws Exception {
        when(proxyConfig.getProxyPacFileLocationAsURL()).
                thenReturn(getClass().getClassLoader().getResource("proxy-simple-all-helpers.pac"));
        proxyController.callStopListeners();
        pacScriptEvaluator.onStart();
        List<ProxyInfo> proxies = pacScriptEvaluator.findProxyForURL(new URI("http://host:80/path?param1=val"));
        assertEquals(1, proxies.size());
        assertTrue(proxies.get(0).getType().isDirect());
    }

    @AfterAll
    void after() {
        remoteServer.shutdown(0, TimeUnit.MILLISECONDS);
    }
}
