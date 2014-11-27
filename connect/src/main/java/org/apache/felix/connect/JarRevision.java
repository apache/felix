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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class JarRevision implements Revision
{
    private final long m_lastModified;
    private final JarFile m_jar;
    private final URL m_url;
    private final String m_urlString;
    private final String m_prefix;

    public JarRevision(JarFile jar, URL url, String prefix, long lastModified)
    {
        m_jar = jar;
        m_url = url;
        m_urlString = m_url.toExternalForm();
        m_prefix = prefix;
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

    public Enumeration<String> getEntries()
    {
        return new EntriesEnumeration(m_jar.entries(), m_prefix);
    }

    @Override
    public URL getEntry(String entryName)
    {
        try
        {
            if ("/".equals(entryName) || "".equals(entryName) || " ".equals(entryName))
            {
                return new URL("jar:" + m_urlString + "!/" + ((m_prefix == null) ? "" : m_prefix));
            }
            if (entryName != null)
            {
                final String target = ((entryName.startsWith("/")) ? entryName.substring(1) : entryName);
                final JarEntry entry = m_jar.getJarEntry(((m_prefix == null) ? "" : m_prefix) + target);
                if (entry != null)
                {
                    URL result = new URL(null, "jar:" + m_urlString + "!/" + ((m_prefix == null) ? "" : m_prefix) + target, new URLStreamHandler()
                    {
                        protected URLConnection openConnection(final URL u) throws IOException
                        {
                            return new java.net.JarURLConnection(u)
                            {

                                public JarFile getJarFile()
                                {
                                    return m_jar;
                                }

                                public void connect() throws IOException
                                {
                                }

                                public InputStream getInputStream() throws IOException
                                {
                                    String extF = u.toExternalForm();
                                    JarEntry targetEntry = entry;
                                    if (!extF.endsWith(target))
                                    {
                                        extF = extF.substring(extF.indexOf('!') + 2);
                                        if (m_prefix != null)
                                        {
                                            if (!extF.startsWith(m_prefix))
                                            {
                                                extF = m_prefix + extF;
                                            }
                                        }
                                        targetEntry = m_jar.getJarEntry(extF);
                                    }
                                    return m_jar.getInputStream(targetEntry);
                                }
                            };
                        }
                    });
                    return result;
                }
                else
                {
                    if (entryName.endsWith("/"))
                    {
                        return new URL("jar:" + m_urlString + "!/" + ((m_prefix == null) ? "" : m_prefix) + target);
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;

    }

}
