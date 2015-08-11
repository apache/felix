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
package org.apache.felix.bundleplugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Constants;

public class ManifestWriter {

    /**
     * Unfortunately we have to write our own manifest :-( because of a stupid
     * bug in the manifest code. It tries to handle UTF-8 but the way it does it
     * it makes the bytes platform dependent. So the following code outputs the
     * manifest. A Manifest consists of
     *
     * <pre>
     *   'Manifest-Version: 1.0\r\n'
     *   main-attributes *
     *   \r\n
     *   name-section
     *
     *   main-attributes ::= attributes
     *   attributes      ::= key ': ' value '\r\n'
     *   name-section    ::= 'Name: ' name '\r\n' attributes
     * </pre>
     *
     * Lines in the manifest should not exceed 72 bytes (! this is where the
     * manifest screwed up as well when 16 bit unicodes were used).
     * <p>
     * As a bonus, we can now sort the manifest!
     */
    static byte[]	CONTINUE	= new byte[] {
            '\r', '\n', ' '
    };

    static Set<String> NICE_HEADERS = new HashSet<String>(
            Arrays.asList(
                    Constants.IMPORT_PACKAGE,
                    Constants.DYNAMICIMPORT_PACKAGE,
                    Constants.IMPORT_SERVICE,
                    Constants.REQUIRE_CAPABILITY,
                    Constants.EXPORT_PACKAGE,
                    Constants.EXPORT_SERVICE,
                    Constants.PROVIDE_CAPABILITY
            )
    );

    /**
     * Main function to output a manifest properly in UTF-8.
     *
     * @param manifest
     *            The manifest to output
     * @param out
     *            The output stream
     * @throws IOException
     *             when something fails
     */
    public static void outputManifest(Manifest manifest, OutputStream out, boolean nice) throws IOException {
        writeEntry(out, "Manifest-Version", "1.0", nice);
        attributes(manifest.getMainAttributes(), out, nice);

        TreeSet<String> keys = new TreeSet<String>();
        for (Object o : manifest.getEntries().keySet())
            keys.add(o.toString());

        for (String key : keys) {
            write(out, 0, "\r\n");
            writeEntry(out, "Name", key, nice);
            attributes(manifest.getAttributes(key), out, nice);
        }
        out.flush();
    }

    /**
     * Write out an entry, handling proper unicode and line length constraints
     */
    private static void writeEntry(OutputStream out, String name, String value, boolean nice) throws IOException {
        if (nice && NICE_HEADERS.contains(name)) {
            int n = write(out, 0, name + ": ");
            String[] parts = Parser.parseDelimitedString(value, ",");
            if (parts.length > 1) {
                write(out, 0, "\r\n ");
                n = 1;
            }
            for (int i = 0; i < parts.length; i++) {
                if (i < parts.length - 1) {
                    write(out, n, parts[i] + ",");
                    write(out, 0, "\r\n ");
                } else {
                    write(out, n, parts[i]);
                    write(out, 0, "\r\n");
                }
                n = 1;
            }
        } else {
            int n = write(out, 0, name + ": ");
            write(out, n, value);
            write(out, 0, "\r\n");
        }
    }

    /**
     * Convert a string to bytes with UTF8 and then output in max 72 bytes
     *
     * @param out
     *            the output string
     * @param i
     *            the current width
     * @param s
     *            the string to output
     * @return the new width
     * @throws IOException
     *             when something fails
     */
    private static int write(OutputStream out, int i, String s) throws IOException {
        byte[] bytes = s.getBytes("UTF8");
        return write(out, i, bytes);
    }

    /**
     * Write the bytes but ensure that the line length does not exceed 72
     * characters. If it is more than 70 characters, we just put a cr/lf +
     * space.
     *
     * @param out
     *            The output stream
     * @param width
     *            The nr of characters output in a line before this method
     *            started
     * @param bytes
     *            the bytes to output
     * @return the nr of characters in the last line
     * @throws IOException
     *             if something fails
     */
    private static int write(OutputStream out, int width, byte[] bytes) throws IOException {
        int w = width;
        for (int i = 0; i < bytes.length; i++) {
            if (w >= 72) { // we need to add the \n\r!
                out.write(CONTINUE);
                w = 1;
            }
            out.write(bytes[i]);
            w++;
        }
        return w;
    }

    /**
     * Output an Attributes map. We will sort this map before outputing.
     *
     * @param value
     *            the attrbutes
     * @param out
     *            the output stream
     * @throws IOException
     *             when something fails
     */
    private static void attributes(Attributes value, OutputStream out, boolean nice) throws IOException {
        TreeMap<String,String> map = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<Object,Object> entry : value.entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }

        map.remove("Manifest-Version"); // get rid of
        // manifest
        // version
        for (Map.Entry<String,String> entry : map.entrySet()) {
            writeEntry(out, entry.getKey(), entry.getValue(), nice);
        }
    }
}
