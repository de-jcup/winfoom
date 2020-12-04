package org.kpax.winfoom.proxy;

import org.apache.http.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.functional.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

@Component
public class ManualProxyInfoHolder implements Resetable {

    @Autowired
    private ProxyConfig proxyConfig;

    private SingletonSupplier<ProxyInfo> proxyInfoSupplier = new SingletonSupplier<>(() -> {
        HttpHost proxyHost = proxyConfig.getProxyType().isDirect() ? null :
                new HttpHost(proxyConfig.getProxyHost(), proxyConfig.getProxyPort());
        return new ProxyInfo(proxyConfig.getProxyType(), proxyHost);
    });

    public ProxyInfo getProxyInfo() {
        return proxyInfoSupplier.get();
    }

    @Override
    public void close() throws Exception {
        proxyInfoSupplier.reset();
    }
}
