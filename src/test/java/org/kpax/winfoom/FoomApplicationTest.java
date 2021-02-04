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

package org.kpax.winfoom;

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.*;
import org.apache.http.impl.auth.win.WindowsNTLMSchemeFactory;
import org.apache.http.impl.auth.win.WindowsNegotiateSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.util.functional.ProxySingletonSupplier;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class FoomApplicationTest {

    public static void main(String[] args) {
        SpringApplication.run(FoomApplicationTest.class, args);
    }

    @Bean
    @Primary
    public ProxySingletonSupplier<CredentialsProvider> credentialsProvider() {
        return new ProxySingletonSupplier<CredentialsProvider>(() -> {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(TestConstants.USERNAME, TestConstants.PASSWORD));
            return credentialsProvider;
        });
    }

    @Bean
    @Primary
    public ProxySingletonSupplier<Registry<AuthSchemeProvider>> authSchemeRegistrySupplier(ProxyConfig proxyConfig) {
        return new ProxySingletonSupplier<Registry<AuthSchemeProvider>>(() ->
                RegistryBuilder.<AuthSchemeProvider>create()
                        .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                        .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
                        .register(AuthSchemes.NTLM, new NTLMSchemeFactory())
                        .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
                        .register(AuthSchemes.KERBEROS, new KerberosSchemeFactory()).build()
        );
    }

}