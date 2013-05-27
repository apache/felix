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
import java.util.StringTokenizer;


/**
 * The JSON configuration writer
 */
public class JSONConfigurationWriter extends ConfigurationWriter
{

    private boolean wrapJSON;

    private boolean startLine;

    private boolean needComma;

    public JSONConfigurationWriter(final Writer delegatee)
    {
        super(delegatee);
        this.wrapJSON = false;
    }

    public void startJSONWrapper()
    {
//        println("{");
//        println("  \"value\": [");
        println("[");

        this.wrapJSON = true;
        this.startLine = true;
        this.needComma = false;
    }

    public void endJSONWrapper()
    {
        if (this.wrapJSON)
        {
            // properly terminate the current line
            this.println();

            this.wrapJSON = false;
            this.startLine = false;

//            super.println();
//            super.println("  ]");
//            super.println("}");
            super.println("]");
        }
    }

    // IE has an issue with white-space:pre in our case so, we write
    // <br/> instead of [CR]LF to get the line break. This also works
    // in other browsers.
    public void println()
    {
        if (wrapJSON)
        {
            if (!this.startLine)
            {
                super.write('"');
                this.startLine = true;
                this.needComma = true;
            }
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
        if (this.wrapJSON)
        {
            if (this.startLine)
            {
                this.startLine();
                this.startLine = false;
            }

            String v = new String(chars, off, len);
            StringTokenizer st = new StringTokenizer(v, "\r\n\"", true);
            while (st.hasMoreTokens())
            {
                String t = st.nextToken();
                if (t.length() == 1)
                {
                    char c = t.charAt(0);
                    if (c == '\r')
                    {
                        // ignore
                    }
                    else if (c == '\n')
                    {
                        this.println();
                        this.startLine();
                    }
                    else if (c == '"')
                    {
                        super.write('\\');
                        super.write(c);
                    }
                    else
                    {
                        super.write(c);
                    }
                }
                else
                {
                    super.write(t.toCharArray(), 0, t.length());
                }
            }
        }
        else
        {
            super.write(chars, off, len);
        }
    }

    // write the string unmodified unless filtering is enabled in
    // which case the writeFiltered(String) method is called for filtering
    public void write(final String string, final int off, final int len)
    {
        write(string.toCharArray(), off, len);
    }

    private void startLine()
    {
        if (this.needComma)
        {
            super.write(',');
            super.println();
            this.needComma = false;
        }

        super.write("    \"".toCharArray(), 0, 5);
        this.startLine = false;
    }
}