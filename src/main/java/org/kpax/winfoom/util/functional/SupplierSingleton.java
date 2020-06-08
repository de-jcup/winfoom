package org.kpax.winfoom.util.functional;

import org.apache.commons.lang3.Validate;
import org.kpax.winfoom.util.Throwables;

public abstract class SupplierSingleton<T> extends GenericSingleton<SupplierCallable<T>, T> {

    @Override
    public T get() {
        try {
            return super.get();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            // We should never get here
            throw new RuntimeException(e);
        }
    }

    public static <T> SupplierSingleton<T> of(final SupplierCallable<T> supplier) {
        Validate.notNull(supplier, "supplier cannot be null");
        return new SupplierSingleton<T>() {
            @Override
            protected SupplierCallable<T> supplier() {
                return supplier;
            }
        };
    }

}
