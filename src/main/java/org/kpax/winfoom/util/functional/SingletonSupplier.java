package org.kpax.winfoom.util.functional;

import org.springframework.util.Assert;

import java.util.function.Supplier;

public final class SingletonSupplier<T> implements Supplier<T> {


    private final Object LOCK = new Object();

    private final Supplier<T> supplier;

    private volatile T t;

    public SingletonSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (t == null) {
            synchronized (LOCK) {
                if (t == null) {
                    t = supplier.get();
                    Assert.state(t != null, "No instance from supplier");
                }
            }
        }
        return t;
    }

    public void reset() {
        synchronized (LOCK) {
            t = null;
        }
    }

    public boolean hasValue() {
        return t != null;
    }

    public static <T> SingletonSupplier<T> of(final Supplier<T> supplier) {
        Assert.notNull(supplier, "supplier cannot be null");
        return new SingletonSupplier<T>(supplier);

    }

}
