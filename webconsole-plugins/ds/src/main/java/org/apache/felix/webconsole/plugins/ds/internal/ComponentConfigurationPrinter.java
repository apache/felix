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
import org.apache.felix.webconsole.WebConsoleUtil;
import org.json.JSONException;
import org.json.JSONWriter;
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

    ComponentConfigurationPrinter(Object scrService, WebConsolePlugin plugin)
    {
        this.scrService = (ServiceComponentRuntime)scrService;
        this.plugin = plugin;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    public void print(PrintWriter pw, Format format, boolean isZip)
    {
        final List<ComponentDescriptionDTO> disabled = new ArrayList<ComponentDescriptionDTO>();
        final List<ComponentConfigurationDTO> configurations = new ArrayList<ComponentConfigurationDTO>();

        final Collection<ComponentDescriptionDTO> descs = scrService.getComponentDescriptionDTOs();
        for(final ComponentDescriptionDTO d : descs)
        {
            if ( !scrService.isComponentEnabled(d) )
            {
                disabled.add(d);
            }
            configurations.addAll(scrService.getComponentConfigurationDTOs(d));
        }
        Collections.sort(configurations, Util.COMPONENT_COMPARATOR);

        if (Format.JSON.equals(format))
        {
            try
            {
                printComponentsJson(pw, disabled, configurations, isZip);
            }
            catch (JSONException t)
            {
                // ignore
            }
        }
        else
        {
            printComponentsText(pw, disabled, configurations);
        }
    }

    private final void printComponentsJson(final PrintWriter pw,
        final List<ComponentDescriptionDTO> disabled,
        final List<ComponentConfigurationDTO> configurations,
        final boolean details) throws JSONException
    {
        final JSONWriter jw = new JSONWriter(pw);
        jw.object();
        jw.key("components"); //$NON-NLS-1$
        jw.array();

        // render disabled descriptions
        for(final ComponentDescriptionDTO cd : disabled)
        {
            plugin.disabledComponent(jw, cd);
        }
        // render configurations
        for (final ComponentConfigurationDTO cfg : configurations)
        {
            plugin.component(jw, cfg, details);
        }

        jw.endArray();
        jw.endObject();
    }

    private static final void printComponentsText(final PrintWriter pw,
            final List<ComponentDescriptionDTO> disabled,
            final List<ComponentConfigurationDTO> configurations)
    {
        if ( !disabled.isEmpty())
        {
            pw.println("Disabled components:");
        }
        for(final ComponentDescriptionDTO cd : disabled)
        {
            disabledComponent(pw, cd);
        }

        if (configurations.size() == 0)
        {
            pw.println("Status: No Component Configurations");
        }
        else
        {
            pw.println("Component Configurations:");
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
            }
        }
    }

    private static final void component(PrintWriter pw, final ComponentConfigurationDTO cfg)
    {

        pw.print(cfg.id);
        pw.print("=[");
        pw.print(cfg.description.name);
        pw.println("]");

        pw.println("  Bundle" + cfg.description.bundle.symbolicName + " ("
            + cfg.description.bundle.id + ")");
        pw.println("  State=" + toStateString(cfg.state));
        pw.println("  DefaultState="
            + (cfg.description.defaultEnabled ? "enabled" : "disabled"));
        pw.println("  Activation=" + (cfg.description.immediate ? "immediate" : "delayed"));

        listServices(pw, cfg);
        listReferences(pw, cfg);
        listProperties(pw, cfg);

        pw.println();
    }

    private static final void disabledComponent(PrintWriter pw, final ComponentDescriptionDTO cfg)
    {

        pw.print(cfg.name);

        pw.println("  Bundle" + cfg.bundle.symbolicName + " ("
            + cfg.bundle.id + ")");
        pw.println("  DefaultState="
            + (cfg.defaultEnabled ? "enabled" : "disabled"));
        pw.println("  Activation=" + (cfg.immediate ? "immediate" : "delayed"));
        pw.println();
    }

    private static void listServices(PrintWriter pw, final ComponentConfigurationDTO cfg)
    {
        String[] services = cfg.description.serviceInterfaces;
        if (services == null)
        {
            return;
        }

        pw.println("  ServiceType=" + cfg.description.scope);

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

    private static final void listReferences(PrintWriter pw, final ComponentConfigurationDTO cfg)
    {
        for(final ReferenceDTO dto : cfg.description.references)
        {
            final SatisfiedReferenceDTO satisfiedRef = findReference(cfg, dto.name);

            pw.println("  Reference=" + dto.name + ", "
                + (satisfiedRef != null ? "Satisfied" : "Unsatisfied"));

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
            }
        }
    }

    private static final void listProperties(PrintWriter pw, final ComponentConfigurationDTO cfg)
    {
        Map<String, Object> props = cfg.properties;
        if (props != null)
        {

            pw.println("  Properties=");
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
    }

    static final String toStateString(int state)
    {
        switch (state)
        {
            case ComponentConfigurationDTO.ACTIVE:
                return "active";
            case ComponentConfigurationDTO.SATISFIED:
                return "satisfied";
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
                return "unsatisfied (configuration)";
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
                return "unsatisfied (reference)";
            default:
                return String.valueOf(state);
        }
    }
}
