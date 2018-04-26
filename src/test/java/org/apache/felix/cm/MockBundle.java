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
package org.apache.felix.cm;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;


public class MockBundle implements Bundle
{

    private final BundleContext context;
    private final String location;


    public MockBundle( BundleContext context, String location )
    {
        this.context = context;
        this.location = location;
    }


    @Override
    public Enumeration findEntries( String arg0, String arg1, boolean arg2 )
    {
        return null;
    }


    @Override
    public BundleContext getBundleContext()
    {
        return context;
    }


    @Override
    public long getBundleId()
    {
        return 0;
    }


    @Override
    public URL getEntry( String arg0 )
    {
        return null;
    }


    @Override
    public Enumeration getEntryPaths( String arg0 )
    {
        return null;
    }


    @Override
    public Dictionary getHeaders()
    {
        return null;
    }


    @Override
    public Dictionary getHeaders( String arg0 )
    {
        return null;
    }


    @Override
    public long getLastModified()
    {
        return 0;
    }


    @Override
    public String getLocation()
    {
        return location;
    }


    @Override
    public ServiceReference[] getRegisteredServices()
    {
        return null;
    }


    @Override
    public URL getResource( String arg0 )
    {
        return null;
    }


    @Override
    public Enumeration getResources( String arg0 )
    {
        return null;
    }


    @Override
    public ServiceReference[] getServicesInUse()
    {
        return null;
    }


    @Override
    public int getState()
    {
        return 0;
    }


    @Override
    public String getSymbolicName()
    {
        return null;
    }


    @Override
    public boolean hasPermission( Object arg0 )
    {
        return false;
    }


    @Override
    public Class loadClass( String arg0 ) throws ClassNotFoundException
    {
        throw new ClassNotFoundException( arg0 );
    }


    @Override
    public void start()
    {
    }


    @Override
    public void stop()
    {
    }


    @Override
    public void uninstall()
    {
    }


    @Override
    public void update()
    {
    }


    @Override
    public void update( InputStream arg0 ) throws BundleException
    {
        if ( arg0 != null )
        {
            try
            {
                arg0.close();
            }
            catch ( IOException ioe )
            {
                throw new BundleException( ioe.getMessage(), ioe );
            }
        }
    }


    @Override
    public void start( int options )
    {
    }


    @Override
    public void stop( int options )
    {
    }


    @Override
    public int compareTo( Bundle o )
    {
        return 0;
    }


    // Framework 1.5 additions

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates( int signersType )
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.5" );
    }


    @Override
    public Version getVersion()
    {
        return Version.emptyVersion;
    }


    // Framework 1.6 additions

    @Override
    public <A> A adapt( Class<A> type )
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.6" );
    }


    @Override
    public File getDataFile( String filename )
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.6" );
    }

}
