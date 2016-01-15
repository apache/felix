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
package org.apache.felix.deployment.rp.autoconf;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PersistencyManager
{
    private static final FileFilter FILES_ONLY_FILTER = new FileFilter()
    {
        public boolean accept(File pathname)
        {
            return pathname.isFile();
        }
    };

    private final File m_root;

    public PersistencyManager(File root)
    {
        m_root = root;
    }

    /**
     * Deletes a resource.
     * 
     * @param name Name of the resource.
     * @throws IOException If the resource could not be deleted.
     */
    public void delete(String name) throws IOException
    {
        name = name.replace('/', File.separatorChar);
        File target = new File(m_root, name);
        if (target.exists() && !target.delete())
        {
            throw new IOException("Unable to delete file: " + target.getAbsolutePath());
        }
        while (target.getParentFile().list().length == 0 && !target.getParentFile().getAbsolutePath().equals(m_root.getAbsolutePath()))
        {
            target = target.getParentFile();
            target.delete();
        }
    }

    /**
     * Returns the names of all persisted resources.
     * @return a list of resource names, never <code>null</code>.
     */
    public List<String> getResourceNames()
    {
        List<String> result = new ArrayList<String>();

        File[] list = m_root.listFiles(FILES_ONLY_FILTER);
        if (list != null && list.length > 0)
        {
            for (File resource : list)
            {
                result.add(resource.getName());
            }
        }
        return result;
    }

    /**
     * Loads a stored resource.
     * 
     * @param name Name of the resource.
     * @return List of <code>AutoConfResource</code>s representing the specified resource, if the resource is unknown an empty list is returned.
     * @throws IOException If the resource could not be properly read.
     */
    public List<AutoConfResource> load(String name) throws IOException
    {
        List<AutoConfResource> resources = new ArrayList<AutoConfResource>();
        name = name.replace('/', File.separatorChar);
        File resourcesFile = new File(m_root, name);
        if (!resourcesFile.exists())
        {
            return resources;
        }

        ObjectInputStream in = null;
        try
        {
            in = new ObjectInputStream(new FileInputStream(resourcesFile));
            resources = (List<AutoConfResource>) in.readObject();
        }
        catch (FileNotFoundException e)
        {
            throw new IOException("Resource does not exist: " + name);
        }
        catch (ClassNotFoundException e)
        {
            throw new IOException("Unable to recreate persisted object from file: " + name);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (Exception e)
                {
                    // not much we can do
                }
            }
        }
        return resources;
    }

    /**
     * Stores a resource.
     * 
     * @param name Name of the resource.
     * @param configs List of <code>AutoConfResource</code>s representing the specified resource.
     * @throws IOException If the resource could not be stored.
     */
    public void store(String name, List<AutoConfResource> configs) throws IOException
    {
        File targetDir = m_root;
        name = name.replace('/', File.separatorChar);

        if (name.startsWith(File.separator))
        {
            name = name.substring(1);
        }
        int lastSeparator = name.lastIndexOf(File.separator);
        File target = null;
        if (lastSeparator != -1)
        {
            targetDir = new File(targetDir, name.substring(0, lastSeparator));
            targetDir.mkdirs();
        }
        target = new File(targetDir, name.substring(lastSeparator + 1));

        ObjectOutputStream out = null;
        try
        {
            out = new ObjectOutputStream(new FileOutputStream(target));
            out.writeObject(configs);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (Exception e)
                {
                    // not much we can do
                }
            }
        }
    }
}
