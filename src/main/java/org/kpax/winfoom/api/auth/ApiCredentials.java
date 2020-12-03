package org.kpax.winfoom.api.auth;

import org.apache.http.auth.*;

import java.security.*;

public class ApiCredentials implements Credentials {

    private Principal principal;

    public ApiCredentials(String apiUserPassword) {
        principal = new BasicUserPrincipal(apiUserPassword);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public String getPassword() {
        return null;
    }
}
