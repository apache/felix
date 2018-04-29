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
package org.apache.felix.scr.integration;

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
<<<<<<< HEAD
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
=======
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

<<<<<<< HEAD
import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ProbeBuilder;
=======
import org.apache.felix.scr.impl.ComponentCommands;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.service.command.Converter;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
<<<<<<< HEAD
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

=======
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import junit.framework.Assert;
import junit.framework.TestCase;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public abstract class ComponentTestBase
{

    @Inject
    protected BundleContext bundleContext;

    protected Bundle bundle;

<<<<<<< HEAD
    protected ServiceTracker scrTracker;

    protected ServiceTracker configAdminTracker;
=======
    protected ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;

    protected ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    // the name of the system property providing the bundle file to be installed and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "target/scr.jar";

    protected static final String PROP_NAME = "theValue";
<<<<<<< HEAD
    protected static final Dictionary<String, String> theConfig;
=======
    protected static final Dictionary<String, Object> theConfig;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    // the JVM option to set to enable remote debugging
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;

<<<<<<< HEAD
=======
    //To investigate any problems at all set to "debug"
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    protected static String DS_LOGLEVEL = "debug";

    protected static String bsnVersionUniqueness = "single";

    // the descriptor file to use for the installed test bundle
    protected static String descriptorFile = "/integration_test_simple_components.xml";
    protected static String COMPONENT_PACKAGE = "org.apache.felix.scr.integration.components";

<<<<<<< HEAD

    protected static boolean NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR = false;
    protected volatile Log log;
    
    //set to true to only get last 1000 lines of log.
    protected static boolean restrictedLogging;
    
    protected static String felixCaVersion = System.getProperty( "felix.ca.version" );


    static
    {
        theConfig = new Hashtable<String, String>();
=======
    protected static boolean NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR = false;
    protected volatile Log log;

    protected static String[] ignoredWarnings; //null unless you need it.

    //set to true to only get last 1000 lines of log.
    protected static boolean restrictedLogging;

    protected static String felixCaVersion = System.getProperty( "felix.ca.version" );

    protected static final String PROP_NAME_FACTORY = ComponentTestBase.PROP_NAME + ".factory";

    static
    {
        theConfig = new Hashtable<>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        theConfig.put( PROP_NAME, PROP_NAME );
    }

    @ProbeBuilder
<<<<<<< HEAD
    public TestProbeBuilder extendProbe(TestProbeBuilder builder) {
        builder.setHeader("Export-Package", "org.apache.felix.scr.integration.components," +
                                            "org.apache.felix.scr.integration.components.activatesignature," +
                                            "org.apache.felix.scr.integration.components.circular," +
                                            "org.apache.felix.scr.integration.components.circularFactory," +
                                            "org.apache.felix.scr.integration.components.concurrency," +
                                            "org.apache.felix.scr.integration.components.felix3680," +
                                            "org.apache.felix.scr.integration.components.felix3680_2");
        builder.setHeader("Import-Package", "org.apache.felix.scr,org.apache.felix.scr.component;mandatory:=\"status\"; status=\"provisional\"");
        builder.setHeader("Bundle-ManifestVersion", "2");
=======
    public TestProbeBuilder extendProbe(TestProbeBuilder builder)
    {
        builder.setHeader( "Export-Package",
                "org.apache.felix.scr.integration.components,"
                        + "org.apache.felix.scr.integration.components.activatesignature,"
                        + "org.apache.felix.scr.integration.components.annoconfig,"
                        + "org.apache.felix.scr.integration.components.circular,"
                        + "org.apache.felix.scr.integration.components.circularFactory,"
                        + "org.apache.felix.scr.integration.components.concurrency,"
                        + "org.apache.felix.scr.integration.components.deadlock,"
                        + "org.apache.felix.scr.integration.components.felix3680,"
                        + "org.apache.felix.scr.integration.components.felix3680_2,"
                        + "org.apache.felix.scr.integration.components.felix4984,"
                        + "org.apache.felix.scr.integration.components.felix5248,"
                        + "org.apache.felix.scr.integration.components.felix5276" );
        builder.setHeader( "Import-Package", "org.apache.felix.scr.component" );
        builder.setHeader( "Bundle-ManifestVersion", "2" );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        return builder;
    }

    @Configuration
    public static Option[] configuration()
    {
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP, BUNDLE_JAR_DEFAULT );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() )
        {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
<<<<<<< HEAD
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        final Option[] base = options(
            provision(
                CoreOptions.bundle( bundleFile.toURI().toString() ),
                mavenBundle( "org.ops4j.pax.tinybundles", "tinybundles", "1.0.0" ),
                mavenBundle( "org.apache.felix", "org.apache.felix.configadmin", felixCaVersion )
             ),
             junitBundles(),
             frameworkProperty( "org.osgi.framework.bsnversion" ).value( bsnVersionUniqueness ),
             systemProperty( "ds.factory.enabled" ).value( Boolean.toString( NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR ) ),
             systemProperty( "ds.loglevel" ).value( DS_LOGLEVEL )

        );
        final Option vmOption = ( paxRunnerVmOption != null ) ? CoreOptions.vmOption( paxRunnerVmOption ) : null;
        return OptionUtils.combine( base, vmOption );
    }


    @Before
    public void setUp() throws BundleException
    {
        log = new Log(restrictedLogging);
        log.start();
        bundleContext.addFrameworkListener( log );
        bundleContext.registerService( LogService.class.getName(), log, null );
        
        scrTracker = new ServiceTracker( bundleContext, "org.apache.felix.scr.ScrService", null );
        scrTracker.open();
        configAdminTracker = new ServiceTracker( bundleContext, "org.osgi.service.cm.ConfigurationAdmin", null );
=======
                    + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        final Option[] base = options(
                provision( CoreOptions.bundle( bundleFile.toURI().toString() ),
                        mavenBundle( "org.ops4j.pax.tinybundles", "tinybundles", "1.0.0" ),
                        mavenBundle( "org.apache.felix", "org.apache.felix.configadmin", felixCaVersion ) ),
                junitBundles(), frameworkProperty( "org.osgi.framework.bsnversion" ).value( bsnVersionUniqueness ),
                systemProperty( "ds.factory.enabled" ).value( Boolean.toString( NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR ) ),
                systemProperty( "ds.loglevel" ).value( DS_LOGLEVEL )

                );
        final Option vmOption = ( paxRunnerVmOption != null )? CoreOptions.vmOption( paxRunnerVmOption ): null;
        NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR = false;
        return OptionUtils.combine( base, vmOption );
    }

    @Before
    public void setUp() throws BundleException
    {
        log = new Log( restrictedLogging, ignoredWarnings );
        log.start();
        bundleContext.addFrameworkListener( log );
        bundleContext.registerService( LogService.class.getName(), log, null );

        scrTracker = new ServiceTracker<>( bundleContext,
                ServiceComponentRuntime.class, null );
        scrTracker.open();
        configAdminTracker = new ServiceTracker<>( bundleContext,
                ConfigurationAdmin.class, null );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        configAdminTracker.open();

        bundle = installBundle( descriptorFile, COMPONENT_PACKAGE );
        bundle.start();
    }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    @After
    public void tearDown() throws BundleException
    {
        try
        {
            if ( bundle != null && bundle.getState() != Bundle.UNINSTALLED )
            {
                bundle.uninstall();
                bundle = null;
            }

            configAdminTracker.close();
            configAdminTracker = null;
            scrTracker.close();
            scrTracker = null;
        }
        finally
        {
            log.stop();
        }
    }

<<<<<<< HEAD

    protected Component[] getComponents()
    {
        ScrService scr = ( ScrService ) scrTracker.getService();
        if ( scr != null )
        {
            return scr.getComponents();
        }

        return null;
    }


    protected Component findComponentByName( String name )
    {
        Component[] components = findComponentsByName( name );
        if ( components != null && components.length > 0 )
        {
            return components[0];
        }

        return null;
    }


    protected Component[] findComponentsByName( String name )
    {
        ScrService scr = ( ScrService ) scrTracker.getService();
        if ( scr != null )
        {
            return scr.getComponents( name );
        }

        return null;
    }


    protected static void delay()
    {
        try
        {
            Thread.sleep( 300 );
        }
        catch ( InterruptedException ie )
        {
            // dont care
        }
    }


    protected ConfigurationAdmin getConfigurationAdmin()
    {
        ConfigurationAdmin ca = ( ConfigurationAdmin ) configAdminTracker.getService();
=======
    protected Collection<ComponentDescriptionDTO> getComponentDescriptions()
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr == null )
        {
            TestCase.fail( "no ServiceComponentRuntime" );
        }
        return scr.getComponentDescriptionDTOs();
    }

    protected ComponentDescriptionDTO findComponentDescriptorByName(String name)
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr == null )
        {
            TestCase.fail( "no ServiceComponentRuntime" );
        }
        return scr.getComponentDescriptionDTO( bundle, name );
    }

    protected Collection<ComponentConfigurationDTO> findComponentConfigurationsByName(Bundle b, String name,
            int expected)
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr == null )
        {
            TestCase.fail( "no ServiceComponentRuntime" );
        }
        ComponentDescriptionDTO cd = scr.getComponentDescriptionDTO( b, name );
        Collection<ComponentConfigurationDTO> ccs = scr.getComponentConfigurationDTOs( cd );

        if ( expected != 0 )
        {
            String sep = "[";
            StringBuffer sb = new StringBuffer();
            for ( Map.Entry<Integer, String> entry : STATES.entrySet() )
            {
                if ( ( expected & entry.getKey() ) != 0 )
                {
                    sb.append( sep ).append( entry.getValue() );
                    sep = ", ";
                }
            }
            sb.append( "]" );
            for ( ComponentConfigurationDTO cc : ccs )
            {
                Assert.assertTrue(
                        "for ComponentConfiguration name: " + cc.description.name + " properties" + cc.properties
                        + "Expected one of state " + sb.toString() + " but was " + STATES.get( cc.state ),
                        ( expected & cc.state ) == cc.state );
            }
        }
        return ccs;
    }

    protected Collection<ComponentConfigurationDTO> findComponentConfigurationsByName(String name, int expected)
    {
        return findComponentConfigurationsByName( bundle, name, expected );
    }

    protected ComponentConfigurationDTO findComponentConfigurationByName(Bundle b, String name, int expected)
    {
        Collection<ComponentConfigurationDTO> ccs = findComponentConfigurationsByName( b, name, expected );
        Assert.assertEquals( 1, ccs.size() );
        return ccs.iterator().next();
    }

    protected ComponentConfigurationDTO findComponentConfigurationByName(String name, int expected)
    {
        return findComponentConfigurationByName( bundle, name, expected );
    }

    static final Map<Integer, String> STATES = new HashMap<>();

    static
    {
        STATES.put( ComponentConfigurationDTO.UNSATISFIED_REFERENCE,
                "Unsatisfied (" + ComponentConfigurationDTO.UNSATISFIED_REFERENCE + ")" );
        STATES.put( ComponentConfigurationDTO.SATISFIED, "Satisified (" + ComponentConfigurationDTO.SATISFIED + ")" );
        STATES.put( ComponentConfigurationDTO.ACTIVE, "Active (" + ComponentConfigurationDTO.ACTIVE + ")" );
    }

    protected ComponentConfigurationDTO getDisabledConfigurationAndEnable(Bundle b, String name, int initialState)
            throws InvocationTargetException, InterruptedException
    {
        int count = 1;
        Collection<ComponentConfigurationDTO> ccs = getConfigurationsDisabledThenEnable( b, name, count, initialState );
        ComponentConfigurationDTO cc = ccs.iterator().next();
        return cc;
    }

    protected ComponentConfigurationDTO getDisabledConfigurationAndEnable(String name, int initialState)
            throws InvocationTargetException, InterruptedException
    {
        return getDisabledConfigurationAndEnable( bundle, name, initialState );
    }

    protected Collection<ComponentConfigurationDTO> getConfigurationsDisabledThenEnable(Bundle b, String name,
            int count, int initialState) throws InvocationTargetException, InterruptedException
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr == null )
        {
            TestCase.fail( "no ServiceComponentRuntime" );
        }
        ComponentDescriptionDTO cd = scr.getComponentDescriptionDTO( b, name );
        Assert.assertFalse( "Expected component disabled", scr.isComponentEnabled( cd ) );
        scr.enableComponent( cd ).getValue();
        Assert.assertTrue( "Expected component enabled", scr.isComponentEnabled( cd ) );

        Collection<ComponentConfigurationDTO> ccs = scr.getComponentConfigurationDTOs( cd );
        Assert.assertEquals( count, ccs.size() );
        for ( ComponentConfigurationDTO cc : ccs )
        {
            Assert.assertEquals( "Expected state " + STATES.get( initialState ) + " but was " + STATES.get( cc.state ),
                    initialState, cc.state );
        }
        return ccs;
    }

    protected Collection<ComponentConfigurationDTO> getConfigurationsDisabledThenEnable(String name, int count,
            int initialState) throws InvocationTargetException, InterruptedException
    {
        return getConfigurationsDisabledThenEnable( bundle, name, count, initialState );
    }

    protected ComponentDescriptionDTO checkConfigurationCount(Bundle b, String name, int count, int expectedState)
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr == null )
        {
            TestCase.fail( "no ServiceComponentRuntime" );
        }
        ComponentDescriptionDTO cd = scr.getComponentDescriptionDTO( b, name );
        Assert.assertTrue( "Expected component enabled", scr.isComponentEnabled( cd ) );

        Collection<ComponentConfigurationDTO> ccs = scr.getComponentConfigurationDTOs( cd );
        Assert.assertEquals( count, ccs.size() );
        if ( expectedState != -1 )
        {
            for ( ComponentConfigurationDTO cc : ccs )
            {
                Assert.assertEquals(
                        "Expected state " + STATES.get( expectedState ) + " but was " + STATES.get( cc.state ),
                        expectedState, cc.state );
            }
        }
        return cd;
    }

    protected ComponentDescriptionDTO checkConfigurationCount(String name, int count, int expectedState)
    {
        return checkConfigurationCount( bundle, name, count, expectedState );
    }

    protected <S> S getServiceFromConfiguration(ComponentConfigurationDTO dto, Class<S> clazz)
    {
        long id = dto.id;
        String filter = "(component.id=" + id + ")";
        Collection<ServiceReference<S>> srs;
        try
        {
            srs = bundleContext.getServiceReferences( clazz, filter );
            Assert.assertEquals( "Nothing for filter: " + filter, 1, srs.size() );
            ServiceReference<S> sr = srs.iterator().next();
            S s = bundleContext.getService( sr );
            Assert.assertNotNull( s );
            return s;
        }
        catch ( InvalidSyntaxException e )
        {
            TestCase.fail( e.getMessage() );
            return null;//unreachable in fact
        }
    }

    protected <S> void ungetServiceFromConfiguration(ComponentConfigurationDTO dto, Class<S> clazz)
    {
        long id = dto.id;
        String filter = "(component.id=" + id + ")";
        Collection<ServiceReference<S>> srs;
        try
        {
            srs = bundleContext.getServiceReferences( clazz, filter );
            Assert.assertEquals( 1, srs.size() );
            ServiceReference<S> sr = srs.iterator().next();
            bundleContext.ungetService( sr );
        }
        catch ( InvalidSyntaxException e )
        {
            TestCase.fail( e.getMessage() );
        }
    }

    protected void enableAndCheck(ComponentDescriptionDTO cd) throws InvocationTargetException, InterruptedException
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr != null )
        {
            scr.enableComponent( cd ).getValue();
            Assert.assertTrue( "Expected component enabled", scr.isComponentEnabled( cd ) );
        }
        else
        {
            throw new NullPointerException( "no ServiceComponentRuntime" );
        }

    }

    protected void disableAndCheck(ComponentConfigurationDTO cc) throws InvocationTargetException, InterruptedException
    {
        ComponentDescriptionDTO cd = cc.description;
        disableAndCheck( cd );
    }

    protected void disableAndCheck(ComponentDescriptionDTO cd) throws InvocationTargetException, InterruptedException
    {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr != null )
        {
            scr.disableComponent( cd ).getValue();
            Assert.assertFalse( "Expected component disabled", scr.isComponentEnabled( cd ) );
        }
        else
        {
            throw new NullPointerException( "no ServiceComponentRuntime" );
        }
    }

    protected void disableAndCheck(String name) throws InvocationTargetException, InterruptedException
    {
        ComponentDescriptionDTO cd = findComponentDescriptorByName( name );
        disableAndCheck( cd );
    }

    protected static void delay()
    {
        delay( 300 );
    }

    protected static void delay(int millis)
    {
        try
        {
            Thread.sleep( millis );
        }
        catch ( InterruptedException ie )
        {
        }
    }

    protected ConfigurationAdmin getConfigurationAdmin()
    {
        ConfigurationAdmin ca = configAdminTracker.getService();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if ( ca == null )
        {
            TestCase.fail( "Missing ConfigurationAdmin service" );
        }
        return ca;
    }

