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
package org.apache.felix.metatype;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;


public class MockBundle implements Bundle
{

    private BundleContext bundleContext;
    private long bundleId;
    private String bundleSymbolicName;
    private Hashtable headers = new Hashtable();


    MockBundle( BundleContext bundleContext, long bundleId, String bundleSymbolicName )
    {
        this.bundleContext = bundleContext;
        this.bundleId = bundleId;
        this.bundleSymbolicName = bundleSymbolicName;
    }


    public BundleContext getBundleContext()
    {
        return bundleContext;
    }


    public Enumeration findEntries( String path, String filePattern, boolean recurse )
    {
        return new Enumeration()
        {
            public boolean hasMoreElements()
            {
                return false;
            }


            public java.lang.Object nextElement()
            {
                throw new NoSuchElementException();
            }
        };
    }


    public long getBundleId()
    {
        return bundleId;
    }


    public URL getEntry( String name )
    {
        return getResource( name );
    }


    public Enumeration getEntryPaths( String path )
    {
        return null;
    }


    public Dictionary getHeaders()
    {
        return headers;
    }


    public Dictionary getHeaders( String locale )
    {
        return headers;
    }


    public long getLastModified()
    {
        return 0;
    }


    public String getLocation()
    {
        return "mock";
    }


    public ServiceReference[] getRegisteredServices()
    {
        return null;
    }


    public URL getResource( String name )
    {
        return getClass().getClassLoader().getResource( name );
    }


    public Enumeration getResources( String name ) throws IOException
    {
        return getClass().getClassLoader().getResources( name );
    }


    public ServiceReference[] getServicesInUse()
    {
        return null;
    }


    public int getState()
    {
        return Bundle.ACTIVE;
    }


    public String getSymbolicName()
    {
        return bundleSymbolicName;
    }


    public boolean hasPermission( java.lang.Object permission )
    {
        return true;
    }


    public Class loadClass( String name ) throws ClassNotFoundException
    {
        return getClass().getClassLoader().loadClass( name );
    }


    public void start()
    {
    }


    public void start( int options )
    {
    }


    public void stop()
    {
    }


    public void stop( int options )
    {
    }


    public void uninstall()
    {
    }


    public void update()
    {
    }


    public void update( InputStream in )
    {
    }


    public int compareTo(Bundle o)
    {
        return 0;
    }


    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType)
    {
        return null;
    }


    public Version getVersion()
    {
        return null;
    }


    public <A> A adapt(Class<A> type)
    {
        return null;
    }


    public File getDataFile(String filename)
    {
        return null;
    }
}
