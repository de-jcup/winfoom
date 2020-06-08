package org.kpax.winfoom.util;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.List;

public class Throwables {

    private Throwables() {
    }

    public static Throwable getRootCause(Throwable throwable, Class<?>... types) {
        Validate.notNull(throwable, "throwable cannot be null");
        if (types == null || types.length == 0) {
            return ExceptionUtils.getRootCause(throwable);
        }
        List<Class<?>> typesList = Arrays.asList(types);
        return ExceptionUtils.getThrowableList(throwable).stream().
                filter(e -> typesList.stream().anyMatch(type -> type.isAssignableFrom(e.getClass()))).findAny().orElse(throwable);
    }

    public static void throwIfUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }


}
