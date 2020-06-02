package org.kpax.winfoom.pac;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultPacHelperMethodsTests {

    DefaultPacHelperMethods defaultPacHelperMethods = new DefaultPacHelperMethods();

    @Test
    void isPlainHostName_Plain_True ()   {
        boolean isPlainHostName = defaultPacHelperMethods.isPlainHostName("www");
        assertTrue(isPlainHostName);
    }

    @Test
    void isPlainHostName_NotPlain_False ()   {
        boolean isPlainHostName = defaultPacHelperMethods.isPlainHostName("www.google.com");
        assertFalse(isPlainHostName);
    }

    @Test
    void dnsDomainIs_Matches_True () {
        boolean dnsDomainIs = defaultPacHelperMethods.dnsDomainIs("www.google.com", ".google.com");
        assertTrue(dnsDomainIs);
    }
    @Test
    void dnsDomainIs_NoMatch_False () {
        boolean dnsDomainIs = defaultPacHelperMethods.dnsDomainIs("www", ".google.com");
        assertFalse(dnsDomainIs);
    }

    @Test
    void  localHostOrDomainIs_ExactMatch_True () {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("www.mozilla.org", "www.mozilla.org");
        assertTrue(localHostOrDomainIs);
    }

    @Test
    void  localHostOrDomainIs_HostnameMatchDomainNotSpecified_True () {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("www", "www.mozilla.org");
        assertTrue(localHostOrDomainIs);
    }

    @Test
    void  localHostOrDomainIs_DomainNameMismatch_False () {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("www.google.com", "www.mozilla.org");
        assertFalse(localHostOrDomainIs);
    }

    @Test
    void  localHostOrDomainIs_HostnameMismatch_False () {
        boolean localHostOrDomainIs = defaultPacHelperMethods.localHostOrDomainIs("home.mozilla.org", "www.mozilla.org");
        assertFalse(localHostOrDomainIs);
    }

    @Test
    void isResolvable_Resolvable_True () {
        boolean isResolvable = defaultPacHelperMethods.isResolvable("www.mozilla.org");
        assertTrue(isResolvable);
    }

    @Test
    void isResolvable_NotResolvable_False () {
        boolean isResolvable = defaultPacHelperMethods.isResolvable("bogus.domain.foobar");
        assertFalse(isResolvable);
    }

}
