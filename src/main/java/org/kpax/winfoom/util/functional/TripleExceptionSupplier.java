package org.kpax.winfoom.util.functional;

/**
 * Generic supplier.
 *
 * @param <T>  the type of value supplied by this supplier
 * @param <E1> the first {@link Exception} type
 * @param <E2> the second {@link Exception} type
 * @param <E3> the third {@link Exception} type
 */
@FunctionalInterface
public interface TripleExceptionSupplier<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E1, E2, E3;
}
