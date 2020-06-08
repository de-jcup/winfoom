package org.kpax.winfoom.util.functional;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface SupplierCallable<V> extends Callable<V> {
    @Override
    V call();
}
