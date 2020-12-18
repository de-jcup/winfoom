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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProxyConfig proxyConfig;

    private volatile Krb5LoginModule loginContext;

    private volatile Subject subject;

    private volatile KerberosTicket kerberosTicket;

    KerberosAuthenticator(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public synchronized boolean authenticate() throws LoginException {
        if (kerberosTicket != null &&
                kerberosTicket.getEndTime().after(new Date())) {
            logger.debug("Already authenticated, nothing to do");
            return false;
        }

        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException e) {
                logger.warn("Cannot logout the former LoginContext, proceed with login", e);
            }
        }

        loginContext = new Krb5LoginModule();
        Map<String, String> map = new HashMap<>();
        map.put("storeKey", "true");
//        map.put("doNotPrompt", "true"); ???

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

        loginContext.login();
        loginContext.commit();

        // Retrieve the Kerberos credentials
        // Get Kerberos ticket
        for (Object o : subject.getPrivateCredentials()) {
            if (o instanceof KerberosTicket) {
                kerberosTicket = ((javax.security.auth.kerberos.KerberosTicket) o);
            }
        }

        logger.debug("kerberosTicket {}", kerberosTicket);

        return true;
    }

    void logout() {
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (Exception e) {
                logger.debug("Error on JAAS logout", e);
            }
        }
        subject = null;
        kerberosTicket = null;

    }

    public <E1 extends Exception, E2 extends Exception, E3 extends Exception> void execute(
            PrivilegedExceptionRunnable action,
            Class<E1> cls1,
            Class<E2> cls2,
            Class<E3> cls3)
            throws E1, E2, E3 {
        if (loginContext != null) {
            try {
                Subject.doAs(subject, action);
            } catch (PrivilegedActionException e) {
                Throwables.throwIfMatches(e.getException(), cls1, cls2, cls3);
            }
        } else {
            throw new SecurityException("JAAS authentication not found");
        }
    }

    public <E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception> void execute(
            PrivilegedExceptionRunnable action,
            Class<E1> cls1,
            Class<E2> cls2,
            Class<E3> cls3,
            Class<E4> cls4)
            throws E1, E2, E3, E4 {
        if (loginContext != null) {
            try {
                Subject.doAs(subject, action);
            } catch (PrivilegedActionException e) {
                Throwables.throwIfMatches(e.getException(), cls1, cls2, cls3, cls4);
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
