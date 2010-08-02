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

/**
 * @author dave
 *
 */
public class StandardResource extends AbstractResource
{
    private final String fsPath;

    /**
     * @param bldProject 
     * @param bPath2
     * @param fsPath2
     */
    public StandardResource(IBldProject project, String bPath, String fsPath)
    {
        super(project, bPath);
        this.fsPath = fsPath;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.core.Resource#getLocalFile()
     */
    public String getLocalFile()
    {
        return fsPath == null ? bPath : fsPath;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(bPath);
        if (fsPath != null)
        {
            sb.append('=');
            sb.append(fsPath);
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.core.Resource#toBNDInstruction(java.io.File[])
     */
    public String toBNDInstruction(File[] classpath)
    {
        StringBuilder sb = new StringBuilder();
        String fsp = fsPath;
        if (fsp == null)
            fsp = bPath;

        fsp = findFileSystemPath(fsp, classpath);

        sb.append(bPath);
        sb.append('=');
        sb.append(fsp);

        return sb.toString();
    }

}
