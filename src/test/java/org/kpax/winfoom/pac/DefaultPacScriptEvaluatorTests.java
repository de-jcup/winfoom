package org.kpax.winfoom.pac;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kpax.winfoom.FoomApplicationTest;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.PacScriptException;
import org.kpax.winfoom.proxy.ProxyInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = FoomApplicationTest.class)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefaultPacScriptEvaluatorTests {

    @MockBean
    private ProxyConfig proxyConfig;

    @Lazy
    @Autowired
    private DefaultPacScriptEvaluator defaultPacScriptEvaluator;

    @BeforeEach
    void beforeEach () throws IOException {
        when(proxyConfig.getProxyPacFileLocationAsURL()).thenReturn(new File("./src/test/resources/proxy-simple-all-helpers.pac").toURI().toURL());

    }

    @Test
    void findProxyForURL_AllHelperMethods_NoError()
            throws URISyntaxException, PacScriptException {
        System.out.println(proxyConfig.getProxyPacFileLocation());
        List<ProxyInfo> proxies = defaultPacScriptEvaluator.findProxyForURL(new URI("http://host:80/path?param1=val"));
        assertEquals(1, proxies.size());
        assertTrue(proxies.get(0).getType().isDirect());
    }
}
