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

package org.apache.felix.http.base.internal.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Some convenience methods for handling URI(-parts).
 */
public abstract class UriUtils
{
    private static final String SLASH_STR = "/";
    private static final char DOT = '.';
    private static final char SLASH = '/';

    /**
     * Concatenates two paths keeping their respective path-parts into consideration.
     *
     * @param path1 the first part of the path, can be <code>null</code>;
     * @param path2 the second part of the path, can be <code>null</code>.
     * @return the concatenated path, can be <code>null</code> in case both given arguments were <code>null</code>.
     */
    public static String concat(String path1, String path2)
    {
        // Handle special cases...
        if (path1 == null && path2 == null)
        {
            return null;
        }
        if (path1 == null)
        {
            path1 = "";
        }
        if (path2 == null)
        {
            path2 = "";
        }
        if (isEmpty(path1) && isEmpty(path2))
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int idx = path1.indexOf('?');
        if (idx == 0)
        {
            // path1 only consists of a query, append it to the second path...
            return path2.concat(path1);
        }
        else if (idx > 0)
        {
            // path1 contains of a path + query, append the path first...
            sb.append(path1.substring(0, idx));
        }
        else
        {
            // Plain paths...
            sb.append(path1);
            // need a slash?
        }

        if (endsWith(sb, SLASH_STR))
        {
            if (path2.startsWith(SLASH_STR))
            {
                sb.append(path2.substring(1));
            }
            else
            {
                sb.append(path2);
            }
        }
        else
        {
            if (path2.startsWith(SLASH_STR))
            {
                sb.append(path2);
            }
            else if (sb.length() > 0 && !isEmpty(path2))
            {
                sb.append(SLASH_STR).append(path2);
            }
            else
            {
                sb.append(path2);
            }
        }

        if (idx > 0)
        {
            // Add the query of path1...
            sb.append(path1.substring(idx, path1.length()));
        }

        return sb.toString();
    }

    /**
     * Decodes a given URL-encoded path assuming it is UTF-8 encoded.
     *
     * @param path the URL-encoded path, can be <code>null</code>.
     * @return the decoded path, can be <code>null</code> only if the given path was <code>null</code>.
     */
    public static String decodePath(String path)
    {
        return decodePath(path, "UTF-8");
    }

    /**
     * Decodes a given URL-encoded path using a given character encoding.
     *
     * @param path the URL-encoded path, can be <code>null</code>;
     * @param encoding the character encoding to use, cannot be <code>null</code>.
     * @return the decoded path, can be <code>null</code> only if the given path was <code>null</code>.
     */
    private static String decodePath(String path, String encoding)
    {
        // Special cases...
        if (path == null)
        {
            return null;
        }

        CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        int len = path.length();
        ByteBuffer buf = ByteBuffer.allocate(len);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i++)
        {
            char ch = path.charAt(i);
            if (ch == '%' && (i + 2 < len))
            {
                // URL-encoded char...
                buf.put((byte) ((16 * hexVal(path, ++i)) + hexVal(path, ++i)));
            }
            else
            {
                if (buf.position() > 0)
                {
                    // flush encoded chars first...
                    sb.append(decode(buf, decoder));
                    buf.clear();
                }

                sb.append(ch);
            }
        }

        // flush trailing encoded characters...
        if (buf.position() > 0)
        {
            sb.append(decode(buf, decoder));
            buf.clear();
        }