<<<<<<< HEAD
    protected org.osgi.service.cm.Configuration configure( String pid )
    {
        return configure( pid, null );
        
    }

    protected org.osgi.service.cm.Configuration configure( String pid, String bundleLocation )
=======
    protected org.osgi.service.cm.Configuration configure(String pid)
    {
        return configure( pid, null );

    }

    protected org.osgi.service.cm.Configuration configure(String pid, String bundleLocation)
    {
        return configure( pid, bundleLocation, theConfig );
    }

    protected org.osgi.service.cm.Configuration configure(String pid, String bundleLocation,
            Dictionary<String, Object> props)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid, null );
<<<<<<< HEAD
            if (bundleLocation != null)
            {
                config.setBundleLocation( bundleLocation );
            }
            config.update( theConfig );
=======
            if ( bundleLocation != null )
            {
                config.setBundleLocation( bundleLocation );
            }
            config.update( props );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            return config;
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating configuration " + pid + ": " + ioe.toString() );
        }
        return null;
    }

<<<<<<< HEAD

    protected void deleteConfig( String pid )
=======
    protected void deleteConfig(String pid)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid );
            config.delete();
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed deleting configuration " + pid + ": " + ioe.toString() );
        }
    }

<<<<<<< HEAD

    protected String createFactoryConfiguration( String factoryPid )
