package org.kpax.winfoom.util.functional;


/**
 * @param <T> the type of value supplied by this supplier
 * @param <E> the {@link Exception} type
 * @see DoubleExceptionSingletonSupplier
 * @see TripleExceptionSingletonSupplier
 */
public class SingleExceptionSingletonSupplier<T, E extends Exception> extends DoubleExceptionSingletonSupplier<T, E, E> {


    /**
     * Constructor.
     *
     * @param supplier the not null supplier
     */
    public SingleExceptionSingletonSupplier(SingleExceptionSupplier<T, E> supplier) {
        super(supplier);
    }
}
