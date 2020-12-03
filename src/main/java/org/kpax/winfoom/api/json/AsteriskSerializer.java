package org.kpax.winfoom.api.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.std.*;

import java.io.*;
import java.util.*;

public class AsteriskSerializer extends StdSerializer<Object> implements ContextualSerializer {

    private String asterisk;

    public AsteriskSerializer() {
        super(Object.class);
    }

    public AsteriskSerializer(String asterisk) {
        super(Object.class);
        this.asterisk = asterisk;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializerProvider, BeanProperty property) {
        Optional<Asterisk> optionalAsterisk = Optional.ofNullable(property)
                .map(prop -> prop.getAnnotation(Asterisk.class));
        return new AsteriskSerializer(optionalAsterisk.map(Asterisk::value).orElse(null));
    }

    @Override
    public void serialize(Object obj, JsonGenerator gen, SerializerProvider prov) throws IOException {
        gen.writeString(asterisk);
    }
}