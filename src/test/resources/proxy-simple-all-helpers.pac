function FindProxyForURL(url, host) {

    if (shExpMatch(host, "*.local")) {
        // do nothing
    }

    if (isInNet(dnsResolve(host), "10.0.0.0", "255.0.0.0")) {
        // do nothing
    }

    if (isResolvable (host) ) {
          // do nothing
    }

    if (isPlainHostName (host) ) {
          // do nothing
    }

    if (shExpMatch(host, "(*.localdomain.com)")) {
        // do nothing
    }

    if (dnsDomainIs(host, "localdomain.com")) {
        // do nothing
    }

    if (localHostOrDomainIs(host, "localdomain.com")) {
        // do nothing
    }

    if (weekdayRange("MON", "FRI", "GMT")) {

    }

    if (dateRange(1, "JUN", 1995, 15, "AUG", 1995)) {

    }

    if (timeRange(0, 0, 0, 0, 0, 30)) {

    }

    var dnsDomainLvl = dnsDomainLevels("www");
    if (!( typeof dnsDomainLvl === 'number')) {
        throw new Error ("dnsDomainLevels: wrong result");
    }

    var myIp = myIpAddress();
    alert(myIp);

    var googleIp = dnsResolve("google.com");
    alert(googleIp);

    // Microsoft extensions

    var myIpx = myIpAddressEx();
    alert(myIpx);

    if (isResolvableEx (host) ) {
              // do nothing
    }

    var googleIpEx = dnsResolveEx("google.com");
    alert(googleIpEx);

    if (isInNetEx(dnsResolveEx(host), "255.0.0.0")) {
        // do nothing
    }

    var beforeSorting = '';
    alert('beforeSorting ' + beforeSorting);

    var afterSorting = sortIpAddressList('10.2.3.9;2001:4898:28:3:201:2ff:feea:fc14;::1;127.0.0.1;::9');
    alert('afterSorting ' + afterSorting);

    return 'DIRECT';
}