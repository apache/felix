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
package org.apache.felix.framework.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.ZipFileX;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.resolver.Content;

/**
 * <p>
 * This class implements a bundle archive revision for a standard bundle
 * JAR file. The specified location is the URL of the JAR file. By default,
 * the associated JAR file is copied into the revision's directory on the
 * file system, but it is possible to mark the JAR as 'by reference', which
 * will result in the bundle JAR be used 'in place' and not being copied. In
 * either case, some of the contents may be extracted into the revision
 * directory, such as embedded JAR files and native libraries.
 * </p>
**/
class JarRevision extends BundleRevision
{
    private static final transient String BUNDLE_JAR_FILE = "bundle.jar";

    private File m_bundleFile = null;
    private final ZipFileX m_zipFile;

    public JarRevision(
        Logger logger, Map configMap, File revisionRootDir,
        String location, boolean byReference)
        throws Exception
    {
        this(logger, configMap, revisionRootDir, location, byReference, null);
    }

    public JarRevision(
        Logger logger, Map configMap, File revisionRootDir, String location,
        boolean byReference, InputStream is)
        throws Exception
    {
        super(logger, configMap, revisionRootDir, location);

        if (byReference)
        {
            m_bundleFile = new File(location.substring(
                location.indexOf(BundleArchive.FILE_PROTOCOL)
                    + BundleArchive.FILE_PROTOCOL.length()));
        }
        else
        {
            m_bundleFile = new File(getRevisionRootDir(), BUNDLE_JAR_FILE);
        }

        // Save and process the bundle JAR.
        initialize(byReference, is);

        // Open shared copy of the JAR file.
        ZipFileX zipFile = null;
        try
        {
            // Open bundle JAR file.
            zipFile = BundleCache.getSecureAction().openZipFile(m_bundleFile);
            // Error if no jar file.
            if (zipFile == null)
            {
                throw new IOException("No JAR file found.");
            }
            m_zipFile = zipFile;
        }
        catch (Exception ex)
        {
            if (zipFile != null) zipFile.close();
            throw ex;
        }
    }

    public Map getManifestHeader() throws Exception
    {
        // Get the embedded resource.
        Map headers = getMainAttributes(m_zipFile);
        // Use an empty map if there is no manifest.
        headers = (headers == null) ? new HashMap() : headers;
        // Create a case insensitive map of manifest attributes.
        return new StringMap(headers, false);
    }

    public synchronized Content getContent() throws Exception
    {
        return new JarContent(getLogger(), getConfig(), this, getRevisionRootDir(),
            m_bundleFile, m_zipFile);
    }

    protected void close() throws Exception
    {
        m_zipFile.close();
    }

    //
    // Private methods.
    //

    private void initialize(boolean byReference, InputStream is)
        throws Exception
    {
        try
        {
            // If the revision directory does not exist, then create it.
            if (!BundleCache.getSecureAction().fileExists(getRevisionRootDir()))
            {
                if (!BundleCache.getSecureAction().mkdir(getRevisionRootDir()))
                {
                    getLogger().log(
                        Logger.LOG_ERROR,
                        getClass().getName() + ": Unable to create revision directory.");
                    throw new IOException("Unable to create archive directory.");
                }

                if (!byReference)
                {
                    URLConnection conn = null;
                    try
                    {
                        if (is == null)
                        {
                            // Do it the manual way to have a chance to
                            // set request properties such as proxy auth.
                            URL url = BundleCache.getSecureAction().createURL(
                                null, getLocation(), null);
                            conn = url.openConnection();

                            // Support for http proxy authentication.
                            String auth = BundleCache.getSecureAction()
                                .getSystemProperty("http.proxyAuth", null);
                            if ((auth != null) && (auth.length() > 0))
                            {
                                if ("http".equals(url.getProtocol()) ||
                                    "https".equals(url.getProtocol()))
                                {
                                    String base64 = Util.base64Encode(auth);
                                    conn.setRequestProperty(
                                        "Proxy-Authorization", "Basic " + base64);
                                }
                            }
                            is = BundleCache.getSecureAction()
                                .getURLConnectionInputStream(conn);
                        }

                        // Save the bundle jar file.
                        BundleCache.copyStreamToFile(is, m_bundleFile);
                    }
                    finally
                    {
                        // This is a hack to fix an issue on Android, where
                        // HttpURLConnections are not properly closed. (FELIX-2728)
                        if ((conn != null) && (conn instanceof HttpURLConnection))
                        {
                            ((HttpURLConnection) conn).disconnect();
                        }
                    }
                }
            }
        }
        finally
        {
            if (is != null) is.close();
        }
    }

