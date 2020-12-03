package org.kpax.winfoom.util;

import org.apache.commons.beanutils.*;
import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;

public class BeanUtils {

    private static final Logger logger = LoggerFactory.getLogger(BeanUtils.class);

    public static void copyNonNullProperties(Object src, Object dest)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Object> objectMap = PropertyUtils.describe(src);
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            if (!entry.getKey().equals("class") && entry.getValue() != null) {
                PropertyUtils.setProperty(dest, entry.getKey(), entry.getValue());
            }
        }
    }

}
