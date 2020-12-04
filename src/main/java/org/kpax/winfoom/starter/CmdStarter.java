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
        if (proxyConfig.isAutodetect()) {
            try {
                boolean result = proxyConfig.autoDetect();
                if (!result) {
                    logger.warn("Failed to retrieve Internet Explorer network settings:");
                }
            } catch (Exception e) {
                logger.error("Error on retrieving Internet Explorer network settings:", e);
            }
        }
        if (proxyConfig.isAutostart()) {
            try {
                proxyConfig.validate();
                proxyController.start();
                logger.info("The local proxy server has been started");
            } catch (InvalidProxySettingsException e) {
                logger.warn("Cannot start local proxy server, invalid proxy configuration", e);
            } catch (Exception e) {
                logger.error("The local proxy server failed to start", e);
            }
        }
        logger.info("Application is ready");
    }
}