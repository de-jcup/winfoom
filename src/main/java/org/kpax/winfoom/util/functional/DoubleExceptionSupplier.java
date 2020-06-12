package org.kpax.winfoom.util.functional;

@FunctionalInterface
public interface DoubleExceptionSupplier<T, E1 extends Exception, E2 extends Exception> extends TripleExceptionSupplier<T, E1, E2, E2> {
}
