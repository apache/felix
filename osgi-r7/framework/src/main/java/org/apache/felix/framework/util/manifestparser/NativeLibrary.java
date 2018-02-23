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
package org.apache.felix.framework.util.manifestparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.osgi.framework.Constants;

public class NativeLibrary
{
    private String m_libraryFile;
    private String[] m_osnames;
    private String[] m_processors;
    private String[] m_osversions;
    private String[] m_languages;
    private String m_selectionFilter;

    public NativeLibrary(
        String libraryFile, String[] osnames, String[] processors, String[] osversions,
        String[] languages, String selectionFilter) throws Exception
    {
        m_libraryFile = libraryFile;
        m_osnames = osnames;
        m_processors = processors;
        m_osversions = osversions;
        m_languages = languages;
        m_selectionFilter = selectionFilter;
    }

    public String getEntryName()
    {
        return m_libraryFile;
    }

    public String[] getOSNames()
    {
        return m_osnames;
    }

    public String[] getProcessors()
    {
        return m_processors;
    }

    public String[] getOSVersions()
    {
        return m_osversions;
    }

    public String[] getLanguages()
    {
        return m_languages;
    }

    public String getSelectionFilter()
    {
        return m_selectionFilter;
    }

    /**
     * <p>
     * Determines if the specified native library name matches this native
     * library definition.
     * </p>
     * @param name the native library name to try to match.
     * @return <tt>true</tt> if this native library name matches this native
     *         library definition; <tt>false</tt> otherwise.
    **/
    public boolean match(Map configMap, String name)
    {
        // First, check for an exact match.
        boolean matched = false;
        if (m_libraryFile.equals(name) || m_libraryFile.endsWith("/" + name))
        {
            matched = true;
        }

        // Then check the mapped name.
        String libname = System.mapLibraryName(name);
        // As well as any additional library file extensions.
        List<String> exts = ManifestParser.parseDelimitedString(
            (String) configMap.get(Constants.FRAMEWORK_LIBRARY_EXTENSIONS), ",");
        if (exts == null)
        {
            exts = new ArrayList<String>();
        }
        // For Mac OSX, try dylib too.
        if (libname.endsWith(".jnilib") && m_libraryFile.endsWith(".dylib"))
        {
            exts.add("dylib");
        }
        if (libname.endsWith(".dylib") && m_libraryFile.endsWith(".jnilib"))
        {
            exts.add("jnilib");
        }
        // Loop until we find a match or not.
        int extIdx = -1;
        while (!matched && (extIdx < exts.size()))
        {
            // Check if the current name matches.
            if (m_libraryFile.equals(libname) || m_libraryFile.endsWith("/" + libname))
            {
                matched = true;
            }

            // Increment extension index.
            extIdx++;

            // If we have other native library extensions to try, then
            // calculate the new native library name.
            if (!matched && (extIdx < exts.size()))
            {
                int idx = libname.lastIndexOf(".");
                libname = (idx < 0)
                    ? libname + "." + exts.get(extIdx)
                    : libname.substring(0, idx + 1) + exts.get(extIdx);
            }
        }

        return matched;
    }

    public String toString()
    {
        if (m_libraryFile != null)
        {
            StringBuffer sb = new StringBuffer();
            sb.append(m_libraryFile);
            for (int i = 0; (m_osnames != null) && (i < m_osnames.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_OSNAME);
                sb.append('=');
                sb.append(m_osnames[i]);
            }
            for (int i = 0; (m_processors != null) && (i < m_processors.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_PROCESSOR);
                sb.append('=');
                sb.append(m_processors[i]);
            }
            for (int i = 0; (m_osversions != null) && (i < m_osversions.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_OSVERSION);
                sb.append('=');
                sb.append(m_osversions[i]);
            }
            for (int i = 0; (m_languages != null) && (i < m_languages.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_LANGUAGE);
                sb.append('=');
                sb.append(m_languages[i]);
            }
            if (m_selectionFilter != null)
            {
                sb.append(';');
                sb.append(Constants.SELECTION_FILTER_ATTRIBUTE);
                sb.append('=');
                sb.append('\'');
                sb.append(m_selectionFilter);
            }

            return sb.toString();
        }
        return "*";
    }
}