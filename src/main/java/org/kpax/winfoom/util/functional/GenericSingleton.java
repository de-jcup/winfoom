package org.kpax.winfoom.util.functional;

import java.util.concurrent.Callable;

public abstract class GenericSingleton<S extends Callable<T>, T> {

    private final Object LOCK = new Object();

    private volatile T t;

    public T get() throws Exception {
        if (t == null) {
            synchronized (LOCK) {
                if (t == null) {
                    t = supplier().call();
                }
            }
        }
        return t;
    }

    protected abstract S supplier();

    public void reset() {
        synchronized (LOCK) {
            t = null;
        }
    }

    public boolean hasValue() {
        return t != null;
    }

}
