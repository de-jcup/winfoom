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

package org.kpax.winfoom.config;

import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.config.*;
import org.apache.http.impl.auth.*;
import org.apache.http.impl.auth.win.*;
import org.apache.http.impl.client.*;
import org.kpax.winfoom.api.auth.*;
import org.springframework.context.annotation.*;

/**
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 1/24/2020
 */
@Configuration
class WebConfiguration {

    /**
     * Create the default system wide {@link CredentialsProvider} for Windows OS.
     * <p>Note: Only works with HTTP proxies.
     *
     * @return the system wide {@link CredentialsProvider}
     */
    @Profile("windows")
    @Bean
    public CredentialsProvider windowsCredentialsProvider() {
        return new WindowsCredentialsProvider(new SystemDefaultCredentialsProvider());
    }

    @Profile("!windows")
    @Bean
    public CredentialsProvider nonWindowsCredentialsProvider(ProxyConfig proxyConfig) {
        return new NonWindowsCredentialsProvider(proxyConfig);
    }

    @Profile("windows")
    @Bean
    public Registry<AuthSchemeProvider> windowsAuthSchemeRegistry() {
        return RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                .register(AuthSchemes.NTLM, new WindowsNTLMSchemeFactory(null))
                .register(AuthSchemes.SPNEGO, new WindowsNegotiateSchemeFactory(null))
                .build();
    }

    @Profile("!windows")
    @Bean
    public Registry<AuthSchemeProvider> nonWindowsAuthSchemeRegistry() {
        return RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                .register(AuthSchemes.NTLM, new WindowsNTLMSchemeFactory(null))
                .build();
    }

}
