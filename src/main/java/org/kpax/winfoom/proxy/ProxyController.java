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
import org.kpax.winfoom.util.*;
import org.kpax.winfoom.util.functional.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.support.*;
import org.springframework.core.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.util.*;

import java.io.*;
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
    public synchronized void start() throws IOException {
        Assert.state(!started, "Already started");
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
     * <p>Also, it removes the {@link Authenticator}, if any.
     */
    public synchronized void stop() {
        if (started) {
            started = false;
            resetAllResetableSingletons();

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

    void resetAllResetableSingletons() {
        logger.debug("Reset all resetable singletons");
        Stream.of(applicationContext.getBeanNamesForType(Resetable.class)).
                map(applicationContext.getBeanFactory()::getSingleton).
                filter(Objects::nonNull).sorted(AnnotationAwareOrderComparator.INSTANCE).
                map(b -> (AutoCloseable) b).forEach(InputOutputs::close);
    }

    void restart() throws Exception {
        stop();
        start();
    }

    public boolean isRunning() {
        return started;
    }

}
