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

import org.apache.felix.scr.impl.config.ScrConfigurationImpl;
import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.info.ScrInfo;
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
public class ScrCommand implements ScrInfo
{

    private static final Comparator<ComponentDescriptionDTO> DESCRIPTION_COMP = new Comparator<ComponentDescriptionDTO>()
    {
        public int compare(final ComponentDescriptionDTO c1, final ComponentDescriptionDTO c2)
        {
            final long bundleId1 = c1.bundle.id;
            final long bundleId2 = c2.bundle.id;
            int result = Long.signum(bundleId1 - bundleId2);
            if ( result == 0)
            {
                // sanity check
                if ( c1.name == null )
                {
                    result = ( c2.name == null ? 0 : -1);
                }
                else if ( c2.name == null )
                {
                    result = 1;
                }
                else
                {
                    result = c1.name.compareTo(c2.name);
                }
            }
            return result;
        }
    };

    private static final Comparator<ComponentConfigurationDTO> CONFIGURATION_COMP = new Comparator<ComponentConfigurationDTO>()
    {
        public int compare(final ComponentConfigurationDTO c1, final ComponentConfigurationDTO c2)
        {
            return Long.signum(c1.id - c2.id);
        }
    };

    private final BundleContext bundleContext;
    private final ServiceComponentRuntime scrService;
    private final ScrConfigurationImpl scrConfiguration;

    private ServiceRegistration<ScrInfo> reg;
    private ServiceRegistration<?> gogoReg;
    private ServiceRegistration<?> shellReg;

    static ScrCommand register(BundleContext bundleContext, ServiceComponentRuntime scrService, ScrConfigurationImpl scrConfiguration)
    {
        final ScrCommand cmd = new ScrCommand(bundleContext, scrService, scrConfiguration);

        cmd.registerCommands(bundleContext, scrService);
        return cmd;
    }

