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

import org.kpax.winfoom.annotation.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.pac.net.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.support.*;
import org.springframework.core.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.util.*;

import java.net.*;
import java.util.*;
import java.util.stream.*;

/**
 * Provide methods to begin, end proxy session.
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
        List<StartupListener> startupListeners = Stream.of(applicationContext.getBeanNamesForType(StartupListener.class)).
                map(applicationContext.getBeanFactory()::getSingleton).
                filter(Objects::nonNull).map(b -> (StartupListener) b).collect(Collectors.toList());
        try {
            for (StartupListener startupListener : startupListeners) {
                TypeQualifier typeQualifier = startupListener.getClass().getMethod("onStart").
                        getDeclaredAnnotation(TypeQualifier.class);
                if (typeQualifier == null || typeQualifier.value() == proxyConfig.getProxyType()) {
                    logger.debug("Call onBeforeStart for: {}", startupListener.getClass());
                    startupListener.onStart();
                } else {
                    logger.debug("onBeforeStart ignored for {}", startupListener.getClass());
                }
            }
        } catch (Exception e) {
            resetState();
            throw e;
        }
        if (proxyConfig.getProxyType().isSocks5()) {
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    String proxyPassword = proxyConfig.getProxySocks5Password();
                    return (new PasswordAuthentication(proxyConfig.getProxySocks5Username(),
                            proxyPassword != null ? proxyPassword.toCharArray() : new char[0]));
                }
            });
        }
        localProxyServer.start();
        started = true;
    }

    /**
     * End the proxy session.
     */
    public synchronized void stop() {
        if (started) {
            started = false;
            resetState();
        } else {
            logger.info("Already stopped, nothing to do");
        }
    }

    /**
     * Reset the Spring beans state.
     * <p>Also, it removes the {@link Authenticator}, if any.
     */
    private void resetState() {
        callStopListeners();

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

    void callStopListeners() {
        logger.debug("Call all StopListener.afterStop singletons");
        Stream.of(applicationContext.getBeanNamesForType(StopListener.class)).
                map(applicationContext.getBeanFactory()::getSingleton).
                filter(Objects::nonNull).sorted(AnnotationAwareOrderComparator.INSTANCE).
                map(b -> (StopListener) b).forEach(StopListener::onStop);
    }

    void restart() throws Exception {
        stop();
        start();
    }

    public boolean isRunning() {
        return started;
    }

}
