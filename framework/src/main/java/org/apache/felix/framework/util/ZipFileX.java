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
package org.apache.felix.framework.util;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The purpose of this class is to fix an apparent bug in the JVM in versions
 * 1.4.2 and lower where directory entries in ZIP/JAR files are not correctly
 * identified.
**/
public class ZipFileX extends ZipFile
{
    public ZipFileX(File file) throws IOException
    {
        super(file);
    }


    public ZipFileX(File file, int mode) throws IOException
    {
        super(file, mode);
    }

    public ZipFileX(String name) throws IOException
    {
        super(name);
    }

    public ZipEntry getEntry(String name)
    {
        ZipEntry entry = super.getEntry(name);
        if ((entry != null) && (entry.getSize() == 0) && !entry.isDirectory())
        {
            ZipEntry dirEntry = super.getEntry(name + '/');
            if (dirEntry != null)
            {
                entry = dirEntry;
            }
        }
        return entry;
    }
}