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

/*    @Test
    void toBeDeleted0 () throws MalformedURLException, URISyntaxException {
        String urlStr = "http://tss4.trafic.ro/cgi-bin/trafic.png?rid=contributors&rn=393954136731&rk=168014773-195018278-1146527015514605056&cc=default&c=24&w=1707&h=960&j=0&f=0&b=63&os=0&d=http%3A//www.contributors.ro/administratie/a-crescut-mortalitatea-popula%25c8%259biei-din-romania-dupa-declan%25c8%2599area-pandemiei-covid-19/&dn=contributors.ro&r=&p=&o=r&se=&vid=9d81b953e0909l04233f10d6c5c49657&fst=1589741143&lst=1589803068&cst=1591863852&vn=3&vl=0&ldt=-1&jsl=33&uuid=&erru=&pt=A%20crescut%20mortalitatea%20popula%u021Biei%20din%20Rom%E2nia%20dup%u0103%20declan%u0219area%20pandemiei%20COVID-19%3F%20%7C%20Contributors&prid=";
        URI url = new URI(urlStr);
    }

    @Test
    void toBeDeleted () throws MalformedURLException, URISyntaxException {
        String urlStr = "http://host?xyz=abc%u021B";
        URI url = new URI(urlStr);

    }*/
}
