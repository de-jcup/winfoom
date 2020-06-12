package org.kpax.winfoom.util.functional;

/**
 * @param <T>  the type of value supplied by this supplier
 * @param <E1> the first {@link Exception} type
 * @param <E2> the second {@link Exception} type
 * @see TripleExceptionSingletonSupplier
 */
public class DoubleExceptionSingletonSupplier<T, E1 extends Exception, E2 extends Exception> extends TripleExceptionSingletonSupplier<T, E1, E2, E2> {


    /**
     * Constructor.
     *
     * @param supplier the not null supplier
     */
    public DoubleExceptionSingletonSupplier(DoubleExceptionSupplier<T, E1, E2> supplier) {
        super(supplier);
    }
}
