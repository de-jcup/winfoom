/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.conn.socket.*;
import org.apache.http.protocol.*;
import org.kpax.winfoom.annotation.*;
import org.kpax.winfoom.config.*;
import org.kpax.winfoom.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.net.*;

/**
 * A {@link ConnectionSocketFactory} implementation for SOCKS proxy.
 */
@ThreadSafe
@Component
class SocksConnectionSocketFactory implements ConnectionSocketFactory {

    @Autowired
    private SystemConfig systemConfig;

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        InetSocketAddress socketAddress = (InetSocketAddress) context.getAttribute(HttpUtils.SOCKS_ADDRESS);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, socketAddress);
        return systemConfig.configureSocket(new Socket(proxy));
    }

    @Override
    public Socket connectSocket(
            final int connectTimeout,
            final Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {
        Socket currentSocket = socket != null ? socket : createSocket(context);
        if (localAddress != null) {
            currentSocket.bind(localAddress);
        }
        try {
            currentSocket.connect(remoteAddress, connectTimeout);
        } catch (SocketTimeoutException e) {
            throw new ConnectTimeoutException(e, host, remoteAddress.getAddress());
        } catch (SocketException e) {
            // Remap some exceptions to
            // single out the connection failed case.
            if (HttpUtils.isConnectionRefused(e)
                    || HttpUtils.isConnectionTimeout(e)) {
                throw new ConnectException(e.getMessage());
            }
            throw e;
        }
        return currentSocket;
    }

}