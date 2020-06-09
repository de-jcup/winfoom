package org.kpax.winfoom.exception;

import org.kpax.winfoom.util.Throwables;
import org.springframework.util.Assert;

public class CheckedExceptionWrapper extends RuntimeException {

    public CheckedExceptionWrapper(Throwable cause) {
        super(cause);
        Assert.isTrue(Throwables.isChecked(cause), "Only checked exceptions can be wrapped");
    }

}
