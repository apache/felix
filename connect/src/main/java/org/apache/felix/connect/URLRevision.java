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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.lang.ref.WeakReference;

class URLRevision implements Revision
{
    private final URL m_url;
    private final long m_lastModified;
    private WeakReference<byte[]> m_urlContent;

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
        InputStream content = null;
        JarInputStream jarInput = null;
        
        try
        {
            content = getUrlContent();
            jarInput = new JarInputStream(content);
            List<String> entries = new ArrayList<String>();
            JarEntry jarEntry;

            while ((jarEntry = jarInput.getNextJarEntry()) != null)
            {
                entries.add(jarEntry.getName());
            }
            return Collections.enumeration(entries);
        }

        catch (IOException e)
        {
            e.printStackTrace();
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        finally
        {
            close(content);
            close(jarInput);
        }
    }
    
    @Override
    public URL getEntry(String entryName)
    {
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
    
    /**
     * Loads the URL content, and cache it using a weak reference.
     * 
     * @return the URL content
     * @throws IOException on any io errors
     */
    private synchronized InputStream getUrlContent() throws IOException {
        BufferedInputStream in = null;
        ByteArrayOutputStream out = null;
        byte[] content = null;

        try
        {
            if (m_urlContent == null || (content = m_urlContent.get()) == null)
            {
                out = new ByteArrayOutputStream(4096);
                in = new BufferedInputStream(m_url.openStream(), 4096);
                int c;
                while ((c = in.read()) != -1)
                {
                    out.write(c);
                }
                content = out.toByteArray();
                m_urlContent = new WeakReference<byte[]>(content);
            }

            return new ByteArrayInputStream(content);
        }

        finally
        {
            close(out);
            close(in);
        }
    }     
    
    /**
     * Helper method used to simply close a stream.
     * 
     * @param closeable the stream to close
     */
    private void close(Closeable closeable) 
    {
        try
        {
            if (closeable != null) 
            {
                closeable.close();
            }
        }
        catch (IOException e) 
        {               
        }
    }
}
