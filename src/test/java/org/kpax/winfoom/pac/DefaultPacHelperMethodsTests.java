package org.kpax.winfoom.pac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kpax.winfoom.FoomApplicationTest;
import org.kpax.winfoom.config.SystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FoomApplicationTest.class)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefaultPacHelperMethodsTests {

    @Autowired
    private DefaultPacHelperMethods defaultPacHelperMethods;

    @Autowired
    private SystemConfig systemConfig;

    @Test
    void isPlainHostName_Plain_True() {
        boolean isPlainHostName = defaultPacHelperMethods.isPlainHostName("www");
        assertTrue(isPlainHostName);
    }

    @Test
    void isPlainHostName_NotPlain_False() {
        boolean isPlainHostName = defaultPacHelperMethods.isPlainHostName("www.google.com");
        assertFalse(isPlainHostName);
    }

    @Test
    void dnsDomainIs_Matches_True() {
        boolean dnsDomainIs = defaultPacHelperMethods.dnsDomainIs("www.google.com", ".google.com");
        assertTrue(dnsDomainIs);
    }

    @Test
    void dnsDomainIs_NoMatch_False() {
        boolean dnsDomainIs = defaultPacHelperMethods.dnsDomainIs("www", ".google.com");
        assertFalse(dnsDomainIs);
    }

    @Test
    void localHostOrDomainIs_ExactMatch_True() {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("www.mozilla.org", "www.mozilla.org");
        assertTrue(localHostOrDomainIs);
    }

    @Test
    void localHostOrDomainIs_HostnameMatchDomainNotSpecified_True() {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("www", "www.mozilla.org");
        assertTrue(localHostOrDomainIs);
    }

    @Test
    void localHostOrDomainIs_DomainNameMismatch_False() {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("www.google.com", "www.mozilla.org");
        assertFalse(localHostOrDomainIs);
    }

    @Test
    void localHostOrDomainIs_HostnameMismatch_False() {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("home.mozilla.org", "www.mozilla.org");
        assertFalse(localHostOrDomainIs);
    }

    @Test
    void isResolvable_Resolvable_True() {
        boolean isResolvable = defaultPacHelperMethods.isResolvable("www.mozilla.org");
        assertTrue(isResolvable);
    }

    @Test
    void isResolvable_NotResolvable_False() {
        boolean isResolvable = defaultPacHelperMethods.isResolvable("bogus.domain.foobar");
        assertFalse(isResolvable);
    }

    @Test
    void isInNetEx_ExactMatchIPv4_True() {
        boolean inNetEx = defaultPacHelperMethods.isInNetEx("198.95.249.79", "198.95.249.79/32");
        assertTrue(inNetEx);
    }

    @Test
    void isInNetEx_MaskMatchIPv4_True() {
        boolean inNetEx = defaultPacHelperMethods.isInNetEx("198.95.249.79", "198.95.0.0/16");
        assertTrue(inNetEx);
    }

    @Test
    void isInNetEx_MaskMatchIPv6_True() throws UnknownHostException {
        String s = "2001:db8:a1d5::";
        boolean inNetEx = defaultPacHelperMethods.isInNetEx(s, s + "/52");
        assertTrue(inNetEx);
    }

    @Test
    void sortIpAddressList_IPv6AndIPv4_RightOrder() {
        String sorted = defaultPacHelperMethods.sortIpAddressList("10.2.3.9;2001:4898:28:3:201:2ff:feea:fc14;::1;127.0.0.1;::9");
        assertEquals("::1;::9;2001:4898:28:3:201:2ff:feea:fc14;10.2.3.9;127.0.0.1", sorted);
    }

    @Test
    void sortIpAddressList_IPv6AndIPv4WithSpaces_RightOrder() {
        String sorted = defaultPacHelperMethods.sortIpAddressList(" 10.2.3.9 ; 2001:4898:28:3:201:2ff:feea:fc14 ;::1; 127.0.0.1 ; ::9 ");
        assertEquals("::1;::9;2001:4898:28:3:201:2ff:feea:fc14;10.2.3.9;127.0.0.1", sorted);
    }

    @Test
    void sortIpAddressList_IPv6_RightOrder() {
        String sorted = defaultPacHelperMethods.sortIpAddressList("2001:4898:28:3:201:2ff:feea:fc14;::1;::9");
        assertEquals("::1;::9;2001:4898:28:3:201:2ff:feea:fc14", sorted);
    }

    @Test
    void sortIpAddressList_IPv4_RightOrder() {
        String sorted = defaultPacHelperMethods.sortIpAddressList("127.0.0.1;10.2.3.9");
        assertEquals("10.2.3.9;127.0.0.1", sorted);
    }

}
