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

/*
 * Modifications copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.pac.net;

import com.sun.jna.*;
import org.kpax.winfoom.exception.*;
import org.springframework.util.*;

/**
 * Hostname utilities for Microsoft Windows OS.
 *
 * @author lbruun
 * @author ecovaci
 */
public class Hostnames {

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
     * @param hostname the hostname
     * @return the domain stripped hostname
     */
    public static String stripDomain(final String hostname) {
        Assert.notNull(hostname, "hostname cannot be null");
        int dotIndex = hostname.indexOf('.');
        return dotIndex > -1 ? hostname.substring(0, dotIndex) : hostname;
    }

}
