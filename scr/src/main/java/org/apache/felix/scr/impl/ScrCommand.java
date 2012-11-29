/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * The <code>ScrCommand</code> class provides the implementations for the
 * Apache Felix Gogo and legacy Apache Felix Shell commands. The
 * {@link #register(BundleContext, ScrService, ScrConfiguration)} method
 * instantiates and registers the Gogo and Shell commands as possible.
 */
class ScrCommand
{

    private final BundleContext bundleContext;
    private final ScrService scrService;
    private final ScrConfiguration scrConfiguration;

    static void register(BundleContext bundleContext, ScrService scrService, ScrConfiguration scrConfiguration)
    {
        final ScrCommand cmd = new ScrCommand(bundleContext, scrService, scrConfiguration);

        /*
         * Register the Gogo Command as a service of its own class.
         * Due to a race condition during project building (this class is
         * compiled for Java 1.3 compatibility before the required
         * ScrGogoCommand class compiled for Java 5 compatibility) this uses
         * reflection to load and instantiate the class. Any failure during this
         * process is just ignored.
         */
        try
        {
            final String scrGogoCommandClassName = "org.apache.felix.scr.impl.ScrGogoCommand";
            final Class scrGogoCommandClass = scrService.getClass().getClassLoader().loadClass(scrGogoCommandClassName);
            final Constructor c = scrGogoCommandClass.getConstructor(new Class[]
                { ScrCommand.class });
            final Object gogoCmd = c.newInstance(new Object[]
                { cmd });
            final Hashtable props = new Hashtable();
            props.put("osgi.command.scope", "scr");
            props.put("osgi.command.function", new String[]
                { "config", "disable", "enable", "info", "list" });
            props.put(Constants.SERVICE_DESCRIPTION, "SCR Gogo Shell Support");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            bundleContext.registerService(scrGogoCommandClassName, gogoCmd, props);
        }
        catch (Throwable t)
        {
            // Might be thrown if running in a pre-Java 5 VM
        }

        // We dynamically import the impl service API, so it
        // might not actually be available, so be ready to catch
        // the exception when we try to register the command service.
        try
        {
            // Register "scr" impl command service as a
            // wrapper for the bundle repository service.
            final Hashtable props = new Hashtable();
            props.put(Constants.SERVICE_DESCRIPTION, "SCR Legacy Shell Support");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            bundleContext.registerService(org.apache.felix.shell.Command.class.getName(), new ScrShellCommand(cmd),
                props);
        }
        catch (Throwable th)
        {
            // Ignore.
        }
    }

    private ScrCommand(BundleContext bundleContext, ScrService scrService, ScrConfiguration scrConfiguration)
    {
        this.bundleContext = bundleContext;
        this.scrService = scrService;
        this.scrConfiguration = scrConfiguration;
    }

    // ---------- Actual implementation

    void list(final String bundleIdentifier, final PrintStream out, final PrintStream err)
    {
        Component[] components;

        if (bundleIdentifier != null)
        {
            Bundle bundle = null;
            try
            {
                long bundleId = Long.parseLong(bundleIdentifier);
                bundle = bundleContext.getBundle(bundleId);
            }
            catch (NumberFormatException nfe)
            {
                // might be a bundle symbolic name
                Bundle[] bundles = bundleContext.getBundles();
                for (int i = 0; i < bundles.length; i++)
                {
                    if (bundleIdentifier.equals(bundles[i].getSymbolicName()))
                    {
                        bundle = bundles[i];
                        break;
                    }
                }
            }

            if (bundle == null)
            {
                err.println("Missing bundle with ID " + bundleIdentifier);
                return;
            }
            if (ComponentRegistry.isBundleActive(bundle))
            {
                components = scrService.getComponents(bundle);
                if (components == null)
                {
                    out.println("Bundle " + bundleIdentifier + " declares no components");
                    return;
                }
            }
            else
            {
                out.println("Bundle " + bundleIdentifier + " is not active");
                return;
            }
        }
        else
        {
            components = scrService.getComponents();
            if (components == null)
            {
                out.println("No components registered");
                return;
            }
        }

        out.println("   Id   State          Name");
        for ( Component component : components )
        {
            out.print( '[' );
            out.print( pad( String.valueOf( component.getId() ), -4 ) );
            out.print( "] [" );
            out.print( pad( toStateString( component.getState() ), 13 ) );
            out.print( "] " );
            out.print( component.getName() );
            out.println();
        }
    }

    void info(final String componentId, PrintStream out, PrintStream err)
    {
        Component[] components = getComponentFromArg(componentId, err);
        if (components == null)
        {
            return;
        }

        for ( Component component : components )
        {
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

            out.print( "Configuration Pid: " );
            out.print( component.getConfigurationPid() );
            if ( component.isConfigurationPidDeclared() )
            {
                out.print( " (declared in the descriptor)" );
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
                for ( Reference ref : refs )
                {
                    out.print( "Reference: " );
                    out.println( ref.getName() );
                    out.print( "    Satisfied: " );
                    out.println( ref.isSatisfied() ? "satisfied" : "unsatisfied" );
                    out.print( "    Service Name: " );
                    out.println( ref.getServiceName() );
                    if ( ref.getTarget() != null )
                    {
                        out.print( "    Target Filter: " );
                        out.println( ref.getTarget() );
                    }
                    out.print( "    Multiple: " );
                    out.println( ref.isMultiple() ? "multiple" : "single" );
                    out.print( "    Optional: " );
                    out.println( ref.isOptional() ? "optional" : "mandatory" );
                    out.print( "    Policy: " );
                    out.println( ref.isStatic() ? "static" : "dynamic" );
                    out.print( "    Policy option: " );
                    out.println( ref.isReluctant() ? "reluctant" : "greedy" );
                    ServiceReference[] serviceRefs = ref.getBoundServiceReferences();
                    if ( serviceRefs != null )
                    {
                        out.print( "    Bound to:" );
                        for ( int k = 0; k < serviceRefs.length; k++ )
                        {
                            out.print( "        " );
                            out.println( serviceRefs[k] );
                        }
                    }
                }
            }

            Dictionary props = component.getProperties();
            if ( props != null )
            {
                out.println( "Properties:" );
                TreeSet keys = new TreeSet( Collections.list( props.keys() ) );
                for ( Object key : keys )
                {
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

    void change(final String componentIdentifier, PrintStream out, PrintStream err, boolean enable)
    {
        Component[] components = getComponentFromArg(componentIdentifier, err);
        if (components == null)
        {
            return;
        }

        for ( Component component : components )
        {
            if ( component.getState() == Component.STATE_DISPOSED )
            {
                err.println( "Component " + component.getName() + " already disposed, cannot change state" );
            }
            else if ( enable )
            {
                if ( component.getState() == Component.STATE_DISABLED )
                {
                    component.enable();
                    out.println( "Component " + component.getName() + " enabled" );
                }
                else
                {
                    out.println( "Component " + component.getName() + " already enabled" );
                }
            }
            else
            {
                if ( component.getState() != Component.STATE_DISABLED )
                {
                    component.disable();
                    out.println( "Component " + component.getName() + " disabled" );
                }
                else
                {
                    out.println( "Component " + component.getName() + " already disabled" );
                }
            }
        }
    }

    void config(PrintStream out)
    {
        out.print("Log Level: ");
        out.println(scrConfiguration.getLogLevel());
        out.print("Component Factory with Factory Configuration: ");
        out.println(scrConfiguration.isFactoryEnabled() ? "Supported" : "Unsupported");
    }

    private String pad(String value, int size)
    {
        boolean right = size < 0;
        size = right ? -size : size;

        if (value.length() >= size)
        {
            return value;
        }

        char[] buf = new char[size];
        int padLen = size - value.length();
        int valOff = right ? padLen : 0;
        int padOff = right ? 0 : value.length();

        value.getChars(0, value.length(), buf, valOff);
        Arrays.fill(buf, padOff, padOff + padLen, ' ');

        return new String(buf);
    }

    private String toStateString(int state)
    {
        switch (state)
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
                return String.valueOf(state);
        }
    }

    private Component[] getComponentFromArg(final String componentIdentifier, PrintStream err)
    {
        Component[] components = null;
        if (componentIdentifier != null)
        {
            try
            {
                long componentId = Long.parseLong(componentIdentifier);
                Component component = scrService.getComponent(componentId);
                if (component == null)
                {
                    err.println("Missing Component with ID " + componentId);
                }
                else
                {
                    components = new Component[]
                        { component };
                }
            }
            catch (NumberFormatException nfe)
            {
                // check whether it is a component name
                components = scrService.getComponents(componentIdentifier);
                if (components == null)
                {
                    err.println("Missing Component with ID " + componentIdentifier);
                }
            }
        }
        else
        {

            err.println("Component ID required");
        }

        return components;
    }

}
