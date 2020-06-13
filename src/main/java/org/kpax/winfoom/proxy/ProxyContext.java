/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;

import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.ScopeConfiguration;
import org.kpax.winfoom.pac.net.IpAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.net.Authenticator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provide a thread pool for async execution also allows begin/end proxy session.
 * <p>We rely on the Spring context to close this instance!
 *
 * @author Eugen Covaci
 */
@Component
public class ProxyContext implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(ProxyContext.class);

    private final ProxyLifecycle proxyLifecycle = new ProxyLifecycle();

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private ScopeConfiguration scopeConfiguration;

    @Autowired
    private LocalProxyServer localProxyServer;

    private ThreadPoolExecutor threadPool;

    @PostConstruct
    private void init() {
        logger.info("Create thread pool");

        this.threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new DefaultThreadFactory());

        logger.info("Done proxy context's initialization");
    }

    /**
     * Begin a proxy session by calling {@link  ProxyLifecycle#start()} .
     *
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        proxyLifecycle.start();
    }

    /**
     * End the proxy session by calling {@link  ProxyLifecycle#stop()}.
     * <p>Also, it removes the {@link Authenticator} - if any.
     */
    public synchronized void stop() {
        proxyLifecycle.stop();

        // Remove auth for SOCKS proxy
        if (proxyConfig.getProxyType().isSocks5()) {
            Authenticator.setDefault(null);
        }

    }

    public boolean isRunning() {
        return proxyLifecycle.isRunning();
    }

    public ExecutorService executorService() {
        return threadPool;
    }

    @Override
    public void close() {
        logger.info("Close all context's resources");
        stop();
        try {
            threadPool.shutdownNow();
        } catch (Exception e) {
            logger.warn("Error on closing thread pool", e);
        }

    }

    public static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public DefaultThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            group = (securityManager != null) ? securityManager.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(group, runnable,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);

            // Make sure all threads are daemons!
            if (!thread.isDaemon()) {
                thread.setDaemon(true);
            }

            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }

    /**
     * Manage a proxy session (start/stop).
     * <p>The proxy session begins with {@link #start()}
     * and ends with {@link #stop()}.
     *
     * @see ProxySessionScope
     */
    private class ProxyLifecycle {

        /**
         * Whether this manager is started or not.
         */
        private volatile boolean started;

        /**
         * Begin a proxy session.
         *
         * @throws Exception
         */
        void start() throws Exception {
            Assert.state(!started, "Already started");
            localProxyServer.start();
            started = true;
        }

        /**
         * <p>End the current proxy session, if any.
         * <p>Does nothing if it's not running.
         */
        void stop() {
            if (started) {
                started = false;
                scopeConfiguration.getProxySessionScope().clear();

                // We reset these suppliers because the network state
                // might have changed during the proxy session.
                // Though unlikely, we take no chances.
                IpAddresses.allPrimaryAddresses.reset();
                IpAddresses.primaryIPv4Address.reset();
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

}
