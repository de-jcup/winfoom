package org.kpax.winfoom.util.functional;

@FunctionalInterface
public interface TripleExceptionSupplier<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E1, E2, E3;
}