    public static int readLine(InputStream is, byte[] buf) throws IOException
    {
        for (int i = 0; i < buf.length; i++)
        {
            int b = is.read();
            if (b < 0)
            {
                return (i == 0) ? -1 : i;
            }
            else
            {
                buf[i] = (byte) b;
                if (buf[i] == '\n')
                {
                    return i + 1;
                }
            }
        }
        return 0;
    }

    private static Map<String, String> getMainAttributes(ZipFileX zipFile) throws IOException
    {
        Map<String, String> mainAttrs = new HashMap<String, String>();
        Map<String, byte[]> tmpMap = new HashMap<String, byte[]>();

        byte[] buf = new byte[512];
        ZipEntry ze = zipFile.getEntry("META-INF/MANIFEST.MF");
        InputStream is = new BufferedInputStream(zipFile.getInputStream(ze));
        String lastName = null;
        try
        {
            for (int len = readLine(is, buf); len != -1; len = readLine(is, buf))
            {
                // Make sure line ends with a line feed.
                if (buf[len - 1] != '\n')
                {
                    throw new IOException(
                        "Manifest error: Line either too long or no line feed - "
                        + new String(buf, 0, 0, len));
                }

                // Ignore line feed.
                len--;

                // If line ends with carriage return, ignore it.
                if ((len > 0) && (buf[len - 1] == '\r'))
                {
                    len--;
                }

                // If line is empty, then we've reached the end
                // of the main attributes group.
                if (len == 0)
                {
                    break;
                }

                // Check if this is a continuation line. If so, read the
                // entire line and add it to the previous line value.
                if (buf[0] == ' ')
                {
                    if (lastName == null)
                    {
                        throw new IOException(
                            "Manifest syntax: Invalid line continuation - "
                            + new String(buf, 0, 0, len));
                    }
                    byte[] lastValue = tmpMap.get(lastName);
                    byte[] tmp = new byte[lastValue.length + len - 1];
                    System.arraycopy(lastValue, 0, tmp, 0, lastValue.length);
                    System.arraycopy(buf, 1, tmp, lastValue.length, len - 1);
                    tmpMap.put(lastName, tmp);
                }
                // Otherwise, try to find the attribute name and its value.
                else
                {
                    for (int i = 0; i < len; i++)
                    {
                        // If we are at the end, then this must be an error.
                        if (i == (len - 1))
                        {
                            throw new IOException(
                                "Manifest syntax: Invalid attribute name - "
                                + new String(buf, 0, 0, len));
                        }
                        // We found the end of the attribute name
                        else if (buf[i] == ':')
                        {
                            // Make sure the header has a space separator.
                            if (buf[i + 1] != ' ')
                            {
                                throw new IOException(
                                    "Manifest syntax: Header space separator missing - "
                                    + new String(buf, 0, 0, len));
                            }
                            // Convert attribute name to a string.
                            lastName = new String(buf, 0, 0, i);
                            byte[] tmp = new byte[len - i - 2];
                            System.arraycopy(buf, i + 2, tmp, 0, len - i - 2);
                            byte[] old = tmpMap.put(lastName, tmp);
                            if (old != null)
                            {
                                throw new IllegalArgumentException(
                                    "Manifest syntax: Duplicate header - "
                                    + new String(buf, 0, 0, len));
                            }
                            break;
                        }
                    }
                }
            }

            for (Entry<String, byte[]> entry : tmpMap.entrySet())
            {
                byte[] value = entry.getValue();
                mainAttrs.put(
                    entry.getKey(),
                    new String(value, 0, value.length, "UTF8"));
            }

        }
        finally
        {
            is.close();
        }

        return mainAttrs;
    }
}