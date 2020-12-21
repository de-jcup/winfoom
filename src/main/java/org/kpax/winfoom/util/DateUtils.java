package org.kpax.winfoom.util;

import java.util.*;

public class DateUtils {

    private DateUtils() {
    }

    public static long secondsFromCurrent(Date date) {
        return (System.currentTimeMillis() - date.getTime()) / 1000;
    }
}
