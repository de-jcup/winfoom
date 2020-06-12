package org.kpax.winfoom.util;

import org.apache.commons.text.StringEscapeUtils;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.stream.IntStream;

public class CacheTests {

    @Test
    void perft_Cache2k() {
        Cache<String, Integer> cache = Cache2kBuilder.of(new Cache2kConfiguration<String, Integer>())
                .name("bla")
                .eternal(true)
                .entryCapacity(200)
                .build();
        IntStream.range(0, 100).forEach(i -> cache.put("key" + i, i));
        long start = System.nanoTime();
        int key50 = cache.get("key50");
        System.out.println("Duration: " + (System.nanoTime() - start));
        Assert.assertEquals(50, key50);
    }

}
