/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.util.functional;

import org.springframework.util.*;

import java.awt.*;
import java.util.concurrent.*;

public class InvokeLaterCallable<V> implements Callable<V> {

    private final Runnable task;

    public InvokeLaterCallable(Runnable task) {
        Assert.notNull(task, "task cannot be null");
        this.task = task;
    }

    @Override
    public V call() throws Exception {
        EventQueue.invokeLater(task);
        return null;
    }

}
