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
