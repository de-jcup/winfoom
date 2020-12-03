package org.kpax.winfoom.config;

import org.apache.commons.lang3.*;

import java.beans.*;
import java.util.*;

public class Base64DecoderPropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String source) {
        if (StringUtils.isNotEmpty(source)) {
            if (source.startsWith("encoded(")) {
                String encoded = source.substring("encoded(".length(), source.length() - 1);
                setValue(new String(Base64.getDecoder().decode(encoded)));
                return;
            }
        }
        setValue(source);
    }

}