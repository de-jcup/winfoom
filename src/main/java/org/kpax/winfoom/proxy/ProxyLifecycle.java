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

import org.kpax.winfoom.pac.net.IpAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Manage a proxy session (start/stop).
 * <p>The proxy session begins with {@link #start()}
 * and ends with {@link #stop()}.
 *
 * @see ProxySessionScope
 */
@Component
class ProxyLifecycle {

    @Autowired
    private ProxySessionScope proxySessionScope;

    @Autowired
    private LocalProxyServer localProxyServer;

    /**
     * Whether this manager is started or not.
     */
    private volatile boolean started;

    /**
     * Begin a proxy session.
     *
     * @throws Exception
     */
    synchronized void start() throws Exception {
        Assert.state(!started, "Already started");
        localProxyServer.start();
        started = true;
    }

    /**
     * <p>End the current proxy session, if any.
     * <p>Does nothing if it's not running.
     */
    synchronized void stop() {
        if (started) {
            started = false;
            proxySessionScope.clear();

            // We reset these suppliers because the network state
            // might have changed during the proxy session.
            // Though unlikely, we take no chances.
            IpAddressUtils.allPrimaryAddresses.reset();
            IpAddressUtils.primaryIPv4Address.reset();
        }
    }

    /**
     * Check if there is an active proxy session.
     *
     * @return {@code true} iff there is an active proxy session
     */
    public boolean isRunning() {
        return started;
    }
}
