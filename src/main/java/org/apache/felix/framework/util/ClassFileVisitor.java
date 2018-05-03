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

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

@IgnoreJRERequirement
public class ClassFileVisitor extends java.nio.file.SimpleFileVisitor<java.nio.file.Path>
{
    private final Set<String> m_imports;
    private final Set<String> m_exports;
    private final ClassParser m_classParser;
    private final SortedMap<String, SortedSet<String>> m_result;

    public ClassFileVisitor(Set<String> imports, Set<String> exports, ClassParser classParser, SortedMap<String, SortedSet<String>> result)
    {
        m_imports = imports;
        m_exports = exports;
        m_classParser = classParser;
        m_result = result;
    }

    @Override
    public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException
    {
        if (file.getNameCount() > 3)
        {
            String name = file.subpath(2, file.getNameCount() - 1).toString().replace("/", ".");
            if (m_exports.contains(name) && file.toString().endsWith(".class"))
            {
                SortedSet<String> strings = m_result.get(name);

                if (!name.startsWith("java."))
                {
                    try
                    {
                        Set<String> refs = m_classParser.parseClassFileUses(file.toString(), java.nio.file.Files.newInputStream(file));
                        refs.retainAll(m_imports);
                        refs.remove(name);
                        if (strings == null)
                        {
                            strings = new TreeSet<String>(refs);
                            m_result.put(name, strings);
                        }
                        else
                        {
                            strings.addAll(refs);
                        }
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
                else if (strings == null)
                {
                    m_result.put(name, new TreeSet<String>());
                }
            }
        }
        return java.nio.file.FileVisitResult.CONTINUE;
    }
}