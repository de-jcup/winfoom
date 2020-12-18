package org.kpax.winfoom.util.functional;

import java.security.*;

public interface PrivilegedExceptionRunnable extends PrivilegedExceptionAction {

    void execute () throws Exception;

    @Override
    default Object run() throws Exception {
        execute();
        return null;
    }
}
