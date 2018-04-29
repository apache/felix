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


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Enumeration findEntries( String arg0, String arg1, boolean arg2 )
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public BundleContext getBundleContext()
    {
        return context;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public long getBundleId()
    {
        return 0;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public URL getEntry( String arg0 )
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Enumeration getEntryPaths( String arg0 )
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Dictionary getHeaders()
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Dictionary getHeaders( String arg0 )
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public long getLastModified()
    {
        return 0;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public String getLocation()
    {
        return location;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public ServiceReference[] getRegisteredServices()
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public URL getResource( String arg0 )
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Enumeration getResources( String arg0 )
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public ServiceReference[] getServicesInUse()
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public int getState()
    {
        return 0;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public String getSymbolicName()
    {
        return null;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean hasPermission( Object arg0 )
    {
        return false;
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Class loadClass( String arg0 ) throws ClassNotFoundException
    {
        throw new ClassNotFoundException( arg0 );
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void start()
    {
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void stop()
    {
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void uninstall()
    {
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void update()
    {
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void start( int options )
    {
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void stop( int options )
    {
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public int compareTo( Bundle o )
    {
        return 0;
    }


    // Framework 1.5 additions

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates( int signersType )
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.5" );
    }


<<<<<<< HEAD
    public Version getVersion()
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.5" );
=======
    @Override
    public Version getVersion()
    {
        return Version.emptyVersion;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    // Framework 1.6 additions

<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public <A> A adapt( Class<A> type )
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.6" );
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public File getDataFile( String filename )
    {
        throw new AbstractMethodError( "Not supported on Framework API 1.4; added in Framework API 1.6" );
    }

}
