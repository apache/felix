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
package org.apache.felix.cm.integration;


import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.osgi.framework.Constants.FRAMEWORK_SECURITY;
import static org.osgi.framework.Constants.FRAMEWORK_SECURITY_OSGI;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
import static org.osgi.service.url.URLConstants.URL_HANDLER_PROTOCOL;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.integration.helper.ManagedServiceFactoryTestActivator3;
import org.apache.felix.cm.integration.helper.NestedURLStreamHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.forked.ForkedTestContainerFactory;
import org.ops4j.pax.exam.junit.ExamFactory;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.url.URLStreamHandlerService;

import junit.framework.TestCase;


/**
 * This test case runs the main Configuration tests with security on to check that
 * nothing breaks.
 * 
 * Note that it must run as a {@link ForkedTestContainerFactory} because otherwise
 * we can't enable Java Security in the Framework
 */
@RunWith( JUnit4TestRunner.class )
@ExamFactory( ForkedTestContainerFactory.class )
@ExamReactorStrategy( AllConfinedStagedReactorFactory.class )
public class ConfigAdminSecurityTest extends ConfigurationBaseTest
{
    
    @Override
    protected Option[] additionalConfiguration() {
    	File policyFile = new File( "src/test/resources/all.policy" );
        return options(
                frameworkProperty( FRAMEWORK_STORAGE_CLEAN ).value( FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT ),
                frameworkProperty( FRAMEWORK_SECURITY ).value( FRAMEWORK_SECURITY_OSGI ),
                systemProperty( "java.security.policy" ).value( policyFile.getAbsolutePath() ),
                mavenBundle( "org.apache.felix", "org.apache.felix.framework.security", "2.6.1" )
        );
    }
    
    @Test
    public void test_secure_configuration() throws BundleException, IOException
    {
        final String factoryPid = "test_secure_configuration";
        bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator3.class );
        bundle.start();
        delay();

        final Configuration config = createFactoryConfiguration( factoryPid, null, true );
        final String pid = config.getPid();
        delay();

        // ==> configuration supplied to the service ms1
        final ManagedServiceFactoryTestActivator3 tester = ManagedServiceFactoryTestActivator3.INSTANCE;
        Dictionary<?, ?> props = tester.configs.get( pid );
        TestCase.assertNotNull( props );
        TestCase.assertEquals( pid, props.get( Constants.SERVICE_PID ) );
        TestCase.assertEquals( factoryPid, props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
        TestCase.assertNull( props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
        TestCase.assertEquals( PROP_NAME, props.get( PROP_NAME ) );
        TestCase.assertEquals( File.separator, props.get( "foo" ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );

        // delete
        config.delete();
        delay();

        // ==> update with null
        TestCase.assertNull( tester.configs.get( pid ) );
        TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
        TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }

    @Test
    public void test_secure_configuration_non_standard_install_url() throws Exception
    {
    	// Override the file URL handler
    	
    	@SuppressWarnings({ "serial", "unused" })
    	ServiceRegistration<URLStreamHandlerService> reg = bundleContext
    	.registerService( URLStreamHandlerService.class, new NestedURLStreamHandler(), 
    			new Hashtable<String, Object>() { {
    				put( URL_HANDLER_PROTOCOL, new String[] { "file" } );
    			} } );
    	
    	
    	// Run the actual test
    	
    	final String factoryPid = "test_secure_configuration_non_standard_install_url";
    	bundle = installBundle( factoryPid, ManagedServiceFactoryTestActivator3.class );
    	bundle.start();
    	delay();
    	
    	final Configuration config = createFactoryConfiguration( factoryPid, null, true );
    	final String pid = config.getPid();
    	delay();
    	
    	// ==> configuration supplied to the service ms1
    	final ManagedServiceFactoryTestActivator3 tester = ManagedServiceFactoryTestActivator3.INSTANCE;
    	Dictionary<?, ?> props = tester.configs.get( pid );
    	TestCase.assertNotNull( props );
    	TestCase.assertEquals( pid, props.get( Constants.SERVICE_PID ) );
    	TestCase.assertEquals( factoryPid, props.get( ConfigurationAdmin.SERVICE_FACTORYPID ) );
    	TestCase.assertNull( props.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION ) );
    	TestCase.assertEquals( PROP_NAME, props.get( PROP_NAME ) );
    	TestCase.assertEquals( File.separator, props.get( "foo" ) );
    	TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
    	TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
    	TestCase.assertEquals( 0, tester.numManagedServiceFactoryDeleteCalls );
    	
    	// delete
    	config.delete();
    	delay();
    	
    	// ==> update with null
    	TestCase.assertNull( tester.configs.get( pid ) );
    	TestCase.assertEquals( 0, tester.numManagedServiceUpdatedCalls );
    	TestCase.assertEquals( 1, tester.numManagedServiceFactoryUpdatedCalls );
    	TestCase.assertEquals( 1, tester.numManagedServiceFactoryDeleteCalls );
    }
}