        return sb.toString();
    }

    /**
     * Removes all superfluous dot-segments using the algorithm described in RFC-3986 section 5.2.4.
     *
     * @param path the path to remove all dot-segments from, can be <code>null</code>.
     * @return the cleaned path, can be <code>null</code> only if the given path was <code>null</code>.
     */
    public static String removeDotSegments(String path)
    {
        // Handle special cases...
        if (path == null)
        {
            return null;
        }
        if (isEmpty(path))
        {
            return "";
        }

        StringBuilder scratch = new StringBuilder(path);
        StringBuilder sb = new StringBuilder();
        char l, la = 0, laa = 0, laaa = 0;

        while (scratch.length() > 0)
        {
            l = la(scratch, 0);
            la = la(scratch, 1);
            laa = la(scratch, 2);

            if (l == DOT)
            {
                if (la == 0)
                {
                    // (D) found '.' at the end of the URL
                    break;
                }
                else if (la == DOT && laa == SLASH)
                {
                    // (A) found '../', remove it from the input...
                    scratch.delete(0, 3);
                    continue;
                }
                else if (la == DOT && laa == 0)
                {
                    // (D) found '..' at the end of the URL
                    break;
                }
                else if (la == SLASH)
                {
                    // (A) found './', remove it from the input...
                    scratch.delete(0, 2);
                    continue;
                }
            }
            else if (l == SLASH && la == DOT)
            {
                if (laa == SLASH)
                {
                    // (B) found '/./', remove the leading '/.'...
                    scratch.delete(0, 2);
                    continue;
                }
                else if (laa == 0)
                {
                    // (B) found '/.' as last part of the URL
                    sb.append(SLASH);
                    // we're done...
                    break;
                }
                else if (laa == DOT)
                {
                    laaa = la(scratch, 3);
                    if (laaa == SLASH)
                    {
                        // (C) found '/../', remove the '/..' part from the input...
                        scratch.delete(0, 3);

                        // go back one segment in the output, including the last '/'...
                        sb.setLength(lb(sb, 0));
                        continue;
                    }
                    else if (laaa == 0)
                    {
                        // (C) found '/..' as last part of the URL, go back one segment in the output, excluding the last '/'...
                        sb.setLength(lb(sb, -1));
                        // we're done...
                        break;
                    }
                }
            }

            // (E) Copy everything up to (but not including) the next '/'...
            do
            {
                sb.append(l);
                scratch.delete(0, 1);
                l = la(scratch, 0);
            }
            while (l != SLASH && l != 0);
        }

        return sb.toString();
    }

    private static char la(CharSequence sb, int idx)
    {
        if (sb.length() > idx)
        {
            return sb.charAt(idx);
        }
        return 0;
    }

    private static int lb(CharSequence sb, int offset)
    {
        int pos = sb.length() - 1 - offset;
        while (pos > 0 && sb.charAt(pos + offset) != SLASH)
        {
            pos--;
        }
        return pos;
    }

    private static String decode(ByteBuffer bb, CharsetDecoder decoder)
    {
        CharBuffer cb = CharBuffer.allocate(128);

        CoderResult result = decoder.decode((ByteBuffer) bb.flip(), cb, true /* endOfInput */);
        if (result.isError())
        {
            throw new IllegalArgumentException("Malformed UTF-8!");
        }

        return ((CharBuffer) cb.flip()).toString();
    }

    private static boolean endsWith(CharSequence seq, String part)
    {
        int len = part.length();
        if (seq.length() < len)
        {
            return false;
        }
        for (int i = 0; i < len; i++)
        {
            if (seq.charAt(seq.length() - (i + 1)) != part.charAt(i))
            {
                return false;
            }
        }
        return true;
    }

    private static int hexVal(CharSequence seq, int idx)
    {
        char ch = seq.charAt(idx);
        if (ch >= '0' && ch <= '9')
        {
            return ch - '0';
        }
        else if (ch >= 'a' && ch <= 'f')
        {
            return 10 + (ch - 'a');
        }
        else if (ch >= 'A' && ch <= 'F')
        {
            return 10 + (ch - 'A');
        }
        throw new IllegalArgumentException("Invalid hex digit: " + ch);
    }

    private static boolean isEmpty(String value)
    {
        return value == null || "".equals(value.trim());
    }

    /**
     * Creates a new {@link UriUtils} instance.
     */
    private UriUtils()
    {
        // Nop
    }
}
