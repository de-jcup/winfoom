/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.proxy;

import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.functional.*;

import java.security.*;

/**
 * The {@link CredentialsProvider} for non Windows systems.
 */
public class NonWindowsCredentialsProvider implements CredentialsProvider, StopListener {

    private ProxyConfig proxyConfig;

    private final SingletonSupplier<Credentials> credentialsSupplier = new SingletonSupplier<Credentials>(() -> {
        if (proxyConfig.isKerberos()) {
            return new NoCredentials();
        } else if (proxyConfig.isNtlm()) {
            int backslashIndex = proxyConfig.getProxyHttpUsername().indexOf('\\');
            String username = backslashIndex > -1 ?
                    proxyConfig.getProxyHttpUsername().substring(backslashIndex + 1) : proxyConfig.getProxyHttpUsername();
            String domain = backslashIndex > -1 ?
                    proxyConfig.getProxyHttpUsername().substring(0, backslashIndex) : null;
            return new NTCredentials(username, proxyConfig.getProxyHttpPassword(), null, domain);
        } else {
            return new UsernamePasswordCredentials(proxyConfig.getProxyHttpUsername(),
                    proxyConfig.getProxyHttpPassword());
        }
    });

    public NonWindowsCredentialsProvider(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void setCredentials(AuthScope authscope, Credentials credentials) {
        throw new UnsupportedOperationException("Cannot supply credentials this way");
    }

    @Override
    public Credentials getCredentials(AuthScope authscope) {
        return credentialsSupplier.get();
    }

    @Override
    public void clear() {
        credentialsSupplier.reset();
    }

    @Override
    public void onStop() {
        clear();
    }

    private static class NoCredentials implements Credentials {

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

    }
}
