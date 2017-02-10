/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreams {
    private InputStreams() {} // prevent instantiation

    /**
     * Read an entire input stream into a byte array.
     * @param is The input stream to read.
     * @return The byte array with the contents of the input stream.
     * @throws IOException if the underlying read operation on the input stream
     * throws an error.
     */
    public static byte [] readStream(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[65536];

            int length = 0;
            int offset = 0;

            while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
                offset += length;

                if (offset == bytes.length) {
                    baos.write(bytes, 0, bytes.length);
                    offset = 0;
                }
            }
            if (offset != 0) {
                baos.write(bytes, 0, offset);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }
}
