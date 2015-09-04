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
package org.apache.felix.webconsole.plugins.event.internal;

import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

/**
 * Java support for propeditor.js handling.
 */
public class PropertiesEditorSupport
{

    private PropertiesEditorSupport()
    {
        // prevent instantiation
    }

    /**
     * Converts the properties from the request to a key-value hashtable.
     * 
     * @param request the request to process
     * @return the converted properties
     */
    public static final Hashtable convertProperties(HttpServletRequest request)
    {
        String keys[] = request.getParameterValues("key"); //$NON-NLS-1$
        String vals[] = request.getParameterValues("val"); //$NON-NLS-1$
        String types[] = request.getParameterValues("type"); //$NON-NLS-1$

        final Hashtable properties = new Hashtable();
        synchronized (properties)
        {
            for (int i = 0; keys != null && i < keys.length; i++)
            {
                properties.put(keys[i], convert(vals[i], types[i]));
            }
        }

        return properties;
    }

    private static final Object convert(String value, String type)
    {
        if ("byte".equals(type)) //$NON-NLS-1$
        {
            return Byte.valueOf(value);
        }
        else if ("int".equals(type)) //$NON-NLS-1$
        {
            return Integer.valueOf(value);
        }
        else if ("long".equals(type)) //$NON-NLS-1$
        {
            return Long.valueOf(value);
        }
        else if ("float".equals(type)) //$NON-NLS-1$
        {
            return Float.valueOf(value);
        }
        else if ("double".equals(type)) //$NON-NLS-1$
        {
            return Double.valueOf(value);
        }
        else if ("string".equals(type)) //$NON-NLS-1$
        {
            return value.toString();
        }
        else if ("char".equals(type)) //$NON-NLS-1$
        {
            return new Character(value.toString().charAt(0));
        }
        else if ("byte array".equals(type)) //$NON-NLS-1$
        {
            return decodeHex(value.toString());
        }
        else
        {
            throw new IllegalArgumentException("Unsupported type!");
        }
    }
    
    private static final byte[] decodeHex(String data)
    {
        final StringTokenizer tok = new StringTokenizer(data, "[]{},;: \t"); //$NON-NLS-1$
        final byte[] bs = new byte[tok.countTokens()];
        int i = 0;
        while (tok.hasMoreTokens())
        {
            final String next = tok.nextToken();
            bs[i++] = Integer.decode(next).byteValue();
        }
        return bs;
    }

}
