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
package org.apache.felix.webconsole.json;

import java.io.IOException;
import java.io.Writer;

/**
 * Simple JSON writer to be used on top of a {@link Writer}.
 */
public class JSONWriter
{

    private final Writer pw;

    private boolean comma;

    public JSONWriter(final Writer pw)
    {
        this.comma = false;
        this.pw = pw;
    }

    public JSONWriter object() throws IOException
    {
        if (this.comma)  this.pw.write(',');
        this.pw.write("{");
        this.comma = false;
        return this;
    }

    public JSONWriter endObject() throws IOException
    {
        this.pw.write('}');
        this.comma = true;
        return this;
    }

    public JSONWriter array() throws IOException
    {
        if (this.comma)  this.pw.write(',');
        this.pw.write("[");
        this.comma = false;
        return this;
    }

    public JSONWriter endArray() throws IOException
    {
        this.pw.write(']');
        this.comma = true;
        return this;
    }

    public JSONWriter key(String key) throws IOException
    {
        if (this.comma)  this.pw.write(',');
        quote(key);
        this.pw.write(':');
        this.comma = false;
        return this;
    }

    public JSONWriter value(final boolean b) throws IOException
    {
        if (this.comma)  this.pw.write(',');
        this.pw.write(b ? "true" : "false");
        this.comma = true;
        return this;
    }

    public JSONWriter value(final double d) throws IOException
    {
        return this.value(new Double(d));
    }

    public JSONWriter value(final int i) throws IOException
    {
        if (this.comma)  this.pw.write(',');
        this.pw.write(String.valueOf(i));
        this.comma = true;
        return this;
    }

    public JSONWriter value(final long l) throws IOException
    {
        if (this.comma)  this.pw.write(',');
        this.pw.write(String.valueOf(l));
        this.comma = true;
        return this;
    }

    public JSONWriter value(final Object value) throws IOException
    {
        if (this.comma)
        {
            this.pw.write(',');
        }
        if (value == null || value.equals(null))
        {
            this.pw.write("null");
        }
        else if (value instanceof Boolean )
        {
            this.pw.write(value.toString());
        }
        else if (value instanceof Number)
        {
            String str = value.toString();
            if ( str.indexOf('.') == -1 || str.indexOf('e') > 0 || str.indexOf('E') > 0 )
            {
                this.pw.write(str);
            } else {
                while (str.endsWith("0"))
                {
                    str = str.substring(0, str.length() - 1);
                }
                if (str.endsWith("."))
                {
                    str = str.substring(0, str.length() - 1);
                }
                this.pw.write(str);
            }
        }
        else
        {
            quote(value.toString());
        }
        this.comma = true;
        return this;
    }

    /**
     * Quote the provided value and escape some characters.
     * @param value The value to quote
     * @throws IOException
     */
    private void quote(final String value) throws IOException
    {
        pw.write('"');
        final int len = value.length();
        for(int i=0;i<len;i++)
        {
            final char c = value.charAt(i);
            switch(c){
            case '"':
                pw.write("\\\"");
                break;
            case '\\':
                pw.write("\\\\");
                break;
            case '\b':
                pw.write("\\b");
                break;
            case '\f':
                pw.write("\\f");
                break;
            case '\n':
                pw.write("\\n");
                break;
            case '\r':
                pw.write("\\r");
                break;
            case '\t':
                pw.write("\\t");
                break;
            case '/':
                pw.write("\\/");
                break;
            default:
                if ((c>='\u0000' && c<='\u001F') || (c>='\u007F' && c<='\u009F') || (c>='\u2000' && c<='\u20FF'))
                {
                    final String hex=Integer.toHexString(c);
                    pw.write("\\u");
                    for(int k=0;k<4-hex.length();k++){
                        pw.write('0');
                    }
                    pw.write(hex.toUpperCase());
                }
                else{
                    pw.write(c);
                }
            }
        }
        pw.write('"');
    }

    /**
     * @see Writer#flush()
     * @throws IOException
     */
    public void flush() throws IOException
    {
        this.pw.flush();
    }
}
