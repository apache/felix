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

import java.io.File;
import java.util.Enumeration;
import java.util.NoSuchElementException;

class FileEntriesEnumeration implements Enumeration<String>
{
    private final File m_dir;
    private final File[] m_children;
    private int m_counter = 0;

    public FileEntriesEnumeration(File dir)
    {
        m_dir = dir;
        m_children = listFilesRecursive(m_dir);
    }

    public synchronized boolean hasMoreElements()
    {
        return (m_children != null) && (m_counter < m_children.length);
    }

    public synchronized String nextElement()
    {
        if ((m_children == null) || (m_counter >= m_children.length))
        {
            throw new NoSuchElementException("No more entry paths.");
        }

        // Convert the file separator character to slashes.
        String abs = m_children[m_counter].getAbsolutePath().replace(
                File.separatorChar, '/');

        // Remove the leading path of the reference directory, since the
        // entry paths are supposed to be relative to the root.
        StringBuilder sb = new StringBuilder(abs);
        sb.delete(0, m_dir.getAbsolutePath().length() + 1);
        // Add a '/' to the end of directory entries.
        if (m_children[m_counter].isDirectory())
        {
            sb.append('/');
        }
        m_counter++;
        return sb.toString();
    }

    private File[] listFilesRecursive(File dir)
    {
        File[] children = dir.listFiles();
        File[] combined = children;
        for (File aChildren : children)
        {
            if (aChildren.isDirectory())
            {
                File[] grandchildren = listFilesRecursive(aChildren);
                if (grandchildren.length > 0)
                {
                    File[] tmp = new File[combined.length + grandchildren.length];
                    System.arraycopy(combined, 0, tmp, 0, combined.length);
                    System.arraycopy(grandchildren, 0, tmp, combined.length,
                            grandchildren.length);
                    combined = tmp;
                }
            }
        }
        return combined;
    }
}