    //used by ComponentTestBase
    protected ScrCommand(BundleContext bundleContext, ServiceComponentRuntime scrService, ScrConfigurationImpl scrConfiguration)
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
         */
        try
        {
            final ScrGogoCommand gogoCmd = new ScrGogoCommand(this);
            final Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("osgi.command.scope", "scr");
            props.put("osgi.command.function", new String[]
                { "config", "disable", "enable", "info", "list" });
            props.put(Constants.SERVICE_DESCRIPTION, "SCR Gogo Shell Support");
            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
            gogoReg = bundleContext.registerService(ScrGogoCommand.class, gogoCmd, props);
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


    public void update( boolean infoAsService )
    {
        if (infoAsService)
        {
            if ( reg == null )
            {
                final Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put(Constants.SERVICE_DESCRIPTION, "SCR Info service");
                props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
                reg = bundleContext.registerService( ScrInfo.class, this, props );
            }
        }
        else
        {
            if ( reg != null )
            {
                reg.unregister();
                reg = null;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#list(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void list(final String bundleIdentifier, final PrintWriter out)
    {
        final List<ComponentDescriptionDTO> descriptions = new ArrayList<ComponentDescriptionDTO>();

        if (bundleIdentifier != null)
        {
            Bundle bundle = null;
            try
            {
                final long bundleId = Long.parseLong(bundleIdentifier);
                bundle = bundleContext.getBundle(bundleId);
            }
            catch (final NumberFormatException nfe)
            {
                // might be a bundle symbolic name
                final Bundle[] bundles = bundleContext.getBundles();
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
                descriptions.addAll(scrService.getComponentDescriptionDTOs(bundle));
                if (descriptions.isEmpty())
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
            descriptions.addAll(scrService.getComponentDescriptionDTOs());
            if (descriptions.isEmpty())
            {
                out.println("No components registered");
                return;
            }
        }

        Collections.sort( descriptions, DESCRIPTION_COMP);

        out.println(" BundleId Component Name Default State");
        out.println("    Component Id State      PIDs (Factory PID)");
        for(final ComponentDescriptionDTO desc : descriptions)
        {
            out.println( String.format( " [%1$4d]   %2$s  %3$s", desc.bundle.id, desc.name, desc.defaultEnabled ? "enabled" : "disabled" ) );
            final List<ComponentConfigurationDTO> configs = new ArrayList<ComponentConfigurationDTO>(this.scrService.getComponentConfigurationDTOs(desc));
            Collections.sort( configs, CONFIGURATION_COMP);
            for ( final ComponentConfigurationDTO component : configs )
            {
                final Object servicePid = component.properties.get(Constants.SERVICE_PID);
                final String factoryPid = (String)component.properties.get("service.factoryPid");

                final StringBuilder pid = new StringBuilder();
                if ( servicePid != null ) {
                    pid.append(servicePid);
                }
                if ( factoryPid != null ) {
                    pid.append(" (");
                    pid.append(factoryPid);
                    pid.append(" )");
                }
                out.println( String.format( "    [%1$4d] [%2$s] %3$s", component.id,
                          toStateString( component.state ),
                          pid.toString()) );
            }
        }
        out.flush();
   }

    /**
     * @see org.apache.felix.scr.impl.ScrInfo#info(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void info(final String componentId, final PrintWriter out)
    {
        final Result result = getComponentsFromArg(componentId, false);
        if (result.components.isEmpty())
        {
            return;
        }

        Collections.sort( result.components, DESCRIPTION_COMP );

        long bundleId = -1;

        for ( ComponentDescriptionDTO component : result.components )
        {
            if ( component.bundle.id != bundleId )
            {
                if ( bundleId != -1 )
                {
                    out.println();
                }
                bundleId = component.bundle.id;
                out.println(String.format("*** Bundle: %1$s (%2$d)", component.bundle.symbolicName, bundleId));
            }
            out.println( "Component Description:");
            out.print( "  Name: " );
            out.println( component.name );
            out.print( "  Implementation Class: " );
            out.println( component.implementationClass );
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
            if ( services != null && services.length > 0 )
            {
                out.println( "  Services: " );
                for ( String service: services )
                {
                    out.print( "    " );
                    out.println( service );
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
            propertyInfo(props, out, "  ", "Component Description");
            if ( result.configuration != null )
            {
                info(result.configuration, out);
            }
            else
            {
            	Collection<ComponentConfigurationDTO> componentConfigurationDTOs = scrService.getComponentConfigurationDTOs(component);
            	if (componentConfigurationDTOs.isEmpty())
            	{
            		out.println("  (No Component Configurations)");
            	}
            	else 
            	{
            		for (final ComponentConfigurationDTO cc: componentConfigurationDTOs)
            		{
            			info(cc, out);
            		}
            	}
            }
            out.println();
        }

        out.flush();
    }

    void propertyInfo(Map<String, Object> props, PrintWriter out, String prefix, String label)
    {
        if ( props != null )
        {
            out.print( prefix );
            out.print( label );
            out.println( " Properties:" );
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
            if ( serviceRefs.length > 0 )
            {
                out.print( "      Bound to:" );
                for ( ServiceReferenceDTO sr: serviceRefs )
                {
                    out.print( "        " );
                    out.println( sr.id );
                    propertyInfo(sr.properties, out, "      ", "Reference" );
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
            if ( serviceRefs.length > 0 )
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
                out.println( "      (no target services)" );
            }

        }
        propertyInfo( cc.properties, out, "    ", "Component Configuration" );
    }

    void change(final String componentIdentifier, final PrintWriter out, final boolean enable)
    {
        final Result result = getComponentsFromArg(componentIdentifier, true);

        for ( final ComponentDescriptionDTO component : result.components )
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
    }

    /**
     * @see org.apache.felix.scr.impl.ScrInfo#config(java.io.PrintStream)
     */
    public void config(final PrintWriter out)
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
        out.print("Info Service registered: ");
        out.println(scrConfiguration.infoAsService() ? "Supported" : "Unsupported");
        out.flush();
    }

    private String toStateString(final int state)
    {
        switch (state)
        {

        case (ComponentConfigurationDTO.UNSATISFIED_REFERENCE):
            return "unsatisfied reference";
        case (ComponentConfigurationDTO.ACTIVE):
            return "active      ";
        case (ComponentConfigurationDTO.SATISFIED):
            return "satisfied   ";
        case (ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION):
            return "unsatisfied config";
        default:
            return "unkown: " + state;
        }
    }

    private static final class Result {
        public List<ComponentDescriptionDTO> components = new ArrayList<ComponentDescriptionDTO>();
        public ComponentConfigurationDTO configuration;
    }

    private Result getComponentsFromArg(final String componentIdentifier, final boolean nameMatch)
    {
        final Pattern p = (componentIdentifier == null ? null : Pattern.compile(componentIdentifier));
        final Result result = new Result();

        for(final ComponentDescriptionDTO cmp : scrService.getComponentDescriptionDTOs())
        {
            if (componentIdentifier != null)
            {
                if ( p.matcher(cmp.name).matches() )
                {
                    result.components.add(cmp);
                }
                else if ( !nameMatch )
                {
                    boolean done = false;
                    for (final ComponentConfigurationDTO cfg: scrService.getComponentConfigurationDTOs(cmp))
                    {
                        if ( p.matcher( String.valueOf( cfg.id )).matches() )
                        {
                            result.components.add( cmp );
                            result.configuration = cfg;
                            done = true;
                            break;
                        }
                    }
                    if ( done )
                    {
                        break;
                    }
                }
            }
            else
            {
                result.components.add(cmp);
            }
        }
        if (componentIdentifier != null && result.components.isEmpty())
        {
            throw new IllegalArgumentException("No Component with name or configuration with ID matching " + componentIdentifier);
        }

        return result;
    }

}
