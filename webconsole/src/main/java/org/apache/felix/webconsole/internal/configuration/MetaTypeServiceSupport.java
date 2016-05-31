/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.configuration;


import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>ConfigManagerBase</code> is the base class for the
 * ConfigurationAdmin support in the web console. It provides various helper
 * methods mostly with respect to using the MetaTypeService to access
 * configuration descriptions.
 */
class MetaTypeServiceSupport extends MetaTypeSupport
{

    private final BundleContext bundleContext;

    private final MetaTypeService service;

    /**
     *
     * @param bundleContext
     * @param service
     *
     * @throws ClassCastException if {@code service} is not a MetaTypeService instances
     */
    MetaTypeServiceSupport( final BundleContext bundleContext, final Object service )
    {
        super();
        this.bundleContext = bundleContext;
        this.service = ( MetaTypeService ) service;
    }

    public BundleContext getBundleContext()
    {
        return bundleContext;
    }

    public MetaTypeService getMetaTypeService()
    {
        return service;
    }


    /**
     * Returns a map of PIDs and providing bundles of MetaType information. The
     * map is indexed by PID and the value of each entry is the bundle providing
     * the MetaType information for that PID.
     *
     * @param locale The name of the locale to get the meta data for.
     * @return see the method description
     */
    Map getPidObjectClasses( final String locale )
    {
        return getObjectClassDefinitions( PID_GETTER, locale );
    }


    /**
     * Returns a map of factory PIDs and providing bundles of MetaType
     * information. The map is indexed by factory PID and the value of each
     * entry is the bundle providing the MetaType information for that factory
     * PID.
     *
     * @param locale The name of the locale to get the meta data for.
     * @return see the method description
     */
    Map getFactoryPidObjectClasses( final String locale )
    {
        return getObjectClassDefinitions( FACTORY_PID_GETTER, locale );
    }


    /**
     * Returns the <code>ObjectClassDefinition</code> objects for the IDs
     * returned by the <code>idGetter</code>. Depending on the
     * <code>idGetter</code> implementation this will be for factory PIDs or
     * plain PIDs.
     *
     * @param idGetter The {@link IdGetter} used to get the list of factory PIDs
     *          or PIDs from <code>MetaTypeInformation</code> objects.
     * @param locale The name of the locale to get the object class definitions
     *          for.
     * @return Map of <code>ObjectClassDefinition</code> objects indexed by the
     *      PID (or factory PID) to which they pertain
     */
    private Map getObjectClassDefinitions( final IdGetter idGetter, final String locale )
    {
        final Map objectClassesDefinitions = new HashMap();
        final MetaTypeService mts = this.getMetaTypeService();
        if ( mts != null )
        {
            final Bundle[] bundles = this.getBundleContext().getBundles();
            for ( int i = 0; i < bundles.length; i++ )
            {
                final MetaTypeInformation mti = mts.getMetaTypeInformation( bundles[i] );
                if ( mti != null )
                {
                    final String[] idList = idGetter.getIds( mti );
                    for ( int j = 0; idList != null && j < idList.length; j++ )
                    {
                        // After getting the list of PIDs, a configuration  might be
                        // removed. So the getObjectClassDefinition will throw
                        // an exception, and this will prevent ALL configuration from
                        // being displayed. By catching it, the configurations will be
                        // visible
                        ObjectClassDefinition ocd = null;
                        try
                        {
                            ocd = mti.getObjectClassDefinition( idList[j], locale );
                        }
                        catch ( IllegalArgumentException ignore )
                        {
                            // ignore - just don't show this configuration
                        }
                        if ( ocd != null )
                        {
                            objectClassesDefinitions.put( idList[j], ocd );
                        }
                    }
                }
            }
        }
        return objectClassesDefinitions;
    }


    ObjectClassDefinition getObjectClassDefinition( Configuration config, String locale )
    {
        // if the configuration is bound, try to get the object class
        // definition from the bundle installed from the given location
        if ( config.getBundleLocation() != null )
        {
            Bundle bundle = getBundle( this.getBundleContext(), config.getBundleLocation() );
            if ( bundle != null )
            {
                String id = config.getFactoryPid();
                if ( null == id )
                {
                    id = config.getPid();
                }
                return getObjectClassDefinition( bundle, id, locale );
            }
        }

        // get here if the configuration is not bound or if no
        // bundle with the bound location is installed. We search
        // all bundles for a matching [factory] PID
        // if the configuration is a factory one, use the factory PID
        if ( config.getFactoryPid() != null )
        {
            return this.getObjectClassDefinition( config.getFactoryPid(), locale );
        }

        // otherwise use the configuration PID
        return this.getObjectClassDefinition( config.getPid(), locale );
    }


