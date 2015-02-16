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

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

/**
 * The <code>ScrCommand</code> class provides the implementations for the
 * Apache Felix Gogo and legacy Apache Felix Shell commands. The
 * {@link #register(BundleContext, ScrService, ScrConfiguration)} method
 * instantiates and registers the Gogo and Shell commands as possible.
 */
public class ScrCommand
{

    private final BundleContext bundleContext;
    private final ServiceComponentRuntime scrService;
    private final ScrConfiguration scrConfiguration;

    private ServiceRegistration<?> gogoReg;
    private ServiceRegistration<?> shellReg;

    static ScrCommand register(BundleContext bundleContext, ServiceComponentRuntime scrService, ScrConfiguration scrConfiguration)
    {
        final ScrCommand cmd = new ScrCommand(bundleContext, scrService, scrConfiguration);

        cmd.registerCommands(bundleContext, scrService);
        return cmd;
    }

    //used by ComponentTestBase
    protected ScrCommand(BundleContext bundleContext, ServiceComponentRuntime scrService, ScrConfiguration scrConfiguration)
    {
        this.bundleContext = bundleContext;
        this.scrService = scrService;
        this.scrConfiguration = scrConfiguration;
    }

    private void registerCommands(BundleContext bundleContext,
        ServiceComponentRuntime scrService)
    {
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
            final Class<?> scrGogoCommandClass = scrService.getClass().getClassLoader().loadClass(scrGogoCommandClassName);
            final Constructor c = scrGogoCommandClass.getConstructor(new Class[]
                { ScrCommand.class });
            final Object gogoCmd = c.newInstance(new Object[]
                { this });
            final Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("osgi.command.scope", "scr");
            props.put("osgi.command.function", new String[]
                { "config", "disable", "enable", "info", "list" });
            props.put(Constants.SERVICE_DESCRIPTION, "SCR Gogo Shell Support");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            gogoReg = bundleContext.registerService(scrGogoCommandClassName, gogoCmd, props);
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
            final Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_DESCRIPTION, "SCR Legacy Shell Support");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            shellReg = bundleContext.registerService(org.apache.felix.shell.Command.class, new ScrShellCommand(this),
                props);
        }
        catch (Throwable th)
        {
            // Ignore.
        }
    }

    void unregister()
    {
        if (gogoReg != null)
        {
            gogoReg.unregister();
            gogoReg = null;
        }
        if ( shellReg != null )
        {
            shellReg.unregister();
            shellReg = null;
        }
    }

    // ---------- Actual implementation


    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#list(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void list(final String bundleIdentifier, final PrintWriter out)
    {
        List<ComponentDescriptionDTO> components;

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
                throw new IllegalArgumentException("Missing bundle with ID " + bundleIdentifier);
            }
            if (ComponentRegistry.isBundleActive(bundle))
            {
                components = new ArrayList<ComponentDescriptionDTO>(scrService.getComponentDescriptionDTOs(bundle));
                if (components.isEmpty())
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
            components = new ArrayList<ComponentDescriptionDTO>(scrService.getComponentDescriptionDTOs());
            if (components.isEmpty())
            {
                out.println("No components registered");
                return;
            }
        }

        Collections.sort( components, new Comparator<ComponentDescriptionDTO>()
                {

                    public int compare(ComponentDescriptionDTO c1, ComponentDescriptionDTO c2)
                    {
                        return c1.name.compareTo(c2.name);
                    }

                });

        out.println(" Name  BundleId DefaultEnabled");
        for ( ComponentDescriptionDTO component : components )
        {
            out.println( String.format( "[%1$s] [%2$4d] [%3$b]", component.name, component.bundle.id, component.defaultEnabled ) );
        }
        out.flush();
   }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#info(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void info(final String componentId, PrintWriter out)
    {
        Collection<ComponentDescriptionDTO> components = getComponentFromArg(componentId);
        if (components == null)
        {
            return;
        }

        Collections.sort( new ArrayList<ComponentDescriptionDTO>(components), new Comparator<ComponentDescriptionDTO>()
                {

                    public int compare(ComponentDescriptionDTO c1, ComponentDescriptionDTO c2)
                    {
                        long bundleId1 = c1.bundle.id;
                        long bundleId2 = c2.bundle.id;
                        int result = Long.signum(bundleId1 - bundleId2);
                        if ( result == 0)
                        {
                            result = c1.name.compareTo(c2.name);
                        }
                        return result;
                    }

                });

        long bundleId = -1;

        for ( ComponentDescriptionDTO component : components )
        {
            if ( component.bundle.id != bundleId )
            {
                if ( bundleId != -1 )
                {
                    out.println();
                    out.println();
                }
                bundleId = component.bundle.id;
                out.println(String.format("*** Bundle: %1$s (%2$d)", component.bundle.symbolicName, bundleId));
            }
            out.println( "Component Description:");
            out.print( "  Name: " );
            out.println( component.name );
            out.print( "  Default State: " );
            out.println( component.defaultEnabled ? "enabled" : "disabled" );
            out.print( "  Activation: " );
            out.println( component.immediate ? "immediate" : "delayed" );

            // DS 1.1 new features
            out.print( "  Configuration Policy: " );
            out.println( component.configurationPolicy );
            out.print( "  Activate Method: " );
            out.print( component.activate );
            out.println();
            out.print( "  Deactivate Method: " );
            out.print( component.deactivate );
            out.println();
            out.print( "  Modified Method: " );
            if ( component.modified != null )
            {
                out.print( component.modified );
            }
            else
            {
                out.print( "-" );
            }
            out.println();

            out.print( "  Configuration Pid: " );
            out.print( Arrays.asList(component.configurationPid) );
            out.println();

            if ( component.factory != null )
            {
                out.print( "  Factory: " );
                out.println( component.factory );
            }

            String[] services = component.serviceInterfaces;
            if ( services != null )
            {
                out.print( "  Services: " );
                for ( int i = 1; i < services.length; i++ )
                {
                    out.print( "          " );
                    out.println( services[i] );
                }
                out.print( "  Service Scope: " );
                out.println( component.scope );
            }

            ReferenceDTO[] refs = component.references;
            if ( refs != null )
            {
                for ( ReferenceDTO ref : refs )
                {
                    out.print( "  Reference: " );
                    out.println( ref.name );
                    out.print( "    Interface Name: " );
                    out.println( ref.interfaceName );
                    if ( ref.target != null )
                    {
                        out.print( "    Target Filter: " );
                        out.println( ref.target );
                    }
                    out.print( "    Cardinality: " );
                    out.println( ref.cardinality );
                    out.print( "    Policy: " );
                    out.println( ref.policy );
                    out.print( "    Policy option: " );
                    out.println( ref.policyOption );
                    out.print( "    Reference Scope: ");
                    out.println( ref.scope);

                }
            }

            Map<String, Object> props = component.properties;
            propertyInfo(props, out, "");
            for (ComponentConfigurationDTO cc: scrService.getComponentConfigurationDTOs(component))
            {
                info(cc, out);
            }
        }

        out.flush();
    }

    void propertyInfo(Map<String, Object> props, PrintWriter out, String prefix)
    {
        if ( props != null )
        {
            out.print( prefix );
            out.println( "  Properties:" );
            TreeMap<String, Object> keys = new TreeMap<String, Object>( props );
            for ( Entry<String, Object> entry: keys.entrySet() )
            {
                out.print( prefix );
                out.print( "    " );
                out.print( entry.getKey() );
                out.print( " = " );

                Object prop = entry.getValue();
                if ( prop.getClass().isArray() )
                {
                    out.print("[");
                    int length = Array.getLength(prop);
                    for (int i = 0; i< length; i++)
                    {
                        out.print(Array.get(prop, i));
                        if ( i < length - 1)
                        {
                            out.print(", ");
                        }
                    }
                    out.println("]");
                }
                else
                {
                    out.println( prop );
                }
            }
        }
    }

    private void info(ComponentConfigurationDTO cc, PrintWriter out)
    {
        out.println( "  Component Configuration:");
        out.print("    ComponentId: ");
        out.println( cc.id );
        out.print("    State: ");
        out.println( toStateString(cc.state));
        for ( SatisfiedReferenceDTO ref: cc.satisfiedReferences)
        {
            out.print( "    SatisfiedReference: ");
            out.println( ref.name );
            out.print( "      Target: " );
            out.println( ref.target );
          ServiceReferenceDTO[] serviceRefs = ref.boundServices;
          if ( serviceRefs != null )
          {
              out.print( "      Bound to:" );
              for ( ServiceReferenceDTO sr: serviceRefs )
              {
                  out.print( "        " );
                  out.println( sr.id );
                  propertyInfo(sr.properties, out, "        ");
              }
          }
          else
          {
              out.println( "      (unbound)" );
          }

        }
        for ( UnsatisfiedReferenceDTO ref: cc.unsatisfiedReferences)
        {
            out.print( "    UnsatisfiedReference: ");
            out.println( ref.name );
            out.print( "      Target: " );
            out.println( ref.target );
          ServiceReferenceDTO[] serviceRefs = ref.targetServices;
          if ( serviceRefs != null )
          {
              out.print( "      Target services:" );
              for ( ServiceReferenceDTO sr: serviceRefs )
              {
                  out.print( "        " );
                  out.println( sr.id );
              }
          }
          else
          {
              out.println( "      (unbound)" );
          }

        }
        propertyInfo( cc.properties, out, "    ");
    }

    void change(final String componentIdentifier, PrintWriter out, boolean enable)
    {
        Collection<ComponentDescriptionDTO> components = getComponentFromArg(componentIdentifier);
        ArrayList<String> disposed = new ArrayList<String>();
        if (components == null)
        {
            return;
        }

        for ( ComponentDescriptionDTO component : components )
        {
            if ( enable )
            {
                if ( !scrService.isComponentEnabled(component) )
                {
                    scrService.enableComponent(component);
                    out.println( "Component " + component.name + " enabled" );
                }
                else
                {
                    out.println( "Component " + component.name + " already enabled" );
                }
            }
            else
            {
                if ( scrService.isComponentEnabled(component) )
                {
                    scrService.disableComponent(component);
                    out.println( "Component " + component.name + " disabled" );
                }
                else
                {
                    out.println( "Component " + component.name + " already disabled" );
                }
            }
        }
        out.flush();
        if ( !disposed.isEmpty() )
        {
            throw new IllegalArgumentException( "Components " + disposed + " already disposed, cannot change state" );

        }
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#config(java.io.PrintStream)
     */
    public void config(PrintWriter out)
    {
        out.print("Log Level: ");
        out.println(scrConfiguration.getLogLevel());
        out.print("Obsolete Component Factory with Factory Configuration: ");
        out.println(scrConfiguration.isFactoryEnabled() ? "Supported" : "Unsupported");
        out.print("Keep instances with no references: ");
        out.println(scrConfiguration.keepInstances() ? "Supported" : "Unsupported");
        out.print("Lock timeount milliseconds: ");
        out.println(scrConfiguration.lockTimeout());
        out.print("Stop timeount milliseconds: ");
        out.println(scrConfiguration.stopTimeout());
        out.print("Global extender: ");
        out.println(scrConfiguration.globalExtender());
    }

    private String toStateString(int state)
    {
        switch (state) {

        case (ComponentConfigurationDTO.UNSATISFIED_REFERENCE):
            return "unsatisfied reference";
        case (ComponentConfigurationDTO.ACTIVE):
            return "active      ";
        case (ComponentConfigurationDTO.SATISFIED):
            return "satisfied  ";
        default:
            return "unkown: " + state;
        }
    }

    private Collection<ComponentDescriptionDTO> getComponentFromArg(final String componentIdentifier)
    {
        Collection<ComponentDescriptionDTO> components = scrService.getComponentDescriptionDTOs();
        if (componentIdentifier != null)
        {
            ArrayList<ComponentDescriptionDTO> cs = new ArrayList<ComponentDescriptionDTO>(components.size());
            Pattern p = Pattern.compile(componentIdentifier);
            for (ComponentDescriptionDTO component: components)
            {
                if ( p.matcher( component.name).matches() )
                {
                    cs.add( component );
                }
            }
            if (cs.isEmpty())
            {
                throw new IllegalArgumentException("No Component with ID or matching " + componentIdentifier);
            }
            components = cs;
        }

        return components;
    }

}
