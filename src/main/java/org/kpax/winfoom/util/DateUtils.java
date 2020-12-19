package org.kpax.winfoom.util;

import java.util.*;

public class DateUtils {

    private DateUtils() {
    }

    public static long secondBetween(Date date1, Date date2) {
        return (date2.getTime() - date1.getTime()) / 1000;
    }
}
