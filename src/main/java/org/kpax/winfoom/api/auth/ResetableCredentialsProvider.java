package org.kpax.winfoom.api.auth;

import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.functional.*;

public class ResetableCredentialsProvider implements CredentialsProvider, Resetable {

    private ProxyConfig proxyConfig;

    public ResetableCredentialsProvider(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    private SingletonSupplier<NTCredentials> ntCredentialsSupplier = new SingletonSupplier<NTCredentials>(() -> {
        int backslashIndex = proxyConfig.getProxyHttpUsername().indexOf('\\');
        String domain = backslashIndex > -1 ? proxyConfig.getProxyHttpUsername().substring(0, backslashIndex) : null;
        return new NTCredentials(proxyConfig.getProxyHttpUsername(), proxyConfig.getProxyHttpPassword(), null, domain);
    });


    @Override
    public void setCredentials(AuthScope authscope, Credentials credentials) {

    }

    @Override
    public Credentials getCredentials(AuthScope authscope) {
        return ntCredentialsSupplier.get();
    }

    @Override
    public void clear() {
        ntCredentialsSupplier.reset();
    }

    @Override
    public void close() throws Exception {
        ntCredentialsSupplier.reset();
    }
}
