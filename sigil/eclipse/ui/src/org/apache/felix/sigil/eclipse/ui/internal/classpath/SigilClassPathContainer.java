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

package org.apache.felix.sigil.eclipse.ui.internal.classpath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.job.ThreadProgressMonitor;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.ui.SigilUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

/**
 * @author dave
 *
 */
public class SigilClassPathContainer implements IClasspathContainer
{

    private static final String CLASSPATH = "classpath";
    private static final String EMPTY = "empty";

    private IClasspathEntry[] entries;
    
    private final ISigilProjectModel sigil;
    
    public SigilClassPathContainer(ISigilProjectModel sigil)
    {
        this.sigil = sigil;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries()
    {
        if (entries == null)
        {
            buildClassPathEntries();
        }

        return entries;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
     */
    public String getDescription()
    {
        return "Bundle Context Classpath";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
     */
    public int getKind()
    {
        return K_SYSTEM;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
     */
    public IPath getPath()
    {
        return new Path(SigilCore.CLASSPATH_CONTAINER_PATH);
    }

    /**
     * @return
     * @throws CoreException 
     * @throws CoreException 
     */
    private void buildClassPathEntries()
    {
        try
        {
            if (sigil.exists()) {
                entries = getCachedClassPath(sigil);
                
                if ( entries == null ) {
                    IProgressMonitor monitor = ThreadProgressMonitor.getProgressMonitor();
                    entries = sigil.findExternalClasspath(monitor).toArray(new IClasspathEntry[0]);
                    
                    cacheClassPath(sigil, entries);
                }
            }
        }
        catch (CoreException e)
        {
            SigilCore.error("Failed to build classpath entries", e);
        }
        finally
        {
            if (entries == null)
            {
                entries = new IClasspathEntry[] {};
            }
        }
    }
    
    static IClasspathEntry[] getCachedClassPath(ISigilProjectModel project) {
        File f = getClassPathDir(project);
        if ( f == null || !f.exists() ) return null;
        
        File[] entries = f.listFiles();
        if ( entries == null || entries.length == 0 ) return null;
        
        ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>(entries.length);
        for(File entry : entries ) {
            if ( EMPTY.equals(entry.getName() ) ) {
                list.clear();
                break;
            }
            else {
                try
                {
                    list.add(readClassPath(project.getJavaModel(), entry));
                }
                catch (IOException e)
                {
                    SigilCore.warn("Failed to read classpath entry " + entry, e);
                }
            }
        }
        return list.toArray(new IClasspathEntry[list.size()]);
    }

    private static File getClassPathDir(ISigilProjectModel project)
    {
        IPath loc = project.getProject().getWorkingLocation(SigilUI.PLUGIN_ID);
        
        if ( loc == null ) return null;
        
        loc = loc.append(CLASSPATH);
        
        return new File(loc.toOSString());
    }

    static void cacheClassPath(ISigilProjectModel project, IClasspathEntry[] entries)
    {
        File f = getClassPathDir(project);
        
        if ( f == null ) return;
        
        if ( !f.exists() ) {
            if (!f.mkdirs()) {
                SigilCore.warn("Failed to create temp working directory " + f);
                return;
            }
        }
        
        try
        {
            if (entries.length == 0)
            {
                File empty = new File(f, EMPTY);
                empty.createNewFile();
            }
            else {
                int i = 0;
                for(IClasspathEntry e : entries) {
                    File entry = new File(f, Integer.toString(i++));
                    writeClassPath(project.getJavaModel(), entry, e);
                }
            }
        }
        catch (IOException e)
        {
            SigilCore.warn("Failed to read write classpath entries", e);
        }
    }

    /**
     * @param sigil
     */
    static void flushCachedClassPath(ISigilProjectModel project)
    {
        File f = getClassPathDir(project);
        
        if ( f == null || !f.exists() ) return;
        
        File[] files = f.listFiles();
        if ( files == null ) return;
        
        for (File entry : files) {
            entry.delete();
        }
    }
    
    private static IClasspathEntry readClassPath(IJavaProject javaModel, File entry) throws IOException
    {
        FileInputStream in = new FileInputStream(entry);
        
        try
        {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            
            byte[] b = new byte[1024];
            
            for(;;) {
                int r = in.read(b);
                if ( r == -1 ) break;
                buf.write(b, 0, r);
            }
            
            String enc = buf.toString();
            return javaModel.decodeClasspathEntry(enc);
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                SigilCore.warn("Failed to close stream to " + entry, e);
            }
        }
    }

    private static void writeClassPath(IJavaProject javaModel, File file, IClasspathEntry entry) throws IOException 
    {
        String enc = javaModel.encodeClasspathEntry(entry);
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(enc.getBytes());
            out.flush();
        }
        finally {
            try
            {
                out.close();
            }
            catch (IOException e)
            {
                SigilCore.warn("Failed to close stream to " + entry, e);
            }            
        }
    }
    
}
