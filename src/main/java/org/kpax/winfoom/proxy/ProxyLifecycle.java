package org.kpax.winfoom.proxy;

import org.kpax.winfoom.config.ProxySessionScope;
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
public class ProxyLifecycle {

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
            proxySessionScope.close();

            // We reset these suppliers because the network state
            // might have change meanwhile during the proxy session.
            // It's unlikely though.
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
