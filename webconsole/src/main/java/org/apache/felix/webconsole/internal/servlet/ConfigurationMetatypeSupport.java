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
package org.apache.felix.webconsole.internal.servlet;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.felix.webconsole.internal.Util;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;


class ConfigurationMetatypeSupport extends ConfigurationSupport implements MetaTypeProvider
{
    private static final String[] CONF_PROPS = new String[]
        { OsgiManager.PROP_MANAGER_ROOT, OsgiManager.DEFAULT_MANAGER_ROOT, //
            OsgiManager.PROP_HTTP_SERVICE_SELECTOR, OsgiManager.DEFAULT_HTTP_SERVICE_SELECTOR, //
            OsgiManager.PROP_DEFAULT_RENDER, OsgiManager.DEFAULT_PAGE, //
            OsgiManager.PROP_REALM, OsgiManager.DEFAULT_REALM, //
            OsgiManager.PROP_USER_NAME, OsgiManager.DEFAULT_USER_NAME, //
            OsgiManager.PROP_PASSWORD, OsgiManager.DEFAULT_PASSWORD, //
            OsgiManager.PROP_CATEGORY, OsgiManager.DEFAULT_CATEGORY, //
            OsgiManager.PROP_LOCALE, "", //$NON-NLS-1$
        };

    private final Object ocdLock = new Object();
    private String ocdLocale;
    private ObjectClassDefinition ocd;


    ConfigurationMetatypeSupport( OsgiManager osgiManager )
    {
        super( osgiManager );
    }


    //---------- MetaTypeProvider

