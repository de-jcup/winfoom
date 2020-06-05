package org.kpax.winfoom.pac;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.kpax.winfoom.exception.PacFileException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultPacScriptEvaluatorTests {

    @Test
    void findProxyForURL_AllHelperMethods_NoError()
            throws IOException, URISyntaxException, PacFileException {
        String pacContent = IOUtils.toString(
                Thread.currentThread().getContextClassLoader().
                        getResourceAsStream("proxy-simple-all-helpers.pac"),
                StandardCharsets.UTF_8);
        DefaultPacScriptEvaluator defaultPacScriptEvaluator = new DefaultPacScriptEvaluator(pacContent, false);
        String proxyForURL = defaultPacScriptEvaluator.findProxyForURL(new URI("http://host:80/path?param1=val"));
        assertEquals("DIRECT", proxyForURL);
    }
}
