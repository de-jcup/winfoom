package org.kpax.winfoom.util;

import org.apache.commons.lang3.Validate;

import java.util.concurrent.Callable;

public final class SingletonReference<T> {

    private final Object LOCK = new Object();

    private volatile T t;

    private final Callable<T> getter;

    private SingletonReference(Callable<T> getter) {
        this.getter = getter;
    }

    public T get() throws Exception {
        if (t == null) {
            synchronized (LOCK) {
                if (t == null) {
                    t = getter.call();
                }
            }
        }
        return t;
    }

    public static <T> SingletonReference<T> of(Callable<T> getter) {
        Validate.notNull(getter, "getter cannot be null");
        return new SingletonReference<T>(getter);
    }
}
