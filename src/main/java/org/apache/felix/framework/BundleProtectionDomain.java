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
package org.apache.felix.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.apache.felix.framework.cache.Content;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.PackagePermission;

import org.osgi.framework.wiring.BundleRevision;

public class BundleProtectionDomain extends ProtectionDomain
{
    private static final class BundleInputStream extends InputStream
    {
        private final Content m_root;
        private final Enumeration m_content;
        private final OutputStreamBuffer m_outputBuffer = new OutputStreamBuffer();

        private ByteArrayInputStream m_buffer = null;
        private JarOutputStream m_output = null;

        private static final String DUMMY_ENTRY = "__DUMMY-ENTRY__/";

        public BundleInputStream(Content root) throws IOException
        {
            m_root = root;

            List entries = new ArrayList();

            int count = 0;
            String manifest = null;
            for (Enumeration e = m_root.getEntries(); e.hasMoreElements();)
            {
                String entry = (String) e.nextElement();
                if (entry.endsWith("/"))
                {
                    // ignore
                }
                else if (entry.equalsIgnoreCase("META-INF/MANIFEST.MF"))
                {
                    if (manifest == null)
                    {
                        manifest = entry;
                    }
                }
                else if (entry.toUpperCase().startsWith("META-INF/")
                            && entry.indexOf('/', "META-INF/".length()) < 0)
                {
                    entries.add(count++, entry);
                }
                else
                {
                    entries.add(entry);
                }
            }
            entries.add(count++, DUMMY_ENTRY);
            if (manifest == null)
            {
                manifest = "META-INF/MANIFEST.MF";
            }
            m_content = Collections.enumeration(entries);

            m_output = new JarOutputStream(m_outputBuffer);
            readNext(manifest);
            m_buffer = new ByteArrayInputStream(m_outputBuffer.m_outBuffer
                .toByteArray());

            m_outputBuffer.m_outBuffer = null;
        }

        public int read() throws IOException
        {
            if ((m_output == null) && (m_buffer == null))
            {
                return -1;
            }

            if (m_buffer != null)
            {
                int result = m_buffer.read();

                if (result == -1)
                {
                    m_buffer = null;
                    return read();
                }

                return result;
            }

            if (m_content.hasMoreElements())
            {
                String current = (String) m_content.nextElement();

                readNext(current);

                if (!m_content.hasMoreElements())
                {
                    m_output.close();
                    m_output = null;
                }

                m_buffer = new ByteArrayInputStream(m_outputBuffer.m_outBuffer
                    .toByteArray());

                m_outputBuffer.m_outBuffer = null;
            }
            else
            {
                m_output.close();
                m_output = null;
            }

            return read();
        }

        private void readNext(String path) throws IOException
        {
            m_outputBuffer.m_outBuffer = new ByteArrayOutputStream();

            if (path == DUMMY_ENTRY)
            {
                JarEntry entry = new JarEntry(path);

                m_output.putNextEntry(entry);
            }
            else
            {
                InputStream in = null;
                try
                {
                    in = m_root.getEntryAsStream(path);

                    if (in == null)
                    {
                        throw new IOException("Missing entry");
                    }

                    JarEntry entry = new JarEntry(path);

                    m_output.putNextEntry(entry);

                    byte[] buffer = new byte[4 * 1024];

                    for (int c = in.read(buffer); c != -1; c = in.read(buffer))
                    {
                        m_output.write(buffer, 0, c);
                    }
                }
                finally
                {
                    if (in != null)
                    {
                        try
                        {
                            in.close();
                        }
                        catch (Exception ex)
                        {
                            // Not much we can do
                        }
                    }
                }
            }

            m_output.closeEntry();

            m_output.flush();
        }
    }

    private static final class OutputStreamBuffer extends OutputStream
    {
        ByteArrayOutputStream m_outBuffer = null;

        public void write(int b)
        {
            m_outBuffer.write(b);
        }

        public void write(byte[] buffer) throws IOException
        {
            m_outBuffer.write(buffer);
        }

        public void write(byte[] buffer, int offset, int length)
        {
            m_outBuffer.write(buffer, offset, length);
        }
    }

    private static final class RevisionAsJarURL extends URLStreamHandler
    {
        private final WeakReference m_revision;

        private RevisionAsJarURL(BundleRevisionImpl revision)
        {
            m_revision = new WeakReference(revision);
        }