    ObjectClassDefinition getObjectClassDefinition( Bundle bundle, String pid, String locale )
    {
        if ( bundle != null )
        {
            MetaTypeService mts = this.getMetaTypeService();
            if ( mts != null )
            {
                MetaTypeInformation mti = mts.getMetaTypeInformation( bundle );
                if ( mti != null )
                {
                    // see #getObjectClasses( final IdGetter idGetter, final String locale )
                    try
                    {
                        return mti.getObjectClassDefinition( pid, locale );
                    }
                    catch ( IllegalArgumentException e )
                    {
                        // MetaTypeProvider.getObjectClassDefinition might throw illegal
                        // argument exception. So we must catch it here, otherwise the
                        // other configurations will not be shown
                        // See https://issues.apache.org/jira/browse/FELIX-2390
                        // https://issues.apache.org/jira/browse/FELIX-3694
                    }
                }
            }
        }

        // fallback to nothing found
        return null;
    }


    ObjectClassDefinition getObjectClassDefinition( String pid, String locale )
    {
        Bundle[] bundles = this.getBundleContext().getBundles();
        for ( int i = 0; i < bundles.length; i++ )
        {
            try
            {
                ObjectClassDefinition ocd = this.getObjectClassDefinition( bundles[i], pid, locale );
                if ( ocd != null )
                {
                    return ocd;
                }
            }
            catch ( IllegalArgumentException iae )
            {
                // don't care
            }
        }
        return null;
    }


    Map getAttributeDefinitionMap( Configuration config, String locale )
    {
        Map adMap = new HashMap();
        ObjectClassDefinition ocd = this.getObjectClassDefinition( config, locale );
        if ( ocd != null )
        {
            AttributeDefinition[] ad = ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
            if ( ad != null )
            {
                for ( int i = 0; i < ad.length; i++ )
                {
                    adMap.put( ad[i].getID(), new MetatypePropertyDescriptor( ad[i], false ) );
                }
            }
        }
        return adMap;
    }


    void mergeWithMetaType( Dictionary props, ObjectClassDefinition ocd, JSONWriter json, Set ignoreAttrIds ) throws JSONException
    {
        json.key( "title" ).value( ocd.getName() ); //$NON-NLS-1$

        if ( ocd.getDescription() != null )
        {
            json.key( "description" ).value( ocd.getDescription() ); //$NON-NLS-1$
        }

        AttributeDefinition[] ad = ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
        AttributeDefinition[] optionalArray = ocd.getAttributeDefinitions( ObjectClassDefinition.OPTIONAL );
        List/*<AttributeDefinition>*/ optional = optionalArray == null ? Collections.EMPTY_LIST : Arrays.asList( optionalArray );
        final Set metatypeAttributes = new HashSet(ignoreAttrIds);
        if ( ad != null )
        {
            json.key( "properties" ).object(); //$NON-NLS-1$
            for ( int i = 0; i < ad.length; i++ )
            {
                final AttributeDefinition adi = ad[i];
                final String attrId = adi.getID();
                if (!ignoreAttrIds.contains(attrId)) {
                    json.key( attrId );
                    boolean isOptional = optional.contains( adi );
                    attributeToJson( json, new MetatypePropertyDescriptor( adi, isOptional ), props.get( attrId ) );
                }
                metatypeAttributes.add( attrId );
            }
            json.endObject();
        }
        final StringBuffer sb = new StringBuffer();
        final Enumeration e = props.keys();
        while ( e.hasMoreElements() )
        {
            String key = (String)e.nextElement();
            if ( !metatypeAttributes.contains(key) ) {
                if ( sb.length() > 0 )
                {
                    sb.append(',');
                }
                sb.append(key);
            }
        }
        if ( sb.length() > 0 )
        {
            json.key("additionalProperties").value(sb.toString());
        }
    }




    /**
     * The <code>IdGetter</code> interface is an internal helper to abstract
     * retrieving object class definitions from all bundles for either
     * pids or factory pids.
     *
     * @see #PID_GETTER
     * @see #FACTORY_PID_GETTER
     */
    private static interface IdGetter
    {
        String[] getIds( MetaTypeInformation metaTypeInformation );
    }

    /**
     * The implementation of the {@link IdGetter} interface returning the PIDs
     * listed in the meta type information.
     *
     * @see #getPidObjectClasses(String)
     */
    private static final IdGetter PID_GETTER = new IdGetter()
    {
        public String[] getIds( MetaTypeInformation metaTypeInformation )
        {
            return metaTypeInformation.getPids();
        }
    };

    /**
     * The implementation of the {@link IdGetter} interface returning the
     * factory PIDs listed in the meta type information.
     *
     * @see #getFactoryPidObjectClasses(String)
     */
    private static final IdGetter FACTORY_PID_GETTER = new IdGetter()
    {
        public String[] getIds( MetaTypeInformation metaTypeInformation )
        {
            return metaTypeInformation.getFactoryPids();
        }
    };

}
