package org.kpax.winfoom.util.functional;


/**
 * @param <T>  the type of value supplied by this supplier
 * @see TripleExceptionSingletonSupplier
 */
public final class SingletonSupplier<T> extends SingleExceptionSingletonSupplier<T, RuntimeException> {


    /**
     * Constructor.
     *
     * @param supplier the not null supplier
     */
    public SingletonSupplier(RuntimeExceptionSupplier<T> supplier) {
        super(supplier);
    }
}
