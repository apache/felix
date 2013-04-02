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
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


public abstract class ComponentTestBase
{

    @Inject
    protected BundleContext bundleContext;

    protected Bundle bundle;

    protected ServiceTracker scrTracker;

    protected ServiceTracker configAdminTracker;

    // the name of the system property providing the bundle file to be installed and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "target/scr.jar";

    protected static final String PROP_NAME = "theValue";
    protected static final Dictionary<String, String> theConfig;

    // the JVM option to set to enable remote debugging
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;

    protected static String DS_LOGLEVEL = "debug";

    // the descriptor file to use for the installed test bundle
    protected static String descriptorFile = "/integration_test_simple_components.xml";

    protected static boolean NONSTANDARD_COMPONENT_FACTORY_BEHAVIOR = false;
    protected volatile Log log;
    
    //set to true to only get last 1000 lines of log.
    protected static boolean restrictedLogging;
    
    protected static String felixCaVersion = System.getProperty( "felix.ca.version" );


    static
    {
        theConfig = new Hashtable<String, String>();
        theConfig.put( PROP_NAME, PROP_NAME );
    }

    @ProbeBuilder
    public TestProbeBuilder extendProbe(TestProbeBuilder builder) {
        builder.setHeader("Export-Package", "org.apache.felix.scr.integration.components," +
                                            "org.apache.felix.scr.integration.components.activatesignature," +
                                            "org.apache.felix.scr.integration.components.circular," +
                                            "org.apache.felix.scr.integration.components.concurrency," +
                                            "org.apache.felix.scr.integration.components.felix3680," +
                                            "org.apache.felix.scr.integration.components.felix3680_2");
        builder.setHeader("Import-Package", "org.apache.felix.scr,org.apache.felix.scr.component;mandatory:=\"status\"; status=\"provisional\"");
        builder.setHeader("Bundle-ManifestVersion", "2");
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
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        final Option[] base = options(
            provision(
                CoreOptions.bundle( bundleFile.toURI().toString() ),
                mavenBundle( "org.ops4j.pax.tinybundles", "tinybundles", "1.0.0" ),
                mavenBundle( "org.apache.felix", "org.apache.felix.configadmin", felixCaVersion )
             ),
             junitBundles(),
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
        configAdminTracker.open();

        bundle = installBundle( descriptorFile );
        bundle.start();
    }


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
        if ( ca == null )
        {
            TestCase.fail( "Missing ConfigurationAdmin service" );
        }
        return ca;
    }


    protected void configure( String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid, null );
            config.update( theConfig );
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating configuration " + pid + ": " + ioe.toString() );
        }
    }


    protected void deleteConfig( String pid )
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


    protected String createFactoryConfiguration( String factoryPid )
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


    protected void deleteFactoryConfigurations( String factoryPid )
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


    protected static Class<?> getType( Object object, String desiredName )
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


    protected static Object getFieldValue( Object object, String fieldName )
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
                {
                    field.setAccessible( true );
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);        
    }


    protected Bundle installBundle( final String descriptorFile ) throws BundleException
    {
        final InputStream bundleStream = bundle()
            .add("OSGI-INF/components.xml", getClass().getResource(descriptorFile))

                .set(Constants.BUNDLE_SYMBOLICNAME, "simplecomponent")
                .set(Constants.BUNDLE_VERSION, "0.0.11")
                .set(Constants.IMPORT_PACKAGE,
                        "org.apache.felix.scr.integration.components," +
                        "org.apache.felix.scr.integration.components.activatesignature," +
                        "org.apache.felix.scr.integration.components.circular," +
                        "org.apache.felix.scr.integration.components.concurrency," +
                        "org.apache.felix.scr.integration.components.felix3680," +
                        "org.apache.felix.scr.integration.components.felix3680_2")
                .set("Service-Component", "OSGI-INF/components.xml")
            .build(withBnd());

        try
        {
            final String location = "test:SimpleComponent/" + System.currentTimeMillis();
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

    // Used to ignore logs displayed by the framework from stdout.
    // (the log service will log it because it listen to fwk error
    // events ...).
    static class NullStdout extends PrintStream
    {
        NullStdout()
        {
            super(new OutputStream()
            {
                @Override
                public void write(int b) throws IOException
                {
                }
            });
        }
    }

    public static class LogEntry
    {
        private final String m_msg;
        private final int m_level;
        private final Throwable m_err;
        private final long m_time;
        private final Thread m_thread;


        LogEntry( int level, String msg, Throwable t )
        {
            m_level = level;
            m_msg = msg;
            m_err = t;
            m_time = System.currentTimeMillis();
            m_thread = Thread.currentThread();
        }


        public String toString()
        {
            return m_msg;
        }


        public int getLevel()
        {
            return m_level;
        }


        public String getMessage()
        {
            return m_msg;
        }


        public Throwable getError()
        {
            return m_err;
        }


        public long getTime()
        {
            return m_time;
        }


        public Thread getThread()
        {
            return m_thread;
        }
    }
    
    public static class Log implements LogService, FrameworkListener, Runnable
    {
        private static final int RESTRICTED_LOG_SIZE = 1000;
        private final SimpleDateFormat m_sdf = new SimpleDateFormat( "HH:mm:ss,S" );
        private final static PrintStream m_out = new PrintStream( new BufferedOutputStream( new FileOutputStream(
            FileDescriptor.err ), 128 ) );
        private final List<String> m_warnings = Collections.synchronizedList( new ArrayList<String>() );
        private LinkedBlockingQueue<LogEntry> m_logQueue = new LinkedBlockingQueue<LogEntry>();
        private volatile Thread m_logThread;
        private volatile PrintStream m_realOut;
        private volatile PrintStream m_realErr;

        protected Throwable firstFrameworkThrowable;

        private final boolean restrictedLogging;
        private final String[] log = new String[1000];
        private int i = 0;

        public Log( boolean restrictedLogging )
        {
            this.restrictedLogging = restrictedLogging;
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


        List<String> foundWarnings()
        {
            return m_warnings;
        }
        
        Throwable getFirstFrameworkThrowable()
        {
            return firstFrameworkThrowable;
        }



        public void run()
        {
            try
            {
                LogEntry entry = null;
                while ( true )
                {
                    entry = m_logQueue.take();
                    if ( entry.getLevel() <= 2 )
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
                        if ( i == RESTRICTED_LOG_SIZE ) i = 0;
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


        // ------------- FrameworkListener -----------------------------------------------------------

        public void frameworkEvent( final FrameworkEvent event )
        {
            int eventType = event.getType();
            String msg = getFrameworkEventMessage( eventType );
            int level = ( eventType == FrameworkEvent.ERROR ) ? LogService.LOG_ERROR : LogService.LOG_WARNING;
            log( level, msg, event.getThrowable() );
            if (event.getThrowable() != null && firstFrameworkThrowable == null)
            {
                firstFrameworkThrowable = event.getThrowable();
            }
        }


        // ------------ LogService ----------------------------------------------------------------

        public void log( int level, String message )
        {
            log( level, message, null );
        }


        public void log( int level, String message, Throwable exception )
        {
            if ( level > getEnabledLogLevel() )
            {
                return;
            }
            m_logQueue.offer( new LogEntry( level, message, exception ) );
        }


        public void log( ServiceReference sr, int osgiLevel, String message )
        {
            log( sr, osgiLevel, message, null );
        }


        public void log( ServiceReference sr, int level, String msg, Throwable exception )
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
            }
        }
    }
}
