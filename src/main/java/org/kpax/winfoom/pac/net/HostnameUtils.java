/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kpax.winfoom.pac.net;

import com.sun.jna.Native;
import org.kpax.winfoom.exception.NativeException;

/**
 * Hostname utilities for Microsoft Windows OS.
 * @author lbruun
 */
public class HostnameUtils {

    /**
     * Gets the local host name.
     *
     * <p>
     * The underlying Windows function may return a simple host name (e.g.
     * {@code chicago}) or it may return a fully qualified host name (e.g.
     * {@code chicago.us.internal.net}).
     *
     * <p>
     * Note that the underlying Windows function will do a name service lookup
     * and the method is therefore potentially blocking, although it is more
     * than likely that Windows has cached this result in the DNS Client Cache
     * and therefore the result will be returned very fast.
     *
     * <p>
     * Windows API equivalent: {@code gethostname()} function from
     * {@code Ws2_32} library.
     *
     * @return the hostname
     * @throws NativeException if there was an error executing the
     *                         system call.
     */
    public static String getHostName() throws NativeException {
        byte[] buf = new byte[256];
        int returnCode = Winsock2Lib.INSTANCE.gethostname(buf, buf.length);
        if (returnCode == 0) {
            return Native.toString(buf);
        } else {
            throw new NativeException(returnCode, "error calling 'gethostname()' function");
        }
    }

    /**
     * Strips the domain part from a host name. Example: for {@code "foo.bar.com"}
     * then {@code "foo"} will be returned.
     *
     * <p>
     * The method is safe to use even if the input is an IPv4 literal or IPv6
     * literal. In this case the input will be returned unchanged.
     *
     * @param hostname the hostname
     * @return the domain stripped hostname
     */
    public static String removeDomain(final String hostname) {
        if (hostname == null) {
            return null;
        }
        if (IpAddressUtils.isValidIPv4Address(hostname)) {
            return hostname;
        }

        int pos = hostname.indexOf('.');
        if (pos == -1) {
            return hostname;
        } else {
            int posColon = hostname.indexOf(':');
            if (posColon >= 0 && posColon < pos) { // It is an IPv6 literal with an embedded IPv4 address
                return hostname;
            }

            return hostname.substring(0, pos);
        }
    }

}
