/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.util;

import org.apache.commons.lang3.exception.*;
import org.kpax.winfoom.annotation.*;
import org.springframework.util.*;

import java.util.*;

/**
 * Various exception related utility methods.
 */
public class Throwables {

    private Throwables() {
    }

    /**
     * Returns the first {@link Throwable} object in the exception chain that matches one of the provided types.
     *
     * @param throwable the throwable to inspect (not null)
     * @param types     the filtering types
     * @return the {@link Optional} wrapping the root cause
     */
    public static Optional<Throwable> getRootCause(Throwable throwable, Class<?>... types) {
        Assert.notNull(throwable, "throwable cannot be null");
        if (types == null || types.length == 0) {
            return Optional.of(ExceptionUtils.getRootCause(throwable));
        }
        List<Class<?>> typesList = Arrays.asList(types);
        return ExceptionUtils.getThrowableList(throwable).stream().
                filter(e -> typesList.stream().anyMatch(type -> type.isAssignableFrom(e.getClass()))).findAny();
    }

    /**
     * Throw the {@link Throwable} only if it is unchecked, does nothing otherwise.
     *
     * @param throwable the {@link Throwable} instance
     */
    public static void throwIfUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }

    /**
     * Check if a {@link Throwable} instance is an unchecked exception
     *
     * @param throwable the {@link Throwable} instance to be inspected
     * @return {@code true} iff it is an unchecked exception
     */
    public static boolean isUnchecked(Throwable throwable) {
        return throwable instanceof RuntimeException || throwable instanceof Error;
    }

    /**
     * Check if a {@link Throwable} instance is a checked exception
     *
     * @param throwable the {@link Throwable} instance to be inspected
     * @return {@code true} iff it is a checked exception
     * @see #isUnchecked(Throwable)
     */
    public static boolean isChecked(Throwable throwable) {
        return !isUnchecked(throwable);
    }

    public static <T extends Exception> void throwIfMatches(@NotNull Exception e,
                                                            @NotNull Class<T> cls) throws T {
        if (e.getClass().isAssignableFrom(cls)) {
            throw cls.cast(e);
        }
    }

    public static <T extends Exception,
            U extends Exception> void throwIfMatches(@NotNull Exception e,
                                                     @NotNull Class<T> cls1,
                                                     @NotNull Class<U> cls2) throws T, U {
        if (cls1.isAssignableFrom(e.getClass())) {
            throw cls1.cast(e);
        }

        if (cls2.isAssignableFrom(e.getClass())) {
            throw cls2.cast(e);
        }
    }

    public static <E1 extends Exception,
            E2 extends Exception,
            E3 extends Exception> void throwIfMatches(@NotNull Exception e,
                                                      @NotNull Class<E1> cls1,
                                                      @NotNull Class<E2> cls2,
                                                      @NotNull Class<E3> cls3) throws E1, E2, E3 {
        throwIfMatches(e, cls1, cls2);
        if (cls3.isAssignableFrom(e.getClass())) {
            throw cls3.cast(e);
        }
    }

    public static <E1 extends Exception,
            E2 extends Exception,
            E3 extends Exception,
            E4 extends Exception> void throwIfMatches(@NotNull Exception e,
                                                      @NotNull Class<E1> cls1,
                                                      @NotNull Class<E2> cls2,
                                                      @NotNull Class<E3> cls3,
                                                      @NotNull Class<E4> cls4
    ) throws E1, E2, E3, E4 {
        throwIfMatches(e, cls1, cls2, cls3);
        if (cls4.isAssignableFrom(e.getClass())) {
            throw cls4.cast(e);
        }
    }

}
