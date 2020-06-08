package org.kpax.winfoom.util.functional;

import org.apache.commons.lang3.Validate;
import org.kpax.winfoom.exception.ResetNotSupportedException;

import java.util.concurrent.Callable;

public abstract class Singleton<T> extends GenericSingleton<Callable<T>, T> {

    @Override
    public void reset() throws ResetNotSupportedException {
        throw new ResetNotSupportedException();
    }

    public static <T> Singleton<T> of(final Callable<T> supplier) {
        Validate.notNull(supplier, "getter cannot be null");
        return new Singleton<T> () {
            @Override
            protected Callable<T> supplier() {
                return supplier;
            }
        };
    }
}
