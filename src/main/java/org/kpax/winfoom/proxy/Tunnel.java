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
import org.kpax.winfoom.annotation.*;
import org.kpax.winfoom.util.*;
import org.springframework.util.*;

import java.io.*;

/**
 * Encapsulate the response of a {@link TunnelConnection}.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 3/31/2020
 */
@ThreadSafe
public final class Tunnel implements StreamSource, Closeable {

    private final ManagedHttpClientConnection connection;
    private final HttpResponse response;

    private final InputStream inputStream;
    private final OutputStream outputStream;

    Tunnel(final ManagedHttpClientConnection connection, final HttpResponse response) throws IOException {
        Assert.notNull(connection, "connection cannot be null");
        Assert.notNull(response, "response cannot be null");
        this.connection = connection;
        this.response = response;
        this.inputStream = connection.getSocket().getInputStream();
        this.outputStream = connection.getSocket().getOutputStream();
    }

    ManagedHttpClientConnection getConnection() {
        return connection;
    }

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public StatusLine getStatusLine() {
        return response.getStatusLine();
    }

    @Override
    public void close() {
        InputOutputs.close(connection);
    }
}
