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

package org.apache.felix.webconsole.plugins.scriptconsole.internal;

import org.osgi.service.log.LogService;

import java.io.Writer;

/**
 * The <code>LogWriter</code> is a simple <code>Writer</code> which writes
 * lines of data to a given Logger. Data is gathered in an internal buffer
 * until the {@link #flush()} method is called or until a CR or LF character is
 * encountered in the data to be written.
 */
class LogWriter extends Writer
{

    /** The logger to which the error messages are written */
    private Logger logger;

    /**
     * The internal buffer to gather message data until being flushed or a CR or
     * LF is encountered in the message data.
     */
    private final StringBuilder lineBuffer = new StringBuilder();

    /**
     * Creates a writer based on the given logger.
     */
    public LogWriter(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * Writes the character to the internal buffer unless the character is a CR
     * or LF in which case the buffer is written to the logger as an error
     * message.
     */
    @Override
    public void write(int c)
    {
        if (c == '\n' || c == '\r')
        {
            flush();
        }
        else
        {
            synchronized (lineBuffer)
            {
                lineBuffer.append((char) c);
            }
        }
    }

    /**
     * Writes the indicated characters to the internal buffer, flushing the
     * buffer on any occurrence of a CR of LF.
     */
    @Override
    public void write(char[] cbuf, int off, int len)
    {
        int i = off;
        for (int n = 0; n < len; n++, i++)
        {
            char c = cbuf[i];

            // if CR/LF flush the line
            if (c == '\n' || c == '\r')
            {

                // append upto the CR/LF
                int subLen = i - off;
                if (subLen > 0)
                {
                    synchronized (lineBuffer)
                    {
                        lineBuffer.append(cbuf, off, subLen);
                    }
                }

                // and flush
                flush();

                // new offset is after the CR/LF
                off = i + 1;
            }
        }

        // remaining data in the buffer is just appended
        if (off < i)
        {
            synchronized (lineBuffer)
            {
                lineBuffer.append(cbuf, off, i - off);
            }
        }
    }

    /**
     * Writes any data conained in the buffer to the logger as an error message.
     */
    @Override
    public void flush()
    {

        String message;
        synchronized (lineBuffer)
        {
            if (lineBuffer.length() == 0)
            {
                return;
            }
            message = lineBuffer.toString();
            lineBuffer.setLength(0);
        }

        logger.log(LogService.LOG_ERROR,message);
    }

    /**
     * Just calls {@link #flush()}
     */
    @Override
    public void close()
    {
        flush();
    }

}
