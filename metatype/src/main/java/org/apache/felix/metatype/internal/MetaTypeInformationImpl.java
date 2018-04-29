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
package org.apache.felix.metatype.internal;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.felix.metatype.DefaultMetaTypeProvider;
import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.DesignateObject;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.OCD;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>MetaTypeInformationImpl</code> class implements the
 * <code>MetaTypeInformation</code> interface returned from the
 * <code>MetaTypeService</code>.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MetaTypeInformationImpl implements MetaTypeInformation
{
<<<<<<< HEAD

    private final Bundle bundle;

    private final Set pids;

    private final Set factoryPids;

=======
    private final Bundle bundle;
    private final Set pids;
    private final Set factoryPids;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    private final Map metaTypeProviders;

    private Set locales;

<<<<<<< HEAD

    protected MetaTypeInformationImpl( Bundle bundle )
=======
    protected MetaTypeInformationImpl(Bundle bundle)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        this.bundle = bundle;
        this.pids = new HashSet();
        this.factoryPids = new HashSet();
        this.metaTypeProviders = new HashMap();
    }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    void dispose()
    {
        this.pids.clear();
        this.factoryPids.clear();
        this.locales = null;
        this.metaTypeProviders.clear();
    }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeInformation#getBundle()
     */
    public Bundle getBundle()
    {
        return this.bundle;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeInformation#getFactoryPids()
     */
    public String[] getFactoryPids()
    {
<<<<<<< HEAD
        return ( String[] ) this.factoryPids.toArray( new String[this.factoryPids.size()] );
=======
        return (String[]) this.factoryPids.toArray(new String[this.factoryPids.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeInformation#getPids()
     */
    public String[] getPids()
    {
<<<<<<< HEAD
        return ( String[] ) this.pids.toArray( new String[this.pids.size()] );
=======
        return (String[]) this.pids.toArray(new String[this.pids.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    public String[] getLocales()
    {
<<<<<<< HEAD
        if ( this.locales == null )
        {
            synchronized ( this )
            {
                Set newLocales = new HashSet();
                for ( Iterator mi = this.metaTypeProviders.values().iterator(); mi.hasNext(); )
                {
                    MetaTypeProvider mtp = ( MetaTypeProvider ) mi.next();
                    this.addValues( newLocales, mtp.getLocales() );
=======
        if (this.locales == null)
        {
            synchronized (this)
            {
                Set newLocales = new HashSet();
                for (Iterator mi = this.metaTypeProviders.values().iterator(); mi.hasNext();)
                {
                    MetaTypeProvider mtp = (MetaTypeProvider) mi.next();
                    this.addValues(newLocales, mtp.getLocales());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
                this.locales = newLocales;
            }
        }

        return ( String[] ) this.locales.toArray( new String[this.locales.size()] );
    }


    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String,
     *      java.lang.String)
     */
<<<<<<< HEAD
    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {

        if ( id == null || id.length() == 0 )
        {
            throw new IllegalArgumentException( "ObjectClassDefinition ID must not be null or empty" );
        }

        MetaTypeProvider mtp = ( MetaTypeProvider ) this.metaTypeProviders.get( id );
        if ( mtp == null )
        {
            throw new IllegalArgumentException( "No ObjectClassDefinition for id=" + id );
        }

        ObjectClassDefinition ocd = mtp.getObjectClassDefinition( id, locale );
        if ( ocd == null )
        {
            throw new IllegalArgumentException( "No localized ObjectClassDefinition for id=" + id );
=======
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
    {

        if (id == null || id.length() == 0)
        {
            throw new IllegalArgumentException("ObjectClassDefinition ID must not be null or empty");
        }

        MetaTypeProvider mtp = (MetaTypeProvider) this.metaTypeProviders.get(id);
        if (mtp == null)
        {
            throw new IllegalArgumentException("No ObjectClassDefinition for id=" + id);
        }

        ObjectClassDefinition ocd = mtp.getObjectClassDefinition(id, locale);
        if (ocd == null)
        {
            throw new IllegalArgumentException("No localized ObjectClassDefinition for id=" + id);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        return ocd;
    }


    // ---------- internal support for metadata -------------------------------

    Designate getDesignate( String pid )
    {
        Object mto = this.metaTypeProviders.get( pid );
        if ( mto instanceof DefaultMetaTypeProvider )
        {
            return ( ( DefaultMetaTypeProvider ) mto ).getDesignate( pid );
        }

        return null;
    }


    // ---------- setters to fill the values -----------------------------------

<<<<<<< HEAD
    protected void addMetaData( MetaData md )
    {
        if ( md.getDesignates() != null )
=======
    protected void addMetaData(MetaData md)
    {
        if (md.getDesignates() != null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            // meta type provide to register by PID
            DefaultMetaTypeProvider dmtp = new DefaultMetaTypeProvider( this.bundle, md );

<<<<<<< HEAD
            Iterator designates = md.getDesignates().values().iterator();
            while ( designates.hasNext() )
            {
                Designate designate = ( Designate ) designates.next();

                // get the OCD reference, ignore the designate if none
                DesignateObject object = designate.getObject();
                String ocdRef = ( object == null ) ? null : object.getOcdRef();
                if ( ocdRef == null )
=======
            Iterator designates = md.getDesignates().iterator();
            while (designates.hasNext())
            {
                Designate designate = (Designate) designates.next();

                // get the OCD reference, ignore the designate if none
                DesignateObject object = designate.getObject();
                String ocdRef = (object == null) ? null : object.getOcdRef();
                if (ocdRef == null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                {
                    continue;
                }

                // get ocd for the reference, ignore designate if none
<<<<<<< HEAD
                OCD ocd = ( OCD ) md.getObjectClassDefinitions().get( ocdRef );
                if ( ocd == null )
=======
                final Map map = md.getObjectClassDefinitions();
                OCD ocd = (OCD) (map == null ? null : map.get(ocdRef));
                if (ocd == null)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                {
                    continue;
                }

                // gather pids and factory pids and register provider
<<<<<<< HEAD
                if ( designate.getFactoryPid() != null )
                {
                    this.factoryPids.add( designate.getFactoryPid() );
                    this.addMetaTypeProvider( designate.getFactoryPid(), dmtp );
                }
                else
                {
                    this.pids.add( designate.getPid() );
                    this.addMetaTypeProvider( designate.getPid(), dmtp );
=======
                if (designate.getFactoryPid() != null)
                {
                    this.factoryPids.add(designate.getFactoryPid());
                    this.addMetaTypeProvider(designate.getFactoryPid(), dmtp);
                }
                else
                {
                    this.pids.add(designate.getPid());
                    this.addMetaTypeProvider(designate.getPid(), dmtp);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
            }
        }
    }

<<<<<<< HEAD

    protected void addMetaTypeProvider( String key, MetaTypeProvider mtp )
    {
        if ( key != null && mtp != null )
        {
            this.metaTypeProviders.put( key, mtp );
=======
    protected void addMetaTypeProvider(String key, MetaTypeProvider mtp)
    {
        if (key != null && mtp != null)
        {
            this.metaTypeProviders.put(key, mtp);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            this.locales = null;
        }
    }

<<<<<<< HEAD

    protected MetaTypeProvider removeMetaTypeProvider( String key )
    {
        if ( key != null )
        {
            this.locales = null;
            return ( MetaTypeProvider ) this.metaTypeProviders.remove( key );
=======
    protected MetaTypeProvider removeMetaTypeProvider(String key)
    {
        if (key != null)
        {
            this.locales = null;
            return (MetaTypeProvider) this.metaTypeProviders.remove(key);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        return null;
    }

<<<<<<< HEAD

    protected void addSingletonMetaTypeProvider( String[] pids, MetaTypeProvider mtp )
    {
        this.addValues( this.pids, pids );
        for ( int i = 0; i < pids.length; i++ )
        {
            addMetaTypeProvider( pids[i], mtp );
        }
    }


    protected void addFactoryMetaTypeProvider( String[] factoryPids, MetaTypeProvider mtp )
    {
        this.addValues( this.factoryPids, factoryPids );
        for ( int i = 0; i < factoryPids.length; i++ )
        {
            addMetaTypeProvider( factoryPids[i], mtp );
        }
    }


    protected boolean removeSingletonMetaTypeProvider( String[] pids )
    {
        boolean wasRegistered = false;
        for ( int i = 0; i < pids.length; i++ )
        {
            wasRegistered |= ( removeMetaTypeProvider( pids[i] ) != null );
            this.pids.remove( pids[i] );
=======
    protected void addSingletonMetaTypeProvider(String[] pids, MetaTypeProvider mtp)
    {
        this.addValues(this.pids, pids);
        for (int i = 0; i < pids.length; i++)
        {
            addMetaTypeProvider(pids[i], mtp);
        }
    }

    protected void addFactoryMetaTypeProvider(String[] factoryPids, MetaTypeProvider mtp)
    {
        this.addValues(this.factoryPids, factoryPids);
        for (int i = 0; i < factoryPids.length; i++)
        {
            addMetaTypeProvider(factoryPids[i], mtp);
        }
    }

    protected boolean removeSingletonMetaTypeProvider(String[] pids)
    {
        boolean wasRegistered = false;
        for (int i = 0; i < pids.length; i++)
        {
            wasRegistered |= (removeMetaTypeProvider(pids[i]) != null);
            this.pids.remove(pids[i]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return wasRegistered;
    }

<<<<<<< HEAD

    protected boolean removeFactoryMetaTypeProvider( String[] factoryPids )
    {
        boolean wasRegistered = false;
        for ( int i = 0; i < factoryPids.length; i++ )
        {
            wasRegistered |= ( removeMetaTypeProvider( factoryPids[i] ) != null );
            this.factoryPids.remove( factoryPids[i] );
=======
    protected boolean removeFactoryMetaTypeProvider(String[] factoryPids)
    {
        boolean wasRegistered = false;
        for (int i = 0; i < factoryPids.length; i++)
        {
            wasRegistered |= (removeMetaTypeProvider(factoryPids[i]) != null);
            this.factoryPids.remove(factoryPids[i]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return wasRegistered;
    }

<<<<<<< HEAD

    protected void addService( String[] pids, boolean isSingleton, boolean isFactory, final MetaTypeProvider mtp )
    {
    }


    protected void removeService( String[] pids, boolean isSingleton, boolean isFactory )
    {
    }


    private void addValues( Collection dest, Object[] values )
    {
        if ( values != null && values.length > 0 )
        {
            dest.addAll( Arrays.asList( values ) );
=======
    protected void addService(String[] pids, boolean isSingleton, boolean isFactory, final MetaTypeProvider mtp)
    {
    }

    protected void removeService(String[] pids, boolean isSingleton, boolean isFactory)
    {
    }

    private void addValues(Collection dest, Object[] values)
    {
        if (values != null && values.length > 0)
        {
            dest.addAll(Arrays.asList(values));
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }
}