    public String[] getLocales()
    {
        // there is no locale support here
        return null;
    }


    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {
        if ( !osgiManager.getConfigurationPid().equals( id ) )
        {
            return null;
        }

        if ( locale == null )
            locale = Locale.ENGLISH.getLanguage();

        // check if OCD is already initialized and it's locale is the same as the requested one
        synchronized ( ocdLock )
        {
            if ( ocd != null && ocdLocale != null && ocdLocale.equals( locale ) )
            {
                return ocd;
            }
        }

        ObjectClassDefinition xocd = null;
        final Locale localeObj = Util.parseLocaleString( locale );
        final ResourceBundle rb = osgiManager.resourceBundleManager.getResourceBundle( osgiManager.getBundleContext()
            .getBundle(), localeObj );
        final Map defaultConfig = osgiManager.getDefaultConfiguration();

        // simple configuration properties
        final ArrayList adList = new ArrayList();
        for ( int i = 0; i < CONF_PROPS.length; i++ )
        {
            final String key = CONF_PROPS[i++];
            final String defaultValue = ConfigurationUtil.getProperty( defaultConfig, key, CONF_PROPS[i] );
            final String name = getString( rb, "metadata." + key + ".name", key ); //$NON-NLS-1$ //$NON-NLS-2$
            final String descr = getString( rb, "metadata." + key + ".description", key ); //$NON-NLS-1$ //$NON-NLS-2$
            adList.add( new AttributeDefinitionImpl( key, name, descr, defaultValue ) );
        }

        // log level is select - so no simple default value; requires localized option labels
        adList.add( new AttributeDefinitionImpl( OsgiManager.PROP_LOG_LEVEL, getString( rb,
            "metadata.loglevel.name", OsgiManager.PROP_LOG_LEVEL ), //$NON-NLS-1$
            getString( rb, "metadata.loglevel.description", OsgiManager.PROP_LOG_LEVEL ), //$NON-NLS-1$
            AttributeDefinition.INTEGER, // type
            new String[]
                { String.valueOf( ConfigurationUtil.getProperty( defaultConfig, OsgiManager.PROP_LOG_LEVEL,
                    OsgiManager.DEFAULT_LOG_LEVEL ) ) }, // default values
            0, // cardinality
            new String[]
                { // option labels
            getString( rb, "log.level.debug", "Debug" ), //$NON-NLS-1$ //$NON-NLS-2$
                getString( rb, "log.level.info", "Information" ), //$NON-NLS-1$ //$NON-NLS-2$
                getString( rb, "log.level.warn", "Warn" ), //$NON-NLS-1$ //$NON-NLS-2$
                getString( rb, "log.level.error", "Error" ), //$NON-NLS-1$ //$NON-NLS-2$
            }, new String[]
            { "4", "3", "2", "1" } ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        // list plugins - requires localized plugin titles
        final TreeMap namesByClassName = new TreeMap();
        final String[] defaultPluginsClasses = OsgiManager.PLUGIN_MAP;
        for ( int i = 0; i < defaultPluginsClasses.length; i++ )
        {
            final String clazz = defaultPluginsClasses[i++];
            final String label = defaultPluginsClasses[i];
            final String name = getString( rb, label + ".pluginTitle", label ); //$NON-NLS-1$
            namesByClassName.put( clazz, name );
        }
        final String[] classes = ( String[] ) namesByClassName.keySet().toArray( new String[namesByClassName.size()] );
        final String[] names = ( String[] ) namesByClassName.values().toArray( new String[namesByClassName.size()] );

        adList.add( new AttributeDefinitionImpl( OsgiManager.PROP_ENABLED_PLUGINS, getString( rb,
            "metadata.plugins.name", OsgiManager.PROP_ENABLED_PLUGINS ), //$NON-NLS-1$
            getString( rb, "metadata.plugins.description", OsgiManager.PROP_ENABLED_PLUGINS ), //$NON-NLS-1$
            AttributeDefinition.STRING, classes, Integer.MIN_VALUE, names, classes ) );

        xocd = new ObjectClassDefinition()
        {

            private final AttributeDefinition[] attrs = ( AttributeDefinition[] ) adList
                .toArray( new AttributeDefinition[adList.size()] );


            public String getName()
            {
                return getString( rb, "metadata.name", "Apache Felix OSGi Management Console" ); //$NON-NLS-1$ //$NON-NLS-2$
            }


            public InputStream getIcon( int arg0 )
            {
                return null;
            }


            public String getID()
            {
                return osgiManager.getConfigurationPid();
            }


            public String getDescription()
            {
                return getString( rb,
                    "metadata.description", "Configuration of the Apache Felix OSGi Management Console." ); //$NON-NLS-1$ //$NON-NLS-2$
            }


            public AttributeDefinition[] getAttributeDefinitions( int filter )
            {
                return ( filter == OPTIONAL ) ? null : attrs;
            }
        };

        synchronized ( ocdLock )
        {
            this.ocd = xocd;
            this.ocdLocale = locale;
        }

        return ocd;
    }


    private static final String getString( ResourceBundle rb, String key, String def )
    {
        try
        {
            return rb.getString( key );
        }
        catch ( Throwable t )
        {
            return def;
        }
    }

    private static class AttributeDefinitionImpl implements AttributeDefinition
    {

        private final String id;
        private final String name;
        private final String description;
        private final int type;
        private final String[] defaultValues;
        private final int cardinality;
        private final String[] optionLabels;
        private final String[] optionValues;


        AttributeDefinitionImpl( final String id, final String name, final String description, final String defaultValue )
        {
            this( id, name, description, STRING, new String[]
                { defaultValue }, 0, null, null );
        }


        AttributeDefinitionImpl( final String id, final String name, final String description, final int type,
            final String[] defaultValues, final int cardinality, final String[] optionLabels,
            final String[] optionValues )
        {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.defaultValues = defaultValues;
            this.cardinality = cardinality;
            this.optionLabels = optionLabels;
            this.optionValues = optionValues;
        }


        public int getCardinality()
        {
            return cardinality;
        }


        public String[] getDefaultValue()
        {
            return defaultValues;
        }


        public String getDescription()
        {
            return description;
        }


        public String getID()
        {
            return id;
        }


        public String getName()
        {
            return name;
        }


        public String[] getOptionLabels()
        {
            return optionLabels;
        }


        public String[] getOptionValues()
        {
            return optionValues;
        }


        public int getType()
        {
            return type;
        }


        public String validate( String arg0 )
        {
            return null;
        }
    }

}