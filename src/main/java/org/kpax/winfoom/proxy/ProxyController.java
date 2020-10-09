/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;

import org.kpax.winfoom.annotation.ThreadSafe;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.pac.net.IpAddresses;
import org.kpax.winfoom.util.InputOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.stream.Stream;

/**
 * Provide methods to begin, end or clear proxy session.
 *
 * @author Eugen Covaci
 */
@ThreadSafe
@Component
public class ProxyController {

    private final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private LocalProxyServer localProxyServer;

    /**
     * Whether the proxy session is started or not.
     */
    private volatile boolean started;

    /**
     * Begin a proxy session.
     *
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        Assert.state(!started, "Already started");
        if (proxyConfig.getProxyType().isSocks5()) {
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    String proxyPassword = proxyConfig.getProxyPassword();
                    return (new PasswordAuthentication(proxyConfig.getProxyUsername(),
                            proxyPassword != null ? proxyPassword.toCharArray() : new char[0]));
                }
            });
        }
        localProxyServer.start();
        started = true;
    }

    /**
     * End the proxy session.
     * <p>Also, it removes the {@link Authenticator} - if any.link
     */
    public synchronized void stop() {
        if (started) {
            started = false;
            resetAllAutoCloseableSingletons();

            // We reset these suppliers because the network state
            // might have changed during the proxy session.
            // Though unlikely, we take no chances.
            IpAddresses.allPrimaryAddresses.reset();
            IpAddresses.primaryIPv4Address.reset();

            // Remove auth for SOCKS proxy
            if (proxyConfig.getProxyType().isSocks5()) {
                Authenticator.setDefault(null);
            }
        }

    }

    void resetAllAutoCloseableSingletons() {
        logger.debug("Reset all autocloseable singletons");
        Stream.of(applicationContext.getBeanNamesForType(AutoCloseable.class)).
                map(applicationContext.getBeanFactory()::getSingleton).
                filter(b -> b != null).
                sorted(AnnotationAwareOrderComparator.INSTANCE).
                forEach(bean -> {
                    logger.debug("Close bean of type {}", bean.getClass());
                    InputOutputs.close((AutoCloseable) bean);
                });
    }

    public boolean isRunning() {
        return started;
    }

}
