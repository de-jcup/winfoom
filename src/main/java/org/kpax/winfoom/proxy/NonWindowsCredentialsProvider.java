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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.functional.*;
import org.springframework.util.*;

/**
 * The {@link CredentialsProvider} for non Windows systems.
 * Currently, only NTLM protocol is supported.
 */
public class NonWindowsCredentialsProvider implements CredentialsProvider, StopListener {

    private ProxyConfig proxyConfig;

    public NonWindowsCredentialsProvider(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    private SingletonSupplier<NTCredentials> ntCredentialsSupplier = new SingletonSupplier<NTCredentials>(() -> {
        Assert.state(StringUtils.isNotEmpty(proxyConfig.getProxyHttpUsername()), "proxyConfig.proxyHttpUsername cannot be empty");
        Assert.state(StringUtils.isNotEmpty(proxyConfig.getProxyHttpPassword()), "proxyConfig.proxyHttpPassword cannot be empty");
        int backslashIndex = proxyConfig.getProxyHttpUsername().indexOf('\\');
        Assert.state(backslashIndex < proxyConfig.getProxyHttpUsername().length() - 1, "proxyConfig.proxyHttpUsername is invalid: missing username");
        String domain = backslashIndex > -1 ? proxyConfig.getProxyHttpUsername().substring(0, backslashIndex) : null;
        String username = backslashIndex > -1 ? proxyConfig.getProxyHttpUsername().substring(backslashIndex + 1) : proxyConfig.getProxyHttpUsername();
        return new NTCredentials(username, proxyConfig.getProxyHttpPassword(), null, domain);
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
    public void onStop() {
        clear();
    }

}
