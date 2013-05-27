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
package org.apache.felix.inventory.impl.helper;

import java.io.Writer;


/**
 * The HTML configuration writer outputs the status as an HTML snippet.
 */
public class HtmlConfigurationWriter extends ConfigurationWriter
{

    // whether or not to filter "<" signs in the output
    private boolean doFilter;

    public HtmlConfigurationWriter(final Writer delegatee)
    {
        super(delegatee);
    }

    public void enableFilter(final boolean doFilter)
    {
        this.doFilter = doFilter;
        if (doFilter) {
            // start filtering
            super.write("<pre>", 0, 5);
        } else {
            // end filtering
            super.write("</pre>", 0, 6);
        }
        super.println();
    }

    // IE has an issue with white-space:pre in our case so, we write
    // <br/> instead of [CR]LF to get the line break. This also works
    // in other browsers.
    public void println()
    {
        if (doFilter)
        {
            this.write('\n'); // write <br/>
        }
        else
        {
            super.println();
        }
    }

    // some VM implementation directly write in underlying stream, instead
    // of
    // delegation to the write() method. So we need to override this, to
    // make
    // sure, that everything is escaped correctly
    public void print(final String str)
    {
        final char[] chars = str.toCharArray();
        write(chars, 0, chars.length);
    }

    private final char[] oneChar = new char[1];

    // always delegate to write(char[], int, int) otherwise in some VM
    // it cause endless cycle and StackOverflowError
    public void write(final int character)
    {
        synchronized (oneChar)
        {
            oneChar[0] = (char) character;
            write(oneChar, 0, 1);
        }
    }

    // write the characters unmodified unless filtering is enabled in
    // which case the writeFiltered(String) method is called for filtering
    public void write(char[] chars, int off, int len)
    {
        if (doFilter)
        {
            chars = this.escapeHtml(new String(chars, off, len)).toCharArray();
            off = 0;
            len = chars.length;
        }
        super.write(chars, off, len);
    }

    // write the string unmodified unless filtering is enabled in
    // which case the writeFiltered(String) method is called for filtering
    public void write(final String string, final int off, final int len)
    {
        write(string.toCharArray(), off, len);
    }

    /**
     * Escapes HTML special chars like: <>&\r\n and space
     *
     *
     * @param text the text to escape
     * @return the escaped text
     */
    private String escapeHtml(final String text)
    {
        final StringBuffer sb = new StringBuffer(text.length() * 4 / 3);
        char ch, oldch = '_';
        for (int i = 0; i < text.length(); i++)
        {
            switch (ch = text.charAt(i))
            {
                case '<':
                    sb.append("&lt;"); //$NON-NLS-1$
                    break;
                case '>':
                    sb.append("&gt;"); //$NON-NLS-1$
                    break;
                case '&':
                    sb.append("&amp;"); //$NON-NLS-1$
                    break;
                case ' ':
                    sb.append("&nbsp;"); //$NON-NLS-1$
                    break;
                case '\r':
                case '\n':
                    if (oldch != '\r' && oldch != '\n')
                        sb.append("\n"); //$NON-NLS-1$
                    break;
                default:
                    sb.append(ch);
            }
            oldch = ch;
        }

        return sb.toString();
    }
}