package org.kpax.winfoom.exception;

import org.kpax.winfoom.util.Throwables;
import org.springframework.util.Assert;

/**
 * A {@link RuntimeException} wrapper for checked exceptions.
 */
public class CheckedExceptionWrapper extends RuntimeException {

    /**
     * Constructor.
     *
     * @param cause a mandatory checked exception
     */
    public CheckedExceptionWrapper(Throwable cause) {
        super(cause);
        Assert.isTrue(Throwables.isChecked(cause), "Only checked exceptions can be wrapped");
    }

}
