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

package org.kpax.winfoom.proxy;

import org.apache.http.HttpHost;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kpax.winfoom.FoomApplicationTest;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.util.ReflectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = FoomApplicationTest.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProxyBlacklistTests {

    private static final int BLACKLIST_TIMEOUT = 1;

    @MockBean
    private ProxyConfig proxyConfig;

    @Autowired
    private ProxyBlacklist proxyBlacklist;

    @BeforeEach
    void before() {
        when(proxyConfig.getBlacklistTimeout()).thenReturn(BLACKLIST_TIMEOUT);
    }

    @BeforeAll
    void beforeAll() throws Exception {
        ReflectUtils.setFinalStatic(proxyBlacklist, "TEMPORAL_UNIT", ChronoUnit.SECONDS);
        proxyBlacklist.clear();
    }

    @Order(0)
    @Test
    void changeTemporalUnit_ToSeconds_True() {
        ChronoUnit temporalUnit = (ChronoUnit) ReflectionTestUtils.getField(proxyBlacklist, "TEMPORAL_UNIT");
        Assertions.assertSame(temporalUnit, ChronoUnit.SECONDS);
    }

    @Order(1)
    @Test
    void clear_Empty_Zero() {
        assertEquals(0, proxyBlacklist.clear());
    }

    @Order(2)
    @Test
    void clear_OneBlacklisted_One() {
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.DIRECT));
        assertEquals(1, proxyBlacklist.clear());
    }

    @Order(3)
    @Test
    void checkBlacklisted_OneBlacklisted_True() {
        proxyBlacklist.clear();
        ProxyInfo proxyInfo = new ProxyInfo(ProxyInfo.PacType.DIRECT);
        proxyBlacklist.blacklist(proxyInfo);
        Assertions.assertTrue(proxyBlacklist.isBlacklisted(proxyInfo));
    }

    @Order(4)
    @Test
    void checkBlacklisted_OneBlacklistedExpired_False() throws InterruptedException {
        proxyBlacklist.clear();
        ProxyInfo proxyInfo = new ProxyInfo(ProxyInfo.PacType.DIRECT);
        proxyBlacklist.blacklist(proxyInfo);
        Thread.sleep(BLACKLIST_TIMEOUT * 1000 + 1);
        assertFalse(proxyBlacklist.isBlacklisted(proxyInfo));
    }

    @Order(5)
    @Test
    void clear_OneBlacklistedExpired_Zero() throws InterruptedException {
        proxyBlacklist.clear();
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.DIRECT));
        Thread.sleep(BLACKLIST_TIMEOUT * 1000 + 1);
        assertEquals(0, proxyBlacklist.clear());
    }

    @Order(6)
    @Test
    void blacklist_TwoBlacklistedSameTypeSameHost_One() {
        proxyBlacklist.clear();
        HttpHost host1 = new HttpHost("host", 1234);
        HttpHost host2 = new HttpHost("host", 1234);
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.HTTP, host1));
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.HTTP, host2));
        assertEquals(1, proxyBlacklist.clear());
    }


    @Order(7)
    @Test
    void blacklist_TwoBlacklistedSameTypeDifferentHosts_Two() {
        proxyBlacklist.clear();
        HttpHost host1 = new HttpHost("host1", 1234);
        HttpHost host2 = new HttpHost("host2", 1234);
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.HTTP, host1));
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.HTTP, host2));
        assertEquals(2, proxyBlacklist.clear());
    }

    @Order(8)
    @Test
    void blacklist_TwoBlacklistedDifferentTypeSameHost_Two() {
        proxyBlacklist.clear();
        HttpHost host1 = new HttpHost("host1", 1234);
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.HTTP, host1));
        proxyBlacklist.blacklist(new ProxyInfo(ProxyInfo.PacType.SOCKS, host1));
        assertEquals(2, proxyBlacklist.clear());
    }

    @Order(9)
    @Test
    void checkBlacklisted_OneBlacklistedExpired_RemoveExpiredAndIsConsistent() throws InterruptedException {
        proxyBlacklist.clear();
        ProxyInfo proxyInfo = new ProxyInfo(ProxyInfo.PacType.DIRECT);
        proxyBlacklist.blacklist(proxyInfo);
        Thread.sleep(BLACKLIST_TIMEOUT * 1000 + 1);
        proxyBlacklist.isBlacklisted(proxyInfo);
        assertEquals(0, proxyBlacklist.getBlacklistMap().size());
        assertFalse(proxyBlacklist.isBlacklisted(proxyInfo));
    }

}
