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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

class URLRevision implements Revision
{
    private final URL m_url;
    private final long m_lastModified;

    public URLRevision(URL url, long lastModified)
    {
        m_url = url;
        if (lastModified > 0)
        {
            m_lastModified = lastModified;
        }
        else
        {
            m_lastModified = System.currentTimeMillis();
        }
    }

    @Override
    public long getLastModified()
    {
        return m_lastModified;
    }

    @Override
    public Enumeration<String> getEntries()
    {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    @Override
    public URL getEntry(String entryName)
    {
        // TODO Auto-generated method stub
        try
        {
            return new URL(m_url, entryName);
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
