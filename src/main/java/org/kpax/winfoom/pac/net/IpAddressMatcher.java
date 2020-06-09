package org.kpax.winfoom.pac.net;

import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Matches a request based on IP Address or subnet mask matching against the remote address.
 * The original is <a href="https://github.com/justinedelson/spring-security/blob/master/web/src/main/java/org/springframework/security/web/util/IpAddressMatcher.java">IpAddressMatcher.java</a>
 *
 * @author Luke Taylor
 * @since 3.0.2
 */
public class IpAddressMatcher {

    private final int nMaskBits;
    private final InetAddress requiredAddress;

    /**
     * Takes a specific IP address or a range specified using the
     * IP/Netmask (e.g. 192.168.1.0/24 or 202.24.0.0/14).
     *
     * @param ipAddress the address or range of addresses from which the request must come.
     */
    public IpAddressMatcher(String ipAddress) throws UnknownHostException {

        if (ipAddress.indexOf('/') > 0) {
            String[] addressAndMask = StringUtils.split(ipAddress, "/");
            ipAddress = addressAndMask[0];
            nMaskBits = Integer.parseInt(addressAndMask[1]);
        } else {
            nMaskBits = 0;
        }
        requiredAddress = InetAddress.getByName(ipAddress);
    }

    public boolean matches(String address) throws UnknownHostException {
        InetAddress remoteAddress = InetAddress.getByName(address);

        if (!requiredAddress.getClass().equals(remoteAddress.getClass())) {
            throw new IllegalArgumentException("IP Address in expression must be the same type as " +
                    "version returned by request");
        }

        if (nMaskBits == 0) {
            return remoteAddress.equals(requiredAddress);
        }

        byte[] remAddr = remoteAddress.getAddress();
        byte[] reqAddr = requiredAddress.getAddress();

        int oddBits = nMaskBits % 8;
        int nMaskBytes = nMaskBits / 8 + (oddBits == 0 ? 0 : 1);
        byte[] mask = new byte[nMaskBytes];

        Arrays.fill(mask, 0, oddBits == 0 ? mask.length : mask.length - 1, (byte) 0xFF);

        if (oddBits != 0) {
            int finalByte = (1 << oddBits) - 1;
            finalByte <<= 8 - oddBits;
            mask[mask.length - 1] = (byte) finalByte;
        }

        for (int i = 0; i < mask.length; i++) {
            if ((remAddr[i] & mask[i]) != (reqAddr[i] & mask[i])) {
                return false;
            }
        }

        return true;
    }

}