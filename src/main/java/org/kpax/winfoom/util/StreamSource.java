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

package org.kpax.winfoom.util;

import org.kpax.winfoom.annotation.*;
import org.springframework.util.*;

import java.io.*;
import java.net.*;

@ThreadSafe
public interface StreamSource {

    @NotNull
    InputStream getInputStream();

    @NotNull
    OutputStream getOutputStream();

    static StreamSource from(@NotNull final InputStream inputStream, @NotNull final OutputStream outputStream) {
        return new DefaultStreamSource(inputStream, outputStream);
    }

    static StreamSource from(@NotNull final Socket socket) throws IOException {
        return new DefaultStreamSource(socket.getInputStream(), socket.getOutputStream());
    }

    class DefaultStreamSource implements StreamSource {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        private DefaultStreamSource(@NotNull final InputStream inputStream, @NotNull final OutputStream outputStream) {
            Assert.notNull(inputStream, "inputStream cannot be null");
            Assert.notNull(outputStream, "outputStream cannot be null");
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @NotNull
        public InputStream getInputStream() {
            return inputStream;
        }

        @NotNull
        public OutputStream getOutputStream() {
            return outputStream;
        }
    }
}
