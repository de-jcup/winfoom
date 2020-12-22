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

package org.kpax.winfoom.auth;

import com.sun.security.auth.module.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.*;
import org.kpax.winfoom.util.functional.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.config.*;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.*;

import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.*;
import javax.security.auth.login.*;
import java.security.*;
import java.util.*;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class KerberosAuthenticator implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private SystemConfig systemConfig;

    private volatile Krb5LoginModule loginContext;

    private volatile Subject subject;

    private volatile KerberosTicket kerberosTicket;

    public synchronized void authenticate() throws LoginException {
        logger.debug("Attempting Kerberos authentication");

        if (kerberosTicket != null) {

            // Enforcing the minimum login gap policy.
            // This is necessary to avoid useless multiple logins in simultaneous access cases.
            if (kerberosTicket.isCurrent() &&
                    kerberosTicket.getAuthTime() != null &&
                    DateUtils.secondsFromCurrent(kerberosTicket.getAuthTime()) < systemConfig.getKerberosLoginMinInterval()) {
                logger.debug("Already authenticated, nothing to do");
                return;
            }

            if (kerberosTicket.isRenewable()) {
                try {
                    // Try to renew the ticket
                    kerberosTicket.refresh();
                    logger.debug("Ticket refresh succeeded");
                    return;
                } catch (Exception e) {
                    logger.debug("Ticket refresh failed", e);
                }
            }

        }

        if (loginContext != null) {
            try {
                logger.debug("Existent LoginContext found, try logout");
                loginContext.logout();
            } catch (Exception e) {
                logger.warn("Cannot logout the former LoginContext: {}", e.getMessage());
            }
        }

        try {
            loginContext = new Krb5LoginModule();
            Map<String, String> map = new HashMap<>();
            map.put("storeKey", "true");
           // map.put("renewTGT", "true");
           // map.put("useTicketCache", "true");
            subject = new Subject();

            loginContext.initialize(subject, new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) {
                    for (Callback callback : callbacks) {
                        if (callback instanceof NameCallback) {
                            ((NameCallback) callback).setName(proxyConfig.getProxyKrbPrincipal());
                        } else if (callback instanceof PasswordCallback) {
                            ((PasswordCallback) callback).setPassword(proxyConfig.getProxyHttpPassword().toCharArray());
                        }
                    }
                }
            }, null, map);

            logger.debug("About to login principal {}", proxyConfig.getProxyKrbPrincipal());
            loginContext.login();
            loginContext.commit();
        } catch (Exception e) {
            // Cleanup on exception
            logout();
            throw e;
        }

        // Retrieve the Kerberos credentials
        // Get Kerberos ticket
        for (Object o : subject.getPrivateCredentials()) {
            if (o instanceof KerberosTicket) {
                kerberosTicket = ((javax.security.auth.kerberos.KerberosTicket) o);
            }
        }

        logger.debug("kerberosTicket {}", kerberosTicket);
    }

    void logout() {
        if (loginContext != null) {
            try {
                loginContext.abort();
            } catch (Exception e) {
                logger.debug("Error on Kerberos logout: {}", e.getMessage());
            }
        }
        subject = null;
        kerberosTicket = null;
    }

    public Date getLastLoginDate() {
        return kerberosTicket != null ? kerberosTicket.getAuthTime() : null;
    }

    public void execute(PrivilegedActionWrapper action) throws PrivilegedActionException {
        if (subject != null) {
            Subject.doAs(subject, action);
        } else {
            throw new SecurityException("Kerberos authentication not found, you need to login first");
        }
    }


    @Override
    public void close() {
        logout();
    }
}
