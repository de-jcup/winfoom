package org.kpax.winfoom.proxy;

import org.kpax.winfoom.config.ProxySessionScope;
import org.kpax.winfoom.pac.net.IpAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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

    synchronized void start() throws Exception {
        Assert.state(!started, "Already started");
        localProxyServer.start();
        started = true;
    }

    synchronized void stop() {
        started = false;
        proxySessionScope.close();

        IpAddressUtils.ALL_PRIMARY_ADDRESSES.reset();
        IpAddressUtils.PRIMARY_IPv4_ADDRESS.reset();
    }

    public boolean isRunning() {
        return started;
    }
}
