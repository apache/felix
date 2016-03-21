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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileVisitor;
import org.jboss.vfs.VisitorAttributes;

/**
 * Loads the content of a bundle using JBoss VFS protocol.
 */
public class VFSRevision implements Revision
{
    private final URL m_url;
    private final long m_lastModified;
    private final Map<String, VirtualFile> m_entries = new HashMap<String, VirtualFile>();
    
    public VFSRevision(URL url, long lastModified)
    {
        m_url = url;
        m_lastModified = lastModified;
    }

    public long getLastModified()
    {
        return m_lastModified;
    }

    public Enumeration<String> getEntries()
    {
        try
        {
            loadEntries(); // lazily load entries
            return Collections.enumeration(m_entries.keySet());
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public URL getEntry(String entryName)
    {
        try
        {
            loadEntries();
            VirtualFile vfile = m_entries.get(entryName);
            return vfile != null ? vfile.toURL() : null;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    private synchronized void loadEntries() throws URISyntaxException, IOException
    {
        if (m_entries.size() == 0)
        {
            final VirtualFile root = VFS.getChild(m_url.toURI());
            final String uriPath = m_url.toURI().getPath();

            root.visit(new VirtualFileVisitor()
            {
                public void visit(VirtualFile vfile)
                {
                    String entryPath = vfile.getPathName().substring(uriPath.length());
                    m_entries.put(entryPath, vfile);
                }

                public VisitorAttributes getAttributes()
                {
                    return VisitorAttributes.RECURSE_LEAVES_ONLY;
                }
            });
        }
    }
}
