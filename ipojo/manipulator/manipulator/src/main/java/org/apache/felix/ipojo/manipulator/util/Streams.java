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

package org.apache.felix.ipojo.manipulator.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@code Streams} is a utility class that helps to manipulate streams.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Streams {

    private static final int ONE_HUNDRED_KB = 102400;

    private static final int FOUR_KB = 4096;

    /**
     * Utility class: no public constructor
     */
    private Streams() {}

    /**
     * Close all the streams
     * @param streams Streams to be closed
     */
    public static void close(Closeable... streams) {
        for (Closeable stream : streams) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
    }

    /**
     * Transfer the given {@link InputStream} content in the given {@link OutputStream}.
     * @param is input
     * @param os output
     * @throws IOException if there is a transfer error.
     */
    public static void transfer(InputStream is, OutputStream os) throws IOException {
        int read;
        byte[] buffer = new byte[FOUR_KB];
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
    }

    /**
     * Read the content of the given InputStream as a byte array.
     * Notice that this method automatically closes the InputStream
     * @param is source
     * @return the content of the InputStream
     * @throws IOException if stream's content cannot be read/written
     */
    public static byte[] readBytes(final InputStream is) throws IOException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int read;
            byte[] data = new byte[ONE_HUNDRED_KB];

            while ((read = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
            }

            buffer.flush();

            return buffer.toByteArray();
        } finally {
            close(is);
        }

    }

}