=======
    protected String createFactoryConfiguration(String factoryPid, String bundleLocation)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.createFactoryConfiguration( factoryPid, null );
            config.update( theConfig );
            return config.getPid();
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating factory configuration " + factoryPid + ": " + ioe.toString() );
            return null;
        }
    }

<<<<<<< HEAD

    protected void deleteFactoryConfigurations( String factoryPid )
=======
    protected void deleteFactoryConfigurations(String factoryPid)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            final String filter = "(service.factoryPid=" + factoryPid + ")";
            org.osgi.service.cm.Configuration[] configs = ca.listConfigurations( filter );
            if ( configs != null )
            {
                for ( org.osgi.service.cm.Configuration configuration : configs )
                {
                    configuration.delete();
                }
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // unexpected
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed deleting configurations " + factoryPid + ": " + ioe.toString() );
        }
    }

<<<<<<< HEAD

    protected static Class<?> getType( Object object, String desiredName )
=======
    //component factory test helper methods
    protected ComponentFactory getComponentFactory(final String componentfactory) throws InvalidSyntaxException
    {
        final ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(),
                "(" + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        TestCase.assertNotNull( refs );
        TestCase.assertEquals( 1, refs.length );
        final ComponentFactory factory = (ComponentFactory) bundleContext.getService( refs[0] );
        TestCase.assertNotNull( factory );
        return factory;
    }

    protected void checkFactory(final String componentfactory, boolean expectFactoryPresent)
            throws InvalidSyntaxException
    {
        ServiceReference[] refs = bundleContext.getServiceReferences( ComponentFactory.class.getName(),
                "(" + ComponentConstants.COMPONENT_FACTORY + "=" + componentfactory + ")" );
        if ( expectFactoryPresent )
        {
            TestCase.assertNotNull( refs );
            TestCase.assertEquals( 1, refs.length );

        }
        else
        {
            TestCase.assertNull( refs );
        }
    }

    protected ComponentInstance createFactoryComponentInstance(final String componentfactory)
            throws InvalidSyntaxException
    {
        Hashtable<String, String> props = new Hashtable<>();
        props.put( PROP_NAME_FACTORY, PROP_NAME_FACTORY );

        return createFactoryComponentInstance( componentfactory, props );
    }

    protected ComponentInstance createFactoryComponentInstance(final String componentfactory,
            Hashtable<String, String> props) throws InvalidSyntaxException
    {
        final ComponentFactory factory = getComponentFactory( componentfactory );

        final ComponentInstance instance = factory.newInstance( props );
        TestCase.assertNotNull( instance );

        TestCase.assertNotNull( instance.getInstance() );
        TestCase.assertEquals( SimpleComponent.INSTANCE, instance.getInstance() );
        TestCase.assertEquals( PROP_NAME_FACTORY, SimpleComponent.INSTANCE.getProperty( PROP_NAME_FACTORY ) );
        return instance;
    }

    protected static Class<?> getType(Object object, String desiredName)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        Class<?> ccImpl = object.getClass();
        while ( ccImpl != null && !desiredName.equals( ccImpl.getSimpleName() ) )
        {
            ccImpl = ccImpl.getSuperclass();
        }
        if ( ccImpl == null )
        {
            TestCase.fail( "ComponentContext " + object + " is not a " + desiredName );
        }

        return ccImpl;
    }

