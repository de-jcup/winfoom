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

package org.kpax.winfoom.config;

import java.util.*;

public class SystemContext {

    public static final List<String> PROFILES =
            Optional.ofNullable(System.getProperty("spring.profiles.active")).
                    map(s -> Arrays.asList(s.split(","))).orElse(Collections.emptyList());

    public static final String OS_NAME = System.getProperty("os.name");

    public static final boolean IS_OS_WINDOWS = OS_NAME.startsWith("Windows");

    public static boolean isGuiMode() {
        return PROFILES.contains("gui");
    }

}
