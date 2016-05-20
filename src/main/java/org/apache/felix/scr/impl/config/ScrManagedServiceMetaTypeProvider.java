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
package org.apache.felix.scr.impl.config;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * The <code>ScrManagedServiceMetaTypeProvider</code> receives the Declarative
 * Services Runtime configuration (by extending the {@link ScrManagedService}
 * class but also provides a MetaType Service ObjectClassDefinition.
 * <p>
 * This class is instantiated in a ServiceFactory manner by the
 * {@link ScrManagedServiceServiceFactory} when the Configuration Admin service
 * implementation and API is available and if the Metatype Service API is also
 * available.
 * <p>
 * Requires OSGi Metatype Service API available
 *
 * @see ScrManagedServiceServiceFactory
 */
class ScrManagedServiceMetaTypeProvider extends ScrManagedService
    implements MetaTypeProvider
{

    static ScrManagedService create(final ScrConfigurationImpl scrConfiguration)
    {
        return new ScrManagedServiceMetaTypeProvider(scrConfiguration);
    }

    private ScrManagedServiceMetaTypeProvider(final ScrConfigurationImpl scrConfiguration)
    {
        super(scrConfiguration);
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    public String[] getLocales()
    {
        return null;
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {
        if ( !ScrConfiguration.PID.equals( id ) )
        {
            return null;
        }

        final ArrayList<AttributeDefinition> adList = new ArrayList<AttributeDefinition>();

        adList.add(new AttributeDefinitionImpl(ScrConfiguration.PROP_LOGLEVEL, "SCR Log Level",
            "Allows limiting the amount of logging information sent to the OSGi LogService."
                + " Supported values are DEBUG, INFO, WARN, and ERROR. Default is ERROR.", AttributeDefinition.INTEGER,
            new String[]
                { String.valueOf(this.getScrConfiguration().getLogLevel()) }, 0, new String[]
                { "Debug", "Information", "Warnings", "Error" }, new String[]
                { "4", "3", "2", "1" }));

        adList
        .add(new AttributeDefinitionImpl(
            ScrConfiguration.PROP_FACTORY_ENABLED,
            "Extended Factory Components",
            "Whether or not to enable the support for creating Factory Component instances based on factory configuration."
                + " This is an Apache Felix SCR specific extension, explicitly not supported by the Declarative Services "
                + "specification. Reliance on this feature prevent the component from being used with other Declarative "
                + "Services implementations. The default value is false to disable this feature.", this
                .getScrConfiguration().isFactoryEnabled()));

        adList.add( new AttributeDefinitionImpl(
                ScrConfiguration.PROP_DELAYED_KEEP_INSTANCES,
                "Keep Component Instances",
                "Whether or not to keep instances of delayed components once they are not referred to any more. The "
                    + "Declarative Services specifications suggests that instances of delayed components are disposed off "
                    + "if there is not used any longer. Setting this flag causes the components to not be disposed off "
                    + "and thus prevent them from being constantly recreated if often used. Examples of such components "
                    + "may be EventHandler services. The default is to dispose of unused components.", this
                    .getScrConfiguration().keepInstances() ) );

        adList.add( new AttributeDefinitionImpl(
                ScrConfiguration.PROP_LOCK_TIMEOUT,
                "Lock timeout milliseconds",
                "How long a lock is held before releasing due to suspected deadlock",
                AttributeDefinition.LONG,
                new String[] { String.valueOf(this.getScrConfiguration().lockTimeout())},
                0, null, null) );

        adList.add( new AttributeDefinitionImpl(
                ScrConfiguration.PROP_STOP_TIMEOUT,
                "Stop timeout milliseconds",
                "How long stopping a bundle is waited for before continuing due to suspected deadlock",
                AttributeDefinition.LONG,
                new String[] { String.valueOf(this.getScrConfiguration().stopTimeout())},
                0, null, null) );

        adList.add( new AttributeDefinitionImpl(
                ScrConfiguration.PROP_GLOBAL_EXTENDER,
                "Global Extender",
                "Whether to extend all bundles whether or not visible to this bundle.",
                false ) );

        return new ObjectClassDefinition()
        {

            private final AttributeDefinition[] attrs = adList
                .toArray(new AttributeDefinition[adList.size()]);

            public String getName()
            {
                return "Apache Felix Declarative Service Implementation";
            }

            public InputStream getIcon(int arg0)
            {
                return null;
            }

            public String getID()
            {
                return ScrConfiguration.PID;
            }

            public String getDescription()
            {
                return "Configuration for the Apache Felix Declarative Services Implementation."
                    + " This configuration overwrites configuration defined in framework properties of the same names.";
            }

            public AttributeDefinition[] getAttributeDefinitions(int filter)
            {
                return (filter == OPTIONAL) ? null : attrs;
            }
        };
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


        AttributeDefinitionImpl( final String id, final String name, final String description, final boolean defaultValue )
        {
            this( id, name, description, BOOLEAN, new String[]
                { String.valueOf(defaultValue) }, 0, null, null );
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
