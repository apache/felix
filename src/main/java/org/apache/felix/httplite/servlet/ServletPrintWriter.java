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

import java.io.CharConversionException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * ServletPrintWriter encodes characters with supplied encoding specification.
 * 
 */
public class ServletPrintWriter extends PrintWriter
{
    private final Charset charset;

    /**
     * @param outputStream OutputStream
     * @param characterEncoding character encoding
     * @throws UnknownCharacterException if the character encoding is not supported.
     */
    public ServletPrintWriter(OutputStream outputStream, String characterEncoding) throws CharConversionException
    {
        super(outputStream);
        if (!Charset.isSupported(characterEncoding))
        {
            throw new CharConversionException("Unsupported character encoding: "
                + characterEncoding);
        }

        this.charset = Charset.forName(characterEncoding);
    }

    /* (non-Javadoc)
     * @see java.io.PrintWriter#print(java.lang.String)
     */
    public void print(String s)
    {
        super.print(charset.encode(s).toString());
    }
}
