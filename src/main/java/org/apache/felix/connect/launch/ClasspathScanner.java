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
package org.apache.felix.connect.launch;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

import org.apache.felix.connect.felix.framework.util.MapToDictionary;

public class ClasspathScanner
{
    public List<BundleDescriptor> scanForBundles() throws Exception
    {
        return scanForBundles(null, null);
    }

    public List<BundleDescriptor> scanForBundles(ClassLoader loader) throws Exception
    {
        return scanForBundles(null, loader);
    }

    public List<BundleDescriptor> scanForBundles(String filterString)
            throws Exception
    {
        return scanForBundles(filterString, null);
    }

    public List<BundleDescriptor> scanForBundles(String filterString, ClassLoader loader)
            throws Exception
    {
        Filter filter = (filterString != null) ? FrameworkUtil
                .createFilter(filterString) : null;

        loader = (loader != null) ? loader : getClass().getClassLoader();

        List<BundleDescriptor> bundles = new ArrayList<BundleDescriptor>();
        byte[] bytes = new byte[1024 * 1024 * 2];
        for (Enumeration<URL> e = loader.getResources(
                "META-INF/MANIFEST.MF"); e.hasMoreElements(); )
        {
            URL manifestURL = e.nextElement();
            InputStream input = null;
            try
            {
                input = manifestURL.openStream();
                int size = 0;
                for (int i = input.read(bytes); i != -1; i = input.read(bytes, size, bytes.length - size))
                {
                    size += i;
                    if (size == bytes.length)
                    {
                        byte[] tmp = new byte[size * 2];
                        System.arraycopy(bytes, 0, tmp, 0, bytes.length);
                        bytes = tmp;
                    }
                }

                // Now parse the main attributes. The idea is to do that
                // without creating new byte arrays. Therefore, we read through
                // the manifest bytes inside the bytes array and write them back into
                // the same array unless we don't need them (e.g., \r\n and \n are skipped).
                // That allows us to create the strings from the bytes array without the skipped
                // chars. We stopp as soon as we see a blankline as that denotes that the main
                //attributes part is finished.
                String key = null;
                int last = 0;
                int current = 0;

                Map<String, String> headers = new HashMap<String, String>();
                for (int i = 0; i < size; i++)
                {
                    // skip \r and \n if it is follows by another \n
                    // (we catch the blank line case in the next iteration)
                    if (bytes[i] == '\r')
                    {
                        if ((i + 1 < size) && (bytes[i + 1] == '\n'))
                        {
                            continue;
                        }
                    }
                    if (bytes[i] == '\n')
                    {
                        if ((i + 1 < size) && (bytes[i + 1] == ' '))
                        {
                            i++;
                            continue;
                        }
                    }
                    // If we don't have a key yet and see the first : we parse it as the key
                    // and skip the :<blank> that follows it.
                    if ((key == null) && (bytes[i] == ':'))
                    {
                        key = new String(bytes, last, (current - last), "UTF-8");
                        if ((i + 1 < size) && (bytes[i + 1] == ' '))
                        {
                            last = current + 1;
                            continue;
                        }
                        else
                        {
                            throw new Exception(
                                    "Manifest error: Missing space separator - " + key);
                        }
                    }
                    // if we are at the end of a line
                    if (bytes[i] == '\n')
                    {
                        // and it is a blank line stop parsing (main attributes are done)
                        if ((last == current) && (key == null))
                        {
                            break;
                        }
                        // Otherwise, parse the value and add it to the map (we throw an
                        // exception if we don't have a key or the key already exist.
                        String value = new String(bytes, last, (current - last), "UTF-8");
                        if (key == null)
                        {
                            throw new Exception("Manifst error: Missing attribute name - " + value);
                        }
                        else if (headers.put(key, value) != null)
                        {
                            throw new Exception("Manifst error: Duplicate attribute name - " + key);
                        }
                        last = current;
                        key = null;
                    }
                    else
                    {
                        // write back the byte if it needs to be included in the key or the value.
                        bytes[current++] = bytes[i];
                    }
                }
                if ((filter == null)
                        || filter.match(new MapToDictionary<String, String>(headers)))
                {
                    bundles.add(new BundleDescriptor(loader, getParentURL(manifestURL).toExternalForm(), headers));
                }
            }
            finally
            {
                if (input != null)
                {
                    input.close();
                }
            }
        }
        return bundles;
    }

    private URL getParentURL(URL url) throws Exception
    {
        String externalForm = url.toExternalForm();
        return new URL(externalForm.substring(0, externalForm.length()
                - "META-INF/MANIFEST.MF".length()));
    }
}
