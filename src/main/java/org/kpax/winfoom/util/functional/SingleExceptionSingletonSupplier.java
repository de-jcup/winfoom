/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

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
