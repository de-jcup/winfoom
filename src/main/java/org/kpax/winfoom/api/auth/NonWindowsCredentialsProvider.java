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

package org.kpax.winfoom.api.auth;

import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.functional.*;

public class NonWindowsCredentialsProvider implements CredentialsProvider, Resetable {

    private ProxyConfig proxyConfig;

    public NonWindowsCredentialsProvider(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    private SingletonSupplier<NTCredentials> ntCredentialsSupplier = new SingletonSupplier<NTCredentials>(() -> {
        int backslashIndex = proxyConfig.getProxyHttpUsername().indexOf('\\');
        String domain = backslashIndex > -1 ? proxyConfig.getProxyHttpUsername().substring(0, backslashIndex) : null;
        return new NTCredentials(proxyConfig.getProxyHttpUsername(), proxyConfig.getProxyHttpPassword(), null, domain);
    });


    @Override
    public void setCredentials(AuthScope authscope, Credentials credentials) {
        throw new UnsupportedOperationException("Cannot supply credentials this way");
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
        clear();
    }
}
