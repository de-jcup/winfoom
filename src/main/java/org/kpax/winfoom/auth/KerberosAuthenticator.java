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

import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.*;
import javax.security.auth.login.*;
import java.security.*;
import java.util.*;


public class KerberosAuthenticator implements AutoCloseable {

    public static final int MIN_LOGIN_INTERVAL = 30;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyConfig proxyConfig;

    private volatile Krb5LoginModule loginContext;

    private volatile Subject subject;

    private volatile KerberosTicket kerberosTicket;

    KerberosAuthenticator(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public synchronized void authenticate() throws LoginException {
        logger.debug("Attempting Kerberos authentication");

        if (kerberosTicket != null &&
                kerberosTicket.isCurrent() &&
                DateUtils.secondBetween(kerberosTicket.getAuthTime(), new Date()) < MIN_LOGIN_INTERVAL) {
            logger.debug("Already authenticated, nothing to do");
            return;
        }

        if (loginContext != null) {
            try {
                logger.debug("Existent LoginContext found, try logout");
                loginContext.logout();
            } catch (Exception e) {
                logger.warn("Cannot logout the former LoginContext, proceed with login", e);
            }
        }

        try {
            loginContext = new Krb5LoginModule();
            Map<String, String> map = new HashMap<>();
            map.put("storeKey", "true");
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
                loginContext.logout();
            } catch (Exception e) {
                logger.debug("Error on Kerberos logout", e);
            }
        }
        subject = null;
        kerberosTicket = null;
    }

    public Date getLastLoginDate() {
        return kerberosTicket != null ? kerberosTicket.getAuthTime() : null;
    }

    public <E1 extends Exception, E2 extends Exception> void execute(
            PrivilegedActionWrapper action,
            Class<E1> cls1,
            Class<E2> cls2)
            throws E1, E2, PrivilegedActionException {
        if (subject != null) {
            try {
                Subject.doAs(subject, action);
            } catch (PrivilegedActionException e) {
                logger.debug("Error on executing action", e);
                Throwables.throwIfMatches(e.getException(), cls1, cls2);
                throw e;
            }
        } else {
            throw new SecurityException("JAAS authentication not found");
        }
    }

    public <E1 extends Exception, E2 extends Exception, E3 extends Exception> void execute(
            PrivilegedActionWrapper action,
            Class<E1> cls1,
            Class<E2> cls2,
            Class<E3> cls3)
            throws E1, E2, E3, PrivilegedActionException {
        if (subject != null) {
            try {
                Subject.doAs(subject, action);
            } catch (PrivilegedActionException e) {
                logger.debug("Error on executing action", e);
                Throwables.throwIfMatches(e.getException(), cls1, cls2, cls3);
                throw e;
            }
        } else {
            throw new SecurityException("JAAS authentication not found");
        }
    }

    public <E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception> void execute(
            PrivilegedActionWrapper action,
            Class<E1> cls1,
            Class<E2> cls2,
            Class<E3> cls3,
            Class<E4> cls4)
            throws E1, E2, E3, E4, PrivilegedActionException {
        if (subject != null) {
            try {
                Subject.doAs(subject, action);
            } catch (PrivilegedActionException e) {
                logger.debug("Error on executing action ", e);
                Throwables.throwIfMatches(e.getException(), cls1, cls2, cls3, cls4);
                throw e;
            }
        } else {
            throw new SecurityException("JAAS authentication not found");
        }
    }


    @Override
    public void close() throws Exception {
        logout();
    }
}
