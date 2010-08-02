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

package org.apache.felix.sigil.common.config.internal;

import java.io.File;

import org.apache.felix.sigil.common.config.IBldProject;
import org.apache.felix.sigil.common.config.Resource;

/**
 * @author dave
 *
 */
public abstract class AbstractResource implements Resource
{
    protected final String bPath;
    protected final IBldProject project;

    protected AbstractResource(IBldProject project, String bPath)
    {
        if (bPath == null)
            throw new NullPointerException();

        if (project == null)
            throw new NullPointerException();

        this.bPath = bPath;
        this.project = project;
    }

    protected String findFileSystemPath(String fsPath, File[] classpath)
    {
        File resolved = project.resolve(fsPath);

        // fsPath may contain Bnd variable, making path appear to not exist

        if (!resolved.exists())
        {
            // Bnd already looks for classpath jars
            File found = findInClasspathDir(fsPath, classpath);
            if (found != null)
            {
                fsPath = found.getPath();
            }
            else
            {
                fsPath = resolved.getAbsolutePath();
            }
        }
        else
        {
            fsPath = resolved.getAbsolutePath();
        }

        return fsPath;
    }

    private File findInClasspathDir(String file, File[] classpath)
    {
        for (File cp : classpath)
        {
            if (cp.isDirectory())
            {
                File path = new File(cp, file);
                if (path.exists())
                {
                    return path;
                }
            }
        }

        return null;
    }
}
