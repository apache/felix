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
package org.apache.felix.httplite.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

/**
 * Simple utility input stream class that provides a method for reading
 * a line of characters, where a "line" is leniently defined as anything
 * ending in '\n' or '\r\n'.
 * 
 * Extends ServletInputStream
**/
public class ConcreteServletInputStream extends ServletInputStream
{
    private InputStream m_is;
    private StringBuffer m_sb = new StringBuffer();

    /**
     * @param is InputStream
     */
    public ConcreteServletInputStream(final InputStream is)
    {
        m_is = is;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException
    {
        return m_is.available();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException
    {
        m_is.close();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#mark(int)
     */
    public void mark(int readlimit)
    {
        m_is.mark(readlimit);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#markSupported()
     */
    public boolean markSupported()
    {
        return m_is.markSupported();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException
    {
        return m_is.read();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[])
     */
    public int read(final byte[] b) throws IOException
    {
        return m_is.read(b);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
        return m_is.read(b, off, len);
    }

    /**
     * @return The next line in the input stream or null for EOF
     * @throws IOException on I/O error
     */
    public String readLine() throws IOException
    {
        m_sb.delete(0, m_sb.length());
        int bytesRead = 0;
        for (int i = m_is.read(); i >= 0; i = m_is.read())
        {
            bytesRead++;
            if ('\n' == (char) i)
            {
                break;
            }
            else if ('\r' != (char) i)
            {
                m_sb.append((char) i);
            }
        }
        if (bytesRead == 0)
        {
            return null;
        }

        return m_sb.toString();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#reset()
     */
    public void reset() throws IOException
    {
        m_is.reset();
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#skip(long)
     */
    public long skip(final long n) throws IOException
    {
        return m_is.skip(n);
    }
}