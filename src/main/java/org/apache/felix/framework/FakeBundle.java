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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 *
 */
public class FakeBundle extends BundleImpl implements Bundle
{
    private final Map m_certs;

    public FakeBundle(Map certs)
    {
        m_certs = Collections.unmodifiableMap(certs);
    }

    public Enumeration findEntries(String arg0, String arg1, boolean arg2)
    {
        return null;
    }

    public BundleContext getBundleContext()
    {
        return null;
    }

    public long getBundleId()
    {
        return -1;
    }

    public URL getEntry(String arg0)
    {
        return null;
    }

    public Enumeration getEntryPaths(String arg0)
    {
        return null;
    }

    public Dictionary getHeaders()
    {
        return new Hashtable();
    }

    public Dictionary getHeaders(String arg0)
    {
        return new Hashtable();
    }

    public long getLastModified()
    {
        return 0;
    }

    public String getLocation()
    {
        return "";
    }

    public ServiceReference[] getRegisteredServices()
    {
        return null;
    }

    public URL getResource(String arg0)
    {
        return null;
    }

    public Enumeration getResources(String arg0) throws IOException
    {
        return null;
    }

    public ServiceReference[] getServicesInUse()
    {
        return null;
    }

    public Map getSignerCertificates(int arg0)
    {
        return m_certs;
    }

    public int getState()
    {
        return Bundle.UNINSTALLED;
    }

    public String getSymbolicName()
    {
        return null;
    }

    public Version getVersion()
    {
        return Version.emptyVersion;
    }

    public boolean hasPermission(Object arg0)
    {
        return false;
    }

    public Class loadClass(String arg0) throws ClassNotFoundException
    {
        return null;
    }

    public void start() throws BundleException
    {
        throw new IllegalStateException();
    }

    public void start(int arg0) throws BundleException
    {
        throw new IllegalStateException();
    }

    public void stop() throws BundleException
    {
        throw new IllegalStateException();
    }

    public void stop(int arg0) throws BundleException
    {
        throw new IllegalStateException();
    }

    public void uninstall() throws BundleException
    {
        throw new IllegalStateException();
    }

    public void update() throws BundleException
    {
        throw new IllegalStateException();
    }

    public void update(InputStream arg0) throws BundleException
    {
        throw new IllegalStateException();
    }

    public boolean equals(Object o)
    {
        return this == o;
    }

    public int hashCode()
    {
        return System.identityHashCode(this);
    }

    public int compareTo(Bundle o) {
        // TODO Auto-generated method stub
        return 0;
    }

    public Object adapt(Class arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public File getDataFile(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public int compareTo(Object t)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
