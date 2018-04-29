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

package org.apache.felix.sigil.eclipse;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author dave
 *
 */
public class PathUtil
{
    /**
     * @param sourceRootPath
     * @return
     */
    public static IPath newPathIfNotNull(String path)
    {
        return path == null ? null : new Path(path);
    }

    /**
     * @param absolutePath
     * @return
     */
    public static IPath newPathIfExists(File file)
    {
        if (file == null)
            return null;
        if (file.exists())
            return new Path(file.getAbsolutePath());
        // fine
        return null;
    }

}
