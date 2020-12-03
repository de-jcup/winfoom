package org.kpax.winfoom.starter;

import org.kpax.winfoom.config.*;
import org.kpax.winfoom.exception.*;
import org.kpax.winfoom.proxy.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.context.event.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

@Profile("!gui")
@Component
public class CmdStarter implements ApplicationListener<ApplicationReadyEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private ProxyController proxyController;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("Application is now started");
        if (proxyConfig.isAutodetect()) {
            try {
                boolean result = proxyConfig.autoDetect();
                if (!result) {
                    System.out.println("Failed to retrieve Internet Explorer network settings:");
                }
            } catch (Exception e) {
                logger.error("Error on auto-detecting system settings", e);
                System.err.println("Error on retrieving Internet Explorer network settings: " + e.getMessage());
            }
        }
        if (proxyConfig.isAutostart()) {
            try {
                proxyConfig.validate();
                proxyController.start();
                System.out.println("The local proxy server has been started");
            } catch (InvalidProxySettingsException e) {
                logger.warn("Invalid proxy configuration", e);
                System.err.println("The local proxy server cannot be started, the configuration is invalid: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Error on starting local proxy server", e);
                System.err.println("The local proxy server failed to start: " + e.getMessage());
            }
        } else {
            System.out.println("The local proxy is currently stopped");
        }
        System.out.println("Press CTRL+C to return");
    }
}