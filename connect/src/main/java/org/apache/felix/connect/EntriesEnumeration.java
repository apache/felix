/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;

class EntriesEnumeration implements Enumeration<String>
{
    private final Enumeration<? extends ZipEntry> m_enumeration;
    private final String m_prefix;
    private volatile String current;

    public EntriesEnumeration(Enumeration<? extends ZipEntry> enumeration)
    {
        this(enumeration, null);
    }

    public EntriesEnumeration(Enumeration<? extends ZipEntry> enumeration, String prefix)
    {
        m_enumeration = enumeration;
        m_prefix = prefix;
    }

    public boolean hasMoreElements()
    {
        while ((current == null) && m_enumeration.hasMoreElements())
        {
            String result = m_enumeration.nextElement().getName();
            if (m_prefix != null)
            {
                if (result.startsWith(m_prefix))
                {
                    current = result.substring(m_prefix.length());
                }
            }
            else
            {
                current = result;
            }
        }
        return (current != null);
    }

    public String nextElement()
    {
        try
        {
            if (hasMoreElements())
            {
                return current;
            }
            else
            {
                throw new NoSuchElementException();
            }
        }
        finally
        {
            current = null;
        }
    }
}