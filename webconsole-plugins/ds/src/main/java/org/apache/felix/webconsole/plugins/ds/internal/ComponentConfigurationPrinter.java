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
package org.apache.felix.webconsole.plugins.ds.internal;

<<<<<<< HEAD
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

/**
 * ComponentConfigurationPrinter prints the available SCR services. 
 */
class ComponentConfigurationPrinter implements ConfigurationPrinter
{

    private final ScrService scrService;

    ComponentConfigurationPrinter(Object scrService)
    {
        this.scrService = (ScrService)scrService;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public String getTitle()
    {
        return "Declarative Services Components";
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw)
    {
        printComponents(pw, scrService.getComponents());
    }

    private static final void printComponents(final PrintWriter pw,
        final Component[] components)
    {
        if (components == null || components.length == 0)
        {
            pw.println("Status: No Components Registered");
        }
        else
        {
            // order components by id
            TreeMap componentMap = new TreeMap();
            for (int i = 0; i < components.length; i++)
            {
                Component component = components[i];
                componentMap.put(new Long(component.getId()), component);
            }

            // render components
            for (Iterator ci = componentMap.values().iterator(); ci.hasNext();)
            {
                Component component = (Component) ci.next();
                component(pw, component);
=======
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;

/**
 * ComponentConfigurationPrinter prints the available SCR services.
 */
class ComponentConfigurationPrinter implements InventoryPrinter
{

    private final ServiceComponentRuntime scrService;
    private final WebConsolePlugin plugin;

    ComponentConfigurationPrinter(ServiceComponentRuntime scrService, WebConsolePlugin plugin)
    {
        this.scrService = scrService;
        this.plugin = plugin;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    public void print(PrintWriter pw, Format format, boolean isZip)
    {
        final List<ComponentDescriptionDTO> noConfig = new ArrayList<ComponentDescriptionDTO>();
        final List<ComponentDescriptionDTO> disabled = new ArrayList<ComponentDescriptionDTO>();
        final List<ComponentConfigurationDTO> configurations = new ArrayList<ComponentConfigurationDTO>();

        final Collection<ComponentDescriptionDTO> descs = scrService.getComponentDescriptionDTOs();
        for(final ComponentDescriptionDTO d : descs)
        {
            if ( !scrService.isComponentEnabled(d) )
            {
                disabled.add(d);
            }
            else
            {
                final Collection<ComponentConfigurationDTO> configs = scrService.getComponentConfigurationDTOs(d);
                if ( configs.isEmpty() )
                {
                    noConfig.add(d);
                }
                else
                {
                    configurations.addAll(configs);
                }
            }
        }
        Collections.sort(configurations, Util.COMPONENT_COMPARATOR);

        if (Format.JSON.equals(format))
        {
            disabled.addAll(noConfig);
            try
            {
                printComponentsJson(pw, disabled, configurations, isZip);
            } catch (IOException ignore)
            {
                // ignore
            }
        }
        else
        {
            printComponentsText(pw, disabled, noConfig, configurations);
        }
    }

    private final void printComponentsJson(final PrintWriter pw,
            final List<ComponentDescriptionDTO> disabled,
            final List<ComponentConfigurationDTO> configurations,
            final boolean details) throws IOException
    {
        final JSONWriter jw = new JSONWriter(pw);
        jw.object();
        jw.key("components"); //$NON-NLS-1$
        jw.array();

        // render disabled descriptions
        for(final ComponentDescriptionDTO cd : disabled)
        {
            plugin.component(jw, cd, null, details);
        }
        // render configurations
        for (final ComponentConfigurationDTO cfg : configurations)
        {
            plugin.component(jw, cfg.description, cfg, details);
        }

        jw.endArray();
        jw.endObject();
    }

    private static final String SEP = "----------------------------------------------------------------------";

    private static final void printComponentsText(final PrintWriter pw,
            final List<ComponentDescriptionDTO> disabled,
            final List<ComponentDescriptionDTO> noConfig,
            final List<ComponentConfigurationDTO> configurations)
    {
        if ( !disabled.isEmpty())
        {
            pw.println(SEP);
            pw.println("Disabled components:");
            pw.println(SEP);
            for(final ComponentDescriptionDTO cd : disabled)
            {
                disabledComponent(pw, cd);
            }
            pw.println();
        }
        if ( !noConfig.isEmpty())
        {
            pw.println(SEP);
            pw.println("Components with missing configuration in Config Admin:");
            pw.println(SEP);
            for(final ComponentDescriptionDTO cd : noConfig)
            {
                disabledComponent(pw, cd);
            }
            pw.println();
        }

        pw.println(SEP);
        if (configurations.isEmpty())
        {
            pw.println("Status: No Component Configurations");
            pw.println(SEP);
        }
        else
        {
            pw.println("Component Configurations:");
            pw.println(SEP);
            // order components by id
            TreeMap<Long, ComponentConfigurationDTO> componentMap = new TreeMap<Long, ComponentConfigurationDTO>();
            for(final ComponentConfigurationDTO cfg : configurations)
            {
                componentMap.put(new Long(cfg.id), cfg);
            }

            // render components
            for (final ComponentConfigurationDTO cfg : componentMap.values())
            {
                component(pw, cfg);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

<<<<<<< HEAD
    private static final void component(PrintWriter pw, Component component)
    {

        pw.print(component.getId());
        pw.print("=[");
        pw.print(component.getName());
        pw.println("]");

        pw.println("  Bundle" + component.getBundle().getSymbolicName() + " ("
            + component.getBundle().getBundleId() + ")");
        pw.println("  State=" + toStateString(component.getState()));
        pw.println("  DefaultState="
            + (component.isDefaultEnabled() ? "enabled" : "disabled"));
        pw.println("  Activation=" + (component.isImmediate() ? "immediate" : "delayed"));

        listServices(pw, component);
        listReferences(pw, component);
        listProperties(pw, component);
=======
    private static final void component(PrintWriter pw, final ComponentConfigurationDTO cfg)
    {

        pw.print(cfg.id);
        pw.print("=[");
        pw.print(cfg.description.name);
        pw.println("]");

        pw.println("  Bundle=" + cfg.description.bundle.symbolicName + " ("
                + cfg.description.bundle.id + ")");
        pw.println("  State=" + toStateString(cfg.state));
        if ( cfg.state == ComponentConfigurationDTO.FAILED_ACTIVATION && cfg.failure != null ) {
            pw.println("  Failure=" + cfg.failure);
        }
        pw.println("  DefaultState="
                + (cfg.description.defaultEnabled ? "enabled" : "disabled"));
        pw.println("  Activation=" + (cfg.description.immediate ? "immediate" : "delayed"));
        pw.println("  ConfigurationPolicy=" + cfg.description.configurationPolicy);

        listServices(pw, cfg.description);
        if ( cfg.service != null ) {
            pw.println("  ServiceId=" + String.valueOf(cfg.service.id));
        }

        listReferences(pw, cfg.description, cfg);
        listProperties(pw, cfg.description, cfg);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        pw.println();
    }

<<<<<<< HEAD
    private static void listServices(PrintWriter pw, Component component)
    {
        String[] services = component.getServices();
=======
    private static final void disabledComponent(PrintWriter pw, final ComponentDescriptionDTO description)
    {

        pw.println(description.name);

        pw.println("  Bundle=" + description.bundle.symbolicName + " ("
                + description.bundle.id + ")");
        pw.println("  DefaultState="
                + (description.defaultEnabled ? "enabled" : "disabled"));
        pw.println("  Activation=" + (description.immediate ? "immediate" : "delayed"));
        pw.println("  ConfigurationPolicy=" + description.configurationPolicy);

        listServices(pw, description);
        listReferences(pw, description, null);
        listProperties(pw, description, null);

        pw.println();
    }

    private static void listServices(PrintWriter pw, final ComponentDescriptionDTO cfg)
    {
        String[] services = cfg.serviceInterfaces;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (services == null)
        {
            return;
        }

<<<<<<< HEAD
        pw.println("  ServiceType="
            + (component.isServiceFactory() ? "service factory" : "service"));
=======
        if ( cfg.scope != null ) {
            pw.println("  ServiceType=" + cfg.scope);
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < services.length; i++)
        {
            if (i > 0)
            {
                buf.append(", ");
            }
            buf.append(services[i]);
        }

        pw.println("  Services=" + buf);
    }

<<<<<<< HEAD
    private static final void listReferences(PrintWriter pw, Component component)
    {
        Reference[] refs = component.getReferences();
        if (refs != null)
        {
            for (int i = 0; i < refs.length; i++)
            {

                pw.println("  Reference=" + refs[i].getName() + ", "
                    + (refs[i].isSatisfied() ? "Satisfied" : "Unsatisfied"));

                pw.println("    Service Name: " + refs[i].getServiceName());

                if (refs[i].getTarget() != null)
                {
                    pw.println("  Target Filter: " + refs[i].getTarget());
                }

                pw.println("    Multiple: "
                    + (refs[i].isMultiple() ? "multiple" : "single"));
                pw.println("    Optional: "
                    + (refs[i].isOptional() ? "optional" : "mandatory"));
                pw.println("    Policy: " + (refs[i].isStatic() ? "static" : "dynamic"));

                // list bound services
                ServiceReference[] boundRefs = refs[i].getServiceReferences();
                if (boundRefs != null && boundRefs.length > 0)
                {
                    for (int j = 0; j < boundRefs.length; j++)
                    {
                        pw.print("    Bound Service: ID ");
                        pw.print(boundRefs[j].getProperty(Constants.SERVICE_ID));

                        String name = (String) boundRefs[j].getProperty(ComponentConstants.COMPONENT_NAME);
                        if (name == null)
                        {
                            name = (String) boundRefs[j].getProperty(Constants.SERVICE_PID);
                            if (name == null)
                            {
                                name = (String) boundRefs[j].getProperty(Constants.SERVICE_DESCRIPTION);
                            }
                        }
                        if (name != null)
                        {
                            pw.print(" (");
                            pw.print(name);
                            pw.print(")");
                        }
                        pw.println();
                    }
                }
                else
                {
                    pw.println("    No Services bound");
                }
=======
    private static SatisfiedReferenceDTO findReference(final ComponentConfigurationDTO component, final String name)
    {
        for(final SatisfiedReferenceDTO dto : component.satisfiedReferences)
        {
            if ( dto.name.equals(name))
            {
                return dto;
            }
        }
        return null;
    }

    private static final void listReferences(PrintWriter pw,
            final ComponentDescriptionDTO description,
            final ComponentConfigurationDTO configuration)
    {
        for(final ReferenceDTO dto : description.references)
        {
            final SatisfiedReferenceDTO satisfiedRef = configuration == null ? null : findReference(configuration, dto.name);

            pw.print("  Reference=");
            pw.print(dto.name);
            if ( configuration != null )
            {
                pw.print(", ");
                pw.print(satisfiedRef != null ? "Satisfied" : "Unsatisfied");
            }
            pw.println();
            pw.println("    Service Name: " + dto.interfaceName);

            if (dto.target != null)
            {
                pw.println("  Target Filter: " + dto.target);
            }

            pw.println("    Cardinality: " + dto.cardinality);
            pw.println("    Policy: " + dto.policy);
            pw.println("    Policy Option: " + dto.policyOption);

            // list bound services
            if ( satisfiedRef != null )
            {
                for(final ServiceReferenceDTO sref : satisfiedRef.boundServices )
                {
                    pw.print("    Bound Service: ID ");
                    pw.print(sref.properties.get(Constants.SERVICE_ID));

                    String name = (String) sref.properties.get(ComponentConstants.COMPONENT_NAME);
                    if (name == null)
                    {
                        name = (String) sref.properties.get(Constants.SERVICE_PID);
                        if (name == null)
                        {
                            name = (String) sref.properties.get(Constants.SERVICE_DESCRIPTION);
                        }
                    }
                    if (name != null)
                    {
                        pw.print(" (");
                        pw.print(name);
                        pw.print(")");
                    }
                    pw.println();
                }
            }
            else
            {
                pw.println("    No Services bound");
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

<<<<<<< HEAD
    private static final void listProperties(PrintWriter pw, Component component)
    {
        Dictionary props = component.getProperties();
=======
    private static final void listProperties(PrintWriter pw,
            final ComponentDescriptionDTO description,
            final ComponentConfigurationDTO cfg)
    {
        Map<String, Object> props = cfg == null ? description.properties : cfg.properties;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (props != null)
        {

            pw.println("  Properties=");
<<<<<<< HEAD
            TreeSet keys = new TreeSet(Util.list(props.keys()));
            for (Iterator ki = keys.iterator(); ki.hasNext();)
            {
                String key = (String) ki.next();
=======
            TreeSet<String> keys = new TreeSet<String>(props.keySet());
            for (Iterator<String> ki = keys.iterator(); ki.hasNext();)
            {
                String key = ki.next();
                Object value = props.get(key);
                value = WebConsoleUtil.toString(value);
                if (value.getClass().isArray())
                {
                    value = Arrays.asList((Object[]) value);
                }
                pw.println("    " + key + "=" + value);
            }
        }
        if ( cfg == null && description.factoryProperties != null ) {
            pw.println("  FactoryProperties=");
            TreeSet<String> keys = new TreeSet<String>(description.factoryProperties.keySet());
            for (Iterator<String> ki = keys.iterator(); ki.hasNext();)
            {
                String key = ki.next();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                Object value = props.get(key);
                value = WebConsoleUtil.toString(value);
                if (value.getClass().isArray())
                {
                    value = Arrays.asList((Object[]) value);
                }
                pw.println("    " + key + "=" + value);
            }
        }
    }

    static final String toStateString(int state)
    {
        switch (state)
        {
<<<<<<< HEAD
            case Component.STATE_DISABLED:
                return "disabled";
            case Component.STATE_ENABLED:
                return "enabled";
            case Component.STATE_UNSATISFIED:
                return "unsatisfied";
            case Component.STATE_ACTIVATING:
                return "activating";
            case Component.STATE_ACTIVE:
                return "active";
            case Component.STATE_REGISTERED:
                return "registered";
            case Component.STATE_FACTORY:
                return "factory";
            case Component.STATE_DEACTIVATING:
                return "deactivating";
            case Component.STATE_DESTROYED:
                return "destroyed";
            default:
                return String.valueOf(state);
=======
        case ComponentConfigurationDTO.ACTIVE:
            return "active";
        case ComponentConfigurationDTO.SATISFIED:
            return "satisfied";
        case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
            return "unsatisfied (configuration)";
        case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
            return "unsatisfied (reference)";
        case ComponentConfigurationDTO.FAILED_ACTIVATION:
            return "failed activation";
        default:
            return String.valueOf(state);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }
}
