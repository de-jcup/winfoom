package org.kpax.winfoom.util.functional;

@FunctionalInterface
public interface SingleExceptionSupplier<T, E extends Exception> extends DoubleExceptionSupplier<T, E, E> {
}
