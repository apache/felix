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
package org.apache.felix.eventadmin.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * The optional meta type provider for the event admin config.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MetaTypeProviderImpl
    implements MetaTypeProvider, ManagedService
{
    private final int m_threadPoolSize;
    private final int m_timeout;
    private final boolean m_requireTopic;
    private final String[] m_ignoreTimeout;
    private final String[] m_ignoreTopic;
    private final double m_asyncThreadPoolRatio;

    private final ManagedService m_delegatee;

    public MetaTypeProviderImpl(final ManagedService delegatee,
            final int threadPoolSize,
            final int timeout, final boolean requireTopic,
            final String[] ignoreTimeout,
            final String[] ignoreTopic,
            final double asyncThreadPoolRatio)
    {
        m_threadPoolSize = threadPoolSize;
        m_timeout = timeout;
        m_requireTopic = requireTopic;
        m_delegatee = delegatee;
        m_ignoreTimeout = ignoreTimeout;
        m_ignoreTopic = ignoreTopic;
        m_asyncThreadPoolRatio = asyncThreadPoolRatio;
    }

    private ObjectClassDefinition ocd;

    /**
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException
    {
        m_delegatee.updated(properties);
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    @Override
    public String[] getLocales()
    {
        return null;
    }

    /**
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    @Override
    public ObjectClassDefinition getObjectClassDefinition( String id, String locale )
    {
        if ( !Configuration.PID.equals( id ) )
        {
            return null;
        }

        if ( ocd == null )
        {
            final ArrayList<AttributeDefinition> adList = new ArrayList<AttributeDefinition>();

            adList.add( new AttributeDefinitionImpl( Configuration.PROP_THREAD_POOL_SIZE, "Thread Pool Size",
                "The size of the thread pool used for event delivery. The default value is 20. " +
                "Increase in case of a large amount of events. A value of " +
                "less then 2 triggers the default value. If the pool is exhausted, event delivery " +
                "is blocked until a thread becomes available from the pool. Each event is delivered " +
                "in a thread from the pool unless the ignore timeouts is configured for the receiving event handler.",
                m_threadPoolSize ) );
            adList.add( new AttributeDefinitionImpl( Configuration.PROP_ASYNC_TO_SYNC_THREAD_RATIO, "Async/sync Thread Pool Ratio",
                    "The ratio of asynchronous to synchronous threads in the internal thread" +
                    " pool. Ratio must be positive and may be adjusted to represent the " +
                    "distribution of post to send operations.  Applications with higher number " +
                    "of post operations should have a higher ratio.",
                    m_asyncThreadPoolRatio));

            adList.add( new AttributeDefinitionImpl( Configuration.PROP_TIMEOUT, "Timeout",
                    "The black-listing timeout in milliseconds. The default value is 5000. Increase or decrease " +
                    "at own discretion. A value of less then 100 turns timeouts off. Any other value is the time " +
                    "in milliseconds granted to each event handler before it gets blacklisted",
                    m_timeout ) );

            adList.add( new AttributeDefinitionImpl( Configuration.PROP_REQUIRE_TOPIC, "Require Topic",
                    "Are event handlers required to be registered with a topic? " +
                    "This is enabled by default. The specification says that event handlers " +
                    "must register with a list of topics they are interested in. Disabling this setting " +
                    "will enable that handlers without a topic are receiving all events " +
                    "(i.e., they are treated the same as with a topic=*).",
                    m_requireTopic ) );
            adList.add( new AttributeDefinitionImpl( Configuration.PROP_IGNORE_TIMEOUT, "Ignore Timeouts",
                    "Configure event handlers to be called without a timeout. If a timeout is configured by default " +
                    "all event handlers are called using the timeout. For performance optimization it is possible to " +
                    "configure event handlers where the timeout handling is not used - this reduces the thread usage " +
                    "from the thread pools as the timout handling requires an additional thread to call the event " +
                    "handler. However, the application should work without this configuration property. It is a " +
                    "pure optimization! The value is a list of strings. If a string ends with a dot, " +
                    "all handlers in exactly this package are ignored. If the string ends with a star, " +
                    "all handlers in this package and all subpackages are ignored. If the string neither " +
                    "ends with a dot nor with a star, this is assumed to define an exact class name.",
                    AttributeDefinition.STRING, m_ignoreTimeout, Integer.MAX_VALUE, null, null));
            adList.add( new AttributeDefinitionImpl( Configuration.PROP_IGNORE_TOPIC, "Ignore Topics",
                    "For performance optimization it is possible to configure topics which are ignored " +
                    "by the event admin implementation. In this case, a event is not delivered to " +
                    "registered event handlers. The value is a list of strings (separated by comma). " +
                    "If a single value ends with a dot, all topics in exactly this package are ignored. " +
                    "If a single value ends with a star, all topics in this package and all sub packages " +
                    "are ignored. If a single value neither ends with a dot nor with a start, this is assumed " +
                    "to define an exact topic. A single star can be used to disable delivery completely.",
                    AttributeDefinition.STRING, m_ignoreTopic, Integer.MAX_VALUE, null, null));
            ocd = new ObjectClassDefinition()
            {

                private final AttributeDefinition[] attrs = adList
                    .toArray( new AttributeDefinition[adList.size()] );


                @Override
                public String getName()
                {
                    return "Apache Felix Event Admin Implementation";
                }


                @Override
                public InputStream getIcon( int arg0 )
                {
                    return null;
                }


                @Override
                public String getID()
                {
                    return Configuration.PID;
                }


                @Override
                public String getDescription()
                {
                    return "Configuration for the Apache Felix Event Admin Implementation." +
                           " This configuration overwrites configuration defined in framework properties of the same names.";
                }


                @Override
                public AttributeDefinition[] getAttributeDefinitions( int filter )
                {
                    return ( filter == OPTIONAL ) ? null : attrs;
                }
            };
        }

        return ocd;
    }

    class AttributeDefinitionImpl implements AttributeDefinition
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

        AttributeDefinitionImpl( final String id, final String name, final String description, final int defaultValue )
        {
            this( id, name, description, INTEGER, new String[]
                { String.valueOf(defaultValue) }, 0, null, null );
        }

        AttributeDefinitionImpl( final String id, final String name, final String description, final double defaultValue )
        {
            this( id, name, description, DOUBLE, new String[]
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


        @Override
        public int getCardinality()
        {
            return cardinality;
        }


        @Override
        public String[] getDefaultValue()
        {
            return defaultValues;
        }


        @Override
        public String getDescription()
        {
            return description;
        }


        @Override
        public String getID()
        {
            return id;
        }


        @Override
        public String getName()
        {
            return name;
        }


        @Override
        public String[] getOptionLabels()
        {
            return optionLabels;
        }


        @Override
        public String[] getOptionValues()
        {
            return optionValues;
        }


        @Override
        public int getType()
        {
            return type;
        }


        @Override
        public String validate( String arg0 )
        {
            return null;
        }
    }
}