<<<<<<< HEAD

    protected static Object getFieldValue( Object object, String fieldName )
=======
    protected static Object getFieldValue(Object object, String fieldName)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        try
        {
            final Field m_componentsField = getField( object.getClass(), fieldName );
            return m_componentsField.get( object );
        }
        catch ( Throwable t )
        {
            TestCase.fail( "Cannot get " + fieldName + " from " + object + ": " + t );
            return null; // keep the compiler happy
        }
    }
<<<<<<< HEAD
    
    protected Object getComponentManagerFromComponentInstance( Object instance )
    {
        Object cc = getFieldValue( instance, "m_componentContext");
        return getFieldValue( cc, "m_componentManager" );
    }


    protected static Field getField( Class<?> type, String fieldName ) throws NoSuchFieldException
    {
        Class<?> clazz = type;
        while (clazz != null)
        {
            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++)
            {
                Field field = fields[i];
                if (field.getName().equals(fieldName))
=======

    protected Object getComponentManagerFromComponentInstance(Object instance)
    {
        Object cc = getFieldValue( instance, "m_componentContext" );
        return getFieldValue( cc, "m_componentManager" );
    }

    protected static Field getField(Class<?> type, String fieldName) throws NoSuchFieldException
    {
        Class<?> clazz = type;
        while ( clazz != null )
        {
            Field[] fields = clazz.getDeclaredFields();
            for ( int i = 0; i < fields.length; i++ )
            {
                Field field = fields[i];
                if ( field.getName().equals( fieldName ) )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                {
                    field.setAccessible( true );
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
<<<<<<< HEAD
        throw new NoSuchFieldException(fieldName);        
    }


    protected Bundle installBundle( final String descriptorFile, String componentPackage ) throws BundleException
    {
        return installBundle(descriptorFile, componentPackage, "simplecomponent", "0.0.11", null);
    }
    
    protected Bundle installBundle( final String descriptorFile, String componentPackage, String symbolicName, String version, String location ) throws BundleException
    {
        final InputStream bundleStream = bundle()
                .add("OSGI-INF/components.xml", getClass().getResource(descriptorFile))

                .set(Constants.BUNDLE_SYMBOLICNAME, symbolicName)
                .set(Constants.BUNDLE_VERSION, version)
                .set(Constants.IMPORT_PACKAGE, componentPackage)
                .set("Service-Component", "OSGI-INF/components.xml")
                .build(withBnd());
=======
        throw new NoSuchFieldException( fieldName );
    }

    protected Bundle installBundle(final String descriptorFile, String componentPackage) throws BundleException
    {
        return installBundle( descriptorFile, componentPackage, "simplecomponent", "0.0.11", null );
    }

    protected Bundle installBundle(final String descriptorFile, String componentPackage, String symbolicName,
            String version, String location) throws BundleException
    {
        final InputStream bundleStream = bundle().add( "OSGI-INF/components.xml",
                getClass().getResource( descriptorFile ) )

                .set( Constants.BUNDLE_SYMBOLICNAME, symbolicName ).set( Constants.BUNDLE_VERSION, version ).set(
                        Constants.IMPORT_PACKAGE, componentPackage ).set( "Service-Component", "OSGI-INF/components.xml" ).set(
                                Constants.REQUIRE_CAPABILITY,
                                ExtenderNamespace.EXTENDER_NAMESPACE
                                + ";filter:=\"(&(osgi.extender=osgi.component)(version>=1.3)(!(version>=2.0)))\"" ).build(
                                        withBnd() );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        try
        {
            if ( location == null )
            {
                location = "test:SimpleComponent/" + System.currentTimeMillis();
            }
            return bundleContext.installBundle( location, bundleStream );
        }
        finally
        {
            try
            {
                bundleStream.close();
            }
            catch ( IOException ioe )
            {
            }
        }
    }

    //Code copied from ScrCommand to make it easier to find out what your test components are actually doing.
    //    @Test
<<<<<<< HEAD
    public void testDescription()
    {
        PrintStream out = System.out;
        info( out );
    }

    void info( PrintStream out )
    {
        Component[] components = getComponents();
        if ( components == null )
        {
            return;
        }

        for ( int j = 0; j < components.length; j++ )
        {
            Component component = components[j];
            out.print( "ID: " );
            out.println( component.getId() );
            out.print( "Name: " );
            out.println( component.getName() );
            out.print( "Bundle: " );
            out.println( component.getBundle().getSymbolicName() + " (" + component.getBundle().getBundleId() + ")" );
            out.print( "State: " );
            out.println( toStateString( component.getState() ) );
            out.print( "Default State: " );
            out.println( component.isDefaultEnabled() ? "enabled" : "disabled" );
            out.print( "Activation: " );
            out.println( component.isImmediate() ? "immediate" : "delayed" );

            // DS 1.1 new features
            out.print( "Configuration Policy: " );
            out.println( component.getConfigurationPolicy() );
            out.print( "Activate Method: " );
            out.print( component.getActivate() );
            if ( component.isActivateDeclared() )
            {
                out.print( " (declared in the descriptor)" );
            }
            out.println();
            out.print( "Deactivate Method: " );
            out.print( component.getDeactivate() );
            if ( component.isDeactivateDeclared() )
            {
                out.print( " (declared in the descriptor)" );
            }
            out.println();
            out.print( "Modified Method: " );
            if ( component.getModified() != null )
            {
                out.print( component.getModified() );
            }
            else
            {
                out.print( "-" );
            }
            out.println();

            if ( component.getFactory() != null )
            {
                out.print( "Factory: " );
                out.println( component.getFactory() );
            }

            String[] services = component.getServices();
            if ( services != null )
            {
                out.print( "Services: " );
                out.println( services[0] );
                for ( int i = 1; i < services.length; i++ )
                {
                    out.print( "          " );
                    out.println( services[i] );
                }
                out.print( "Service Type: " );
                out.println( component.isServiceFactory() ? "service factory" : "service" );
            }

            Reference[] refs = component.getReferences();
            if ( refs != null )
            {
                for ( int i = 0; i < refs.length; i++ )
                {
                    out.print( "Reference: " );
                    out.println( refs[i].getName() );
                    out.print( "    Satisfied: " );
                    out.println( refs[i].isSatisfied() ? "satisfied" : "unsatisfied" );
                    out.print( "    Service Name: " );
                    out.println( refs[i].getServiceName() );
                    if ( refs[i].getTarget() != null )
                    {
                        out.print( "    Target Filter: " );
                        out.println( refs[i].getTarget() );
                    }
                    out.print( "    Multiple: " );
                    out.println( refs[i].isMultiple() ? "multiple" : "single" );
                    out.print( "    Optional: " );
                    out.println( refs[i].isOptional() ? "optional" : "mandatory" );
                    out.print( "    Policy: " );
                    out.println( refs[i].isStatic() ? "static" : "dynamic" );
                    out.print( "    Policy option: " );
                    out.println( refs[i].isReluctant() ? "reluctant" : "greedy" );
                }
            }

            Dictionary props = component.getProperties();
            if ( props != null )
            {
                out.println( "Properties:" );
                TreeSet keys = new TreeSet( Collections.list( props.keys() ) );
                for ( Iterator ki = keys.iterator(); ki.hasNext(); )
                {
                    Object key = ki.next();
                    out.print( "    " );
                    out.print( key );
                    out.print( " = " );

                    Object prop = props.get( key );
                    if ( prop.getClass().isArray() )
                    {
                        prop = Arrays.asList( ( Object[] ) prop );
                    }
                    out.print( prop );

                    out.println();
                }
            }
        }
    }
    
    protected boolean isAtLeastR5() 
    {
        try
        {
            Method m = org.osgi.service.cm.Configuration.class.getDeclaredMethod( "getChangeCount");
=======
    public void testDescription() throws Exception {
        PrintStream out = System.out;
        info( new PrintWriter( out ) );
    }

    private static class InfoWriter extends ComponentCommands
    {

        protected InfoWriter(ServiceComponentRuntime scrService)
        {
            super( null, scrService, null );
        }

    }

    void info(PrintWriter out) throws Exception {
        ServiceComponentRuntime scr = scrTracker.getService();
        if ( scr == null )
        {
            TestCase.fail( "no ServiceComponentRuntime" );
        }
        InfoWriter iw = new InfoWriter(scr);
        out.print(iw.format(iw.list(), Converter.LINE));
    }

    protected boolean isAtLeastR5()
    {
        try
        {
            Method m = org.osgi.service.cm.Configuration.class.getDeclaredMethod( "getChangeCount" );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            return true;
        }
        catch ( SecurityException e )
        {
<<<<<<< HEAD
            throw new RuntimeException(e);
=======
            throw new RuntimeException( e );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        catch ( NoSuchMethodException e )
        {
            return false;
        }
    }

<<<<<<< HEAD
    private String toStateString( int state )
    {
        switch ( state )
        {
            case Component.STATE_DISABLED:
                return "disabled";
            case Component.STATE_UNSATISFIED:
                return "unsatisfied";
            case Component.STATE_ACTIVE:
                return "active";
            case Component.STATE_REGISTERED:
                return "registered";
            case Component.STATE_FACTORY:
                return "factory";
            case Component.STATE_DISPOSED:
                return "disposed";

            case Component.STATE_ENABLING:
                return "enabling";
            case Component.STATE_ENABLED:
                return "enabled";
            case Component.STATE_ACTIVATING:
                return "activating";
            case Component.STATE_DEACTIVATING:
                return "deactivating";
            case Component.STATE_DISABLING:
                return "disabling";
            case Component.STATE_DISPOSING:
                return "disposing";
            default:
                return String.valueOf( state );
        }
    }

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    // Used to ignore logs displayed by the framework from stdout.
    // (the log service will log it because it listen to fwk error
    // events ...).
    static class NullStdout extends PrintStream
    {
        NullStdout()
        {
<<<<<<< HEAD
            super(new OutputStream()
=======
            super( new OutputStream()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {
                @Override
                public void write(int b) throws IOException
                {
                }
<<<<<<< HEAD
            });
=======
            } );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    public static class LogEntry
    {
        private final String m_msg;
        private final int m_level;
        private final Throwable m_err;
        private final long m_time;
        private final Thread m_thread;

<<<<<<< HEAD

        LogEntry( int level, String msg, Throwable t )
=======
        LogEntry(int level, String msg, Throwable t)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            m_level = level;
            m_msg = msg;
            m_err = t;
            m_time = System.currentTimeMillis();
            m_thread = Thread.currentThread();
        }

<<<<<<< HEAD

=======
        @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public String toString()
        {
            return m_msg;
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public int getLevel()
        {
            return m_level;
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public String getMessage()
        {
            return m_msg;
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public Throwable getError()
        {
            return m_err;
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public long getTime()
        {
            return m_time;
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public Thread getThread()
        {
            return m_thread;
        }
    }
<<<<<<< HEAD
    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public static class Log implements LogService, FrameworkListener, Runnable
    {
        private static final int RESTRICTED_LOG_SIZE = 1000;
        private final SimpleDateFormat m_sdf = new SimpleDateFormat( "HH:mm:ss,S" );
<<<<<<< HEAD
        private final static PrintStream m_out = new PrintStream( new BufferedOutputStream( new FileOutputStream(
            FileDescriptor.err ), 128 ) );
        private final List<String> m_warnings = Collections.synchronizedList( new ArrayList<String>() );
        private LinkedBlockingQueue<LogEntry> m_logQueue = new LinkedBlockingQueue<LogEntry>();
        private volatile Thread m_logThread;
        private volatile PrintStream m_realOut;
        private volatile PrintStream m_realErr;
=======
        private final static PrintStream m_out = new PrintStream(
                new BufferedOutputStream( new FileOutputStream( FileDescriptor.err ), 128 ) );
        private final List<String> m_warnings = Collections.synchronizedList( new ArrayList<String>() );
        private LinkedBlockingQueue<LogEntry> m_logQueue = new LinkedBlockingQueue<>();
        private volatile Thread m_logThread;
        private volatile PrintStream m_realOut;
        private volatile PrintStream m_realErr;
        private String[] ignoredWarnings;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        protected Throwable firstFrameworkThrowable;

        private final boolean restrictedLogging;
        private final String[] log = new String[1000];
        private int i = 0;

<<<<<<< HEAD
        public Log( boolean restrictedLogging )
        {
            this.restrictedLogging = restrictedLogging;
=======
        public Log(boolean restrictedLogging, String[] ignoredWarnings)
        {
            this.restrictedLogging = restrictedLogging;
            this.ignoredWarnings = ignoredWarnings;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        public void start()
        {
            m_realOut = System.out;
            m_realErr = System.err;
            System.setOut( new NullStdout() );
            System.setErr( new NullStdout() );
            m_logThread = new Thread( this );
            m_logThread.start();
        }

<<<<<<< HEAD
        
        public void stop()
        {
            System.setOut(m_realOut);
            System.setErr(m_realErr);
            if ( restrictedLogging )
            {
                for (int j = 0; j < RESTRICTED_LOG_SIZE; j++)
                {
                    if ( log[i] != null )
                    {
                        m_realErr.println(log[i++]);
                    }
                    if (i == RESTRICTED_LOG_SIZE) i = 0;
=======
        public void stop()
        {
            System.setOut( m_realOut );
            System.setErr( m_realErr );
            if ( restrictedLogging )
            {
                for ( int j = 0; j < RESTRICTED_LOG_SIZE; j++ )
                {
                    if ( log[i] != null )
                    {
                        m_realErr.println( log[i++] );
                    }
                    if ( i == RESTRICTED_LOG_SIZE )
                        i = 0;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
            }
            else
            {
                m_out.flush();
            }
            m_warnings.clear();
            m_logThread.interrupt();
            try
            {
                m_logThread.join();
            }
            catch ( InterruptedException e )
            {
            }
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        List<String> foundWarnings()
        {
            return m_warnings;
        }
<<<<<<< HEAD
        
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        Throwable getFirstFrameworkThrowable()
        {
            return firstFrameworkThrowable;
        }

<<<<<<< HEAD


=======
        @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public void run()
        {
            try
            {
                LogEntry entry = null;
                while ( true )
                {
                    entry = m_logQueue.take();
<<<<<<< HEAD
                    if ( entry.getLevel() <= 2 )
=======
                    if ( entry.getLevel() <= 2 && acceptWarning( entry.getMessage() ) )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    {
                        if ( m_warnings.size() < 1024 )
                        {
                            m_warnings.add( entry.getMessage() );
                        }
                        else
                        {
                            // Avoid out of memory ...
                            m_warnings.set( 1023, "Unexpected errors logged. Please look at previous logs" );
                        }
                    }

                    StringWriter sw = new StringWriter();
                    sw.append( "log level: " + entry.getLevel() );
                    sw.append( " D=" );
                    sw.append( m_sdf.format( new Date( entry.getTime() ) ) );
                    sw.append( " T=" + entry.getThread() );
                    sw.append( ": " );
                    sw.append( entry.getMessage() );
                    if ( entry.getError() != null )
                    {
                        sw.append( System.getProperty( "line.separator" ) );
                        PrintWriter pw = new PrintWriter( sw );
                        entry.getError().printStackTrace( pw );
                        pw.flush();
                    }
                    if ( restrictedLogging )
                    {
                        log[i++] = sw.toString();
<<<<<<< HEAD
                        if ( i == RESTRICTED_LOG_SIZE ) i = 0;
=======
                        if ( i == RESTRICTED_LOG_SIZE )
                            i = 0;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    }
                    else
                    {
                        m_out.println( sw.toString() );
                        m_out.flush();
                    }
                }
            }
            catch ( InterruptedException e )
            {
                return;
            }
        }

<<<<<<< HEAD

        // ------------- FrameworkListener -----------------------------------------------------------

        public void frameworkEvent( final FrameworkEvent event )
        {
            int eventType = event.getType();
            String msg = getFrameworkEventMessage( eventType );
            int level = ( eventType == FrameworkEvent.ERROR ) ? LogService.LOG_ERROR : LogService.LOG_WARNING;
            log( level, msg, event.getThrowable() );
            if (event.getThrowable() != null && firstFrameworkThrowable == null)
=======
        // ------------- FrameworkListener -----------------------------------------------------------

        private boolean acceptWarning(String message)
        {
            if ( ignoredWarnings != null )
            {
                for ( String ignore : ignoredWarnings )
                {
                    if ( message.contains( ignore ) )
                    {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public void frameworkEvent(final FrameworkEvent event)
        {
            int eventType = event.getType();
            String msg = getFrameworkEventMessage( eventType );
            int level = ( eventType == FrameworkEvent.ERROR )? LogService.LOG_ERROR: LogService.LOG_WARNING;
            log( level, msg, event.getThrowable() );
            if ( event.getThrowable() != null && firstFrameworkThrowable == null )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            {
                firstFrameworkThrowable = event.getThrowable();
            }
        }

<<<<<<< HEAD

        // ------------ LogService ----------------------------------------------------------------

        public void log( int level, String message )
=======
        // ------------ LogService ----------------------------------------------------------------

        @Override
        public void log(int level, String message)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            log( level, message, null );
        }

<<<<<<< HEAD

        public void log( int level, String message, Throwable exception )
=======
        @Override
        public void log(int level, String message, Throwable exception)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            if ( level > getEnabledLogLevel() )
            {
                return;
            }
            m_logQueue.offer( new LogEntry( level, message, exception ) );
        }

<<<<<<< HEAD

        public void log( ServiceReference sr, int osgiLevel, String message )
=======
        @Override
        public void log(ServiceReference sr, int osgiLevel, String message)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            log( sr, osgiLevel, message, null );
        }

<<<<<<< HEAD

        public void log( ServiceReference sr, int level, String msg, Throwable exception )
=======
        @Override
        public void log(ServiceReference sr, int level, String msg, Throwable exception)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            if ( sr != null )
            {
                StringBuilder sb = new StringBuilder();
                Object serviceId = sr.getProperty( Constants.SERVICE_ID );
                if ( serviceId != null )
                {
                    sb.append( "[" + serviceId.toString() + "] " );
                }
                sb.append( msg );
                log( level, sb.toString(), exception );
            }
            else
            {
                log( level, msg, exception );
            }
        }

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        private int getEnabledLogLevel()
        {
            if ( DS_LOGLEVEL.regionMatches( true, 0, "err", 0, "err".length() ) )
            {
                return LogService.LOG_ERROR;
            }
            else if ( DS_LOGLEVEL.regionMatches( true, 0, "warn", 0, "warn".length() ) )
            {
                return LogService.LOG_WARNING;
            }
            else if ( DS_LOGLEVEL.regionMatches( true, 0, "info", 0, "info".length() ) )
            {
                return LogService.LOG_INFO;
            }
            else
            {
                return LogService.LOG_DEBUG;
            }
        }

<<<<<<< HEAD

        private String getFrameworkEventMessage( int event )
        {
            switch ( event )
            {
                case FrameworkEvent.ERROR:
                    return "FrameworkEvent: ERROR";
                case FrameworkEvent.INFO:
                    return "FrameworkEvent INFO";
                case FrameworkEvent.PACKAGES_REFRESHED:
                    return "FrameworkEvent: PACKAGE REFRESHED";
                case FrameworkEvent.STARTED:
                    return "FrameworkEvent: STARTED";
                case FrameworkEvent.STARTLEVEL_CHANGED:
                    return "FrameworkEvent: STARTLEVEL CHANGED";
                case FrameworkEvent.WARNING:
                    return "FrameworkEvent: WARNING";
                default:
                    return null;
=======
        private String getFrameworkEventMessage(int event)
        {
            switch (event)
            {
            case FrameworkEvent.ERROR:
                return "FrameworkEvent: ERROR";
            case FrameworkEvent.INFO:
                return "FrameworkEvent INFO";
            case FrameworkEvent.PACKAGES_REFRESHED:
                return "FrameworkEvent: PACKAGE REFRESHED";
            case FrameworkEvent.STARTED:
                return "FrameworkEvent: STARTED";
            case FrameworkEvent.STARTLEVEL_CHANGED:
                return "FrameworkEvent: STARTLEVEL CHANGED";
            case FrameworkEvent.WARNING:
                return "FrameworkEvent: WARNING";
            default:
                return null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }
}
