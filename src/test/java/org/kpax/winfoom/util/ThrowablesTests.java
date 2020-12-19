package org.kpax.winfoom.util;

import org.apache.http.*;
import org.junit.jupiter.api.*;
import org.kpax.winfoom.exception.*;

import java.io.*;

public class ThrowablesTests {

    @Test
    public void throwIfMatches_fourExceptions_throwOne () {
        Assertions.assertThrows(IOException.class, () ->
                Throwables.throwIfMatches(new FileNotFoundException(), IOException.class, HttpException.class, RuntimeException.class, ProxyAuthorizationException.class));
    }
}
