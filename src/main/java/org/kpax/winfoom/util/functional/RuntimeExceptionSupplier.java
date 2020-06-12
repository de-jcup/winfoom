package org.kpax.winfoom.util.functional;

@FunctionalInterface
public interface RuntimeExceptionSupplier<T> extends SingleExceptionSupplier<T, RuntimeException> {
}