        @Override
        protected URLConnection openConnection(URL u) throws IOException
        {
            return new JarURLConnection(u)
            {
                @Override
                public JarFile getJarFile() throws IOException
                {
                    BundleRevisionImpl revision = (BundleRevisionImpl) m_revision.get();

                    if (revision != null)
                    {
                        Content content = revision.getContent();
                        if (content instanceof JarContent)
                        {
                            return Felix.m_secureAction.openJarFile(((JarContent) content).getFile());
                        }
                        else
                        {
                            File target = Felix.m_secureAction.createTempFile("jar", null, null);
                            Felix.m_secureAction.deleteFileOnExit(target);
                            FileOutputStream output = null;
                            InputStream input = null;
                            IOException rethrow = null;
                            try
                            {
                                output = new FileOutputStream(target);
                                input = new BundleInputStream(content);
                                byte[] buffer = new byte[64 * 1024];
                                for (int i = input.read(buffer);i != -1; i = input.read(buffer))
                                {
                                    output.write(buffer,0, i);
                                }
                            }
                            catch (IOException ex)
                            {
                                rethrow = ex;
                            }
                            finally
                            {
                                if (output != null)
                                {
                                    try
                                    {
                                        output.close();
                                    }
                                    catch (IOException ex)
                                    {
                                        if (rethrow == null)
                                        {
                                            rethrow = ex;
                                        }
                                    }
                                }

                                if (input != null)
                                {
                                    try
                                    {
                                        input.close();
                                    }
                                    catch (IOException ex)
                                    {
                                        if (rethrow == null)
                                        {
                                            rethrow = ex;
                                        }
                                    }
                                }

                                if (rethrow != null)
                                {
                                    throw rethrow;
                                }
                            }
                            return Felix.m_secureAction.openJarFile(target);
                        }
                    }
                    throw new IOException("Unable to access bundle revision.");
                }

                @Override
                public void connect() throws IOException
                {

                }
            };
        }

        private static URL create(BundleRevisionImpl revision) throws MalformedURLException
        {
            RevisionAsJarURL handler = new RevisionAsJarURL(revision);

            boolean useCachedUrlForCodeSource = Boolean.parseBoolean(
                    revision.getBundle().getFramework().getProperty(FelixConstants.USE_CACHEDURLS_PROPS));
            if (useCachedUrlForCodeSource)
            {
                String location = "jar:" + revision.getEntry("/") + "!/";
                return Felix.m_secureAction.createURL(
                        Felix.m_secureAction.createURL(null, "jar:", handler),
                        location,
                        handler
                );
            }

            String location = revision.getBundle()._getLocation();
            if (location.startsWith("reference:"))
            {
                location = location.substring("reference:".length());
            }
            URL url;
            try
            {
                url = Felix.m_secureAction.createURL(
                    Felix.m_secureAction.createURL(null, "jar:", handler), location, null);
            }
            catch (MalformedURLException ex)
            {
                url = null;
            }

            if (url != null && !url.getProtocol().equalsIgnoreCase("jar"))
            {
                return url;
            }
            else if (url == null)
            {
                location = "jar:" + revision.getEntry("/") + "!/";
            }

            return Felix.m_secureAction.createURL(
                Felix.m_secureAction.createURL(null, "jar:", handler),
                location,
                handler
            );
        }
    }

    private final WeakReference<BundleRevisionImpl> m_revision;
    private final int m_hashCode;
    private final String m_toString;
    private volatile PermissionCollection m_woven;

    BundleProtectionDomain(BundleRevisionImpl revision, Object certificates)
        throws MalformedURLException
    {
        super(
            new CodeSource(
                RevisionAsJarURL.create(revision),
                (Certificate[]) certificates),
            null, null, null);
        m_revision = new WeakReference<BundleRevisionImpl>(revision);
        m_hashCode = revision.hashCode();
        m_toString = "[" + revision + "]";
    }

    BundleRevisionImpl getRevision()
    {
        return m_revision.get();
    }

    public boolean implies(Permission permission)
    {
        Felix felix = getFramework();
        return felix != null && felix.impliesBundlePermission(this, permission, false);
    }

    boolean superImplies(Permission permission)
    {
        return super.implies(permission);
    }

    public boolean impliesDirect(Permission permission)
    {
        Felix felix = getFramework();
        return felix != null && felix.impliesBundlePermission(this, permission, true);
    }

    boolean impliesWoven(Permission permission)
    {
        return m_woven != null && m_woven.implies(permission);
    }

    synchronized void addWoven(String s)
    {
        if (m_woven == null)
        {
            m_woven = new Permissions();
        }
        m_woven.add(new PackagePermission(s, PackagePermission.IMPORT));
    }

    BundleImpl getBundle()
    {
        BundleRevisionImpl revision = m_revision.get();
        return revision != null ? revision.getBundle() : null;
    }

    Felix getFramework() {
        BundleRevisionImpl revision = m_revision.get();
        return revision != null ? revision.getBundle().getFramework() : null;
    }

    public int hashCode()
    {
        return m_hashCode;
    }

    public boolean equals(Object other)
    {
        if ((other == null) || (other.getClass() != BundleProtectionDomain.class))
        {
            return false;
        }
        if (m_hashCode != other.hashCode())
        {
            return false;
        }
        return m_revision.get() == ((BundleProtectionDomain) other).m_revision.get();
    }

    public String toString()
    {
        return m_toString;
    }
}
