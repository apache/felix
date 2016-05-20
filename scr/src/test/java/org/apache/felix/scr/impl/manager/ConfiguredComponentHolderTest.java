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
package org.apache.felix.scr.impl.manager;


import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.inject.ComponentMethodsImpl;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.TargetedPID;


public class ConfiguredComponentHolderTest extends TestCase
{

    public void test_none()
    {
        // setup a holder
        final String name = "test.none";
        final ComponentMetadata cm = createComponentMetadata( name );
        final TestingConfiguredComponentHolder holder = new TestingConfiguredComponentHolder( cm );

        holder.enableComponents(false);
        // assert single component and no map
        final SingleComponentManager cmgr = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgr );
        assertEquals( "Expect no other component manager list", 1, getComponentManagers( holder ).size());
    }


    public void test_singleton()
    {
        // setup a holder
        final String name = "test.singleton";
        final ComponentMetadata cm = createComponentMetadata( name );
        final TestingConfiguredComponentHolder holder = new TestingConfiguredComponentHolder( cm );

        holder.enableComponents(false);
        // assert single component and no map
        final SingleComponentManager cmgr = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgr );
        assertEquals( "Expect no other component manager list", 1, getComponentManagers( holder ).size());

        // configure with the singleton configuration
        final Dictionary config = new Hashtable();
        config.put( "value", name );
        TargetedPID targetedPid = new TargetedPID(name);
		holder.configurationUpdated( targetedPid, null, config, 0 );

        // assert single component and no map
        final SingleComponentManager cmgrAfterConfig = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgrAfterConfig );
        assertEquals( "Expect no other component manager list", 1, getComponentManagers( holder ).size());

//        // assert configuration of single component
        final Map componentConfig = ( ( MockImmediateComponentManager ) cmgrAfterConfig ).getConfiguration();
        assertEquals( "Expect exact configuration set", config, componentConfig );

        // unconfigure singleton
        holder.configurationDeleted( targetedPid, null );

        // assert single component and no map
        final SingleComponentManager cmgrAfterUnconfig = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgrAfterUnconfig );
        assertEquals( "Expect no other component manager list", 1, getComponentManagers( holder ).size());

        // assert no configuration of single component
//TODO multipids fix, correct assertion        assertFalse( "Expect no configuration", cmgrAfterUnconfig.hasConfiguration() );
    }


    public void test_factory()
    {
        // setup a holder
        final String name = "test.factory";
        final ComponentMetadata cm = createComponentMetadata( name );
        final TestingConfiguredComponentHolder holder = new TestingConfiguredComponentHolder( cm );
        
        holder.enableComponents(false);

        // assert single component and no map
        final SingleComponentManager cmgr = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgr );
        assertEquals( "Expect no other component manager list", 1, getComponentManagers( holder ).size());

        // configure with configuration
        final String pid1 = "test.factory.0001";
        final Dictionary config1 = new Hashtable();
        config1.put( "value", pid1 );
        TargetedPID targetedFactoryPid = new TargetedPID(name);
		TargetedPID targetedPid1 = new TargetedPID(pid1);
		holder.configurationUpdated( targetedPid1, targetedFactoryPid, config1, 0 );

        // assert single component and single-entry map
        final SingleComponentManager cmgrAfterConfig = getSingleManager( holder );
        final List<SingleComponentManager> cmgrsAfterConfig = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrAfterConfig );
        assertNotNull( "Expect component manager list", cmgrsAfterConfig );
        assertEquals( "Expect one component manager in list", 1, cmgrsAfterConfig.size() );

        // add another configuration
        final String pid2 = "test.factory.0002";
        final Dictionary config2 = new Hashtable();
        config1.put( "value", pid2 );
        TargetedPID targetedPid2 = new TargetedPID(pid2);
		holder.configurationUpdated( targetedPid2, targetedFactoryPid, config2, 1 );

        final List<SingleComponentManager> cmgrsAfterConfig2 = getComponentManagers( holder );
        assertNotNull( "Expect component manager list", cmgrsAfterConfig2 );
        assertEquals( "Expect two component manager in list", 2, cmgrsAfterConfig2.size() );

        // remove second configuration
        holder.configurationDeleted( targetedPid2, targetedFactoryPid );

        final List<SingleComponentManager> cmgrsAfterUnConfig2 = getComponentManagers( holder );
        assertNotNull( "Expect component manager list", cmgrsAfterUnConfig2 );
//TODO Multipids fix correct assertion        assertEquals( "Expect one component manager in list", 1, cmgrsAfterUnConfig2.size() );

        // add second config again and remove first config -> replace singleton component
        holder.configurationUpdated( targetedPid2, targetedFactoryPid, config2, 2 );
        holder.configurationDeleted( targetedPid1, targetedFactoryPid );

        // assert single component and single-entry map
        final List<SingleComponentManager> cmgrsAfterConfigUnconfig = getComponentManagers( holder );
        assertNotNull( "Expect component manager list", cmgrsAfterConfigUnconfig );
//TODO Multipids fix correct assertion        assertEquals( "Expect one component manager in list", 1, cmgrsAfterConfigUnconfig.size() );

        // remove second configuration (leaving no configurations)
        holder.configurationDeleted( targetedPid2, targetedFactoryPid );

        // assert single component and single-entry map
        final List<SingleComponentManager> cmgrsAfterAllUnconfig = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrsAfterAllUnconfig );
//TODO Multipids fix correct assertion        assertEquals( "Expect no component manager list", 1, cmgrsAfterAllUnconfig.size() );

    }


    private static ComponentMetadata createComponentMetadata( String name )
    {
        final ComponentMetadata metadata = new ComponentMetadata( DSVersion.DS11 );
        metadata.setName( name );
        metadata.setImplementationClassName(Object.class.getName());
        metadata.validate(null);

        return metadata;
    }


    private static SingleComponentManager getSingleManager( ConfigurableComponentHolder holder )
    {
    	List<SingleComponentManager> managers = getComponentManagers(holder);
    	assertEquals(1, managers.size());
    	return managers.get(0);
    }


    private static List<SingleComponentManager> getComponentManagers( ConfigurableComponentHolder holder )
    {
    	return holder.getComponentManagers();
    }

    private static class TestingConfiguredComponentHolder extends ConfigurableComponentHolder
    {
        TestingConfiguredComponentHolder( ComponentMetadata metadata )
        {
            super( null, metadata );
        }


        protected SingleComponentManager createComponentManager(boolean factoryConfiguration)
        {
            return new MockImmediateComponentManager( this );
        }

        protected ComponentMethods createComponentMethods() {
            return new ComponentMethodsImpl();
        }
    }

    private static class MockImmediateComponentManager<S> extends SingleComponentManager<S>
    {

        private Map<String, Object> m_configuration;


        public MockImmediateComponentManager( ComponentContainer container )
        {
            super( container, new ComponentMethodsImpl() );
        }


        Map<String, Object> getConfiguration()
        {
            return m_configuration;
        }


        public boolean hasConfiguration()
        {
            return m_configuration != null;
        }


        public void reconfigure( Map<String, Object> configuration, boolean configurationDeleted )
        {
            this.m_configuration = configuration;
        }
    }
}
