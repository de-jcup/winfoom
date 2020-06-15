/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.util;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.configuration.Cache2kConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

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
