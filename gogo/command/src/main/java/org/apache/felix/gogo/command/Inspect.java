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
package org.apache.felix.gogo.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class Inspect
{
    public static final String NONSTANDARD_SERVICE_NAMESPACE = "service";

    public static final String CAPABILITY = "capability";
    public static final String REQUIREMENT = "requirement";

    private static final String EMPTY_MESSAGE = "[EMPTY]";
    private static final String UNUSED_MESSAGE = "[UNUSED]";
    private static final String UNRESOLVED_MESSAGE = "[UNRESOLVED]";

    private final BundleContext m_bc;

    public Inspect(BundleContext bc)
    {
        m_bc = bc;
    }

    @Descriptor("inspects bundle capabilities and requirements")
    public String inspect(
        @Descriptor("('capability' | 'requirement')") String direction,
        @Descriptor("(<namespace> | 'service')") String namespace,
        @Descriptor("target bundles") Bundle[] bundles)
    {
        return inspect(m_bc, direction, namespace, bundles);
    }

    private static String inspect(
        BundleContext bc, String direction, String namespace, Bundle[] bundles)
    {
        // Verify arguments.
        if (isValidDirection(direction))
        {
            bundles = ((bundles == null) || (bundles.length == 0))
                ? bc.getBundles() : bundles;

            if (CAPABILITY.startsWith(direction))
            {
                return printCapabilities(bc, Util.parseSubstring(namespace), bundles);
            }
            else
            {
                return printRequirements(bc, Util.parseSubstring(namespace), bundles);
            }
        }

        return "Invalid argument: " + direction;
    }

    public static String printCapabilities(
        BundleContext bc, List<String> namespace, Bundle[] bundles)
    {
        try (Formatter f = new Formatter()) {
            for (Bundle b : bundles)
            {
                // Print out any matching generic capabilities.
                BundleWiring wiring = b.adapt(BundleWiring.class);
                if (wiring != null)
                {
                    String title = b + " provides:";
                    f.format("%s%n%s%n", title, Util.getUnderlineString(title.length()));

                    // Print generic capabilities for matching namespaces.
                    boolean matches = printMatchingCapabilities(wiring, namespace, f);

                    // Handle service capabilities separately, since they aren't part
                    // of the generic model in OSGi.
                    if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE))
                    {
                        matches |= printServiceCapabilities(b, f);
                    }

                    // If there were no capabilities for the specified namespace,
                    // then say so.
                    if (!matches)
                    {
                        f.format("%s %s%n", Util.unparseSubstring(namespace), EMPTY_MESSAGE);
                    }
                }
                else
                {
                    f.format("Bundle %s is not resolved.",
                        b.getBundleId());
                }
            }
            return f.toString();
        }
    }

    private static boolean printMatchingCapabilities(BundleWiring wiring, List<String> namespace, Formatter f)
    {
        List<BundleWire> wires = wiring.getProvidedWires(null);
        Map<BundleCapability, List<BundleWire>> aggregateCaps =
            aggregateCapabilities(namespace, wires);
        List<BundleCapability> allCaps = wiring.getCapabilities(null);
        boolean matches = false;
        for (BundleCapability cap : allCaps)
        {
            if (matchNamespace(namespace, cap.getNamespace()))
            {
                if ("osgi.service".equals(cap.getNamespace())) {
                    continue;
                }
                matches = true;
                List<BundleWire> dependents = aggregateCaps.get(cap);
                Object keyAttr =
                    cap.getAttributes().get(cap.getNamespace());
                if ("osgi.native".equals(cap.getNamespace()))
                {
                    f.format("%s with properties:%n", cap.getNamespace());
                    List<Entry<String, Object>> sortedEntries = 
                            new ArrayList<Entry<String, Object>>(cap.getAttributes().entrySet());
                    Collections.sort(sortedEntries, new Comparator<Entry<String, Object>>() {
                        @Override
                        public int compare(Entry<String, Object> o1, Entry<String, Object> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                        }});
                    
                    for (Entry<String, Object> e : sortedEntries) {
                        f.format("   %s = %s%n", e.getKey(), e.getValue());
                    }
                    
                    if (dependents != null)
                    {
                        f.format("   required by:%n");
                        for (BundleWire wire : dependents) {
                            f.format("      %s%n", wire.getRequirerWiring().getBundle());
                        }
                    }
                    else
                    {
                        f.format("   %s%n", UNUSED_MESSAGE);
                    }
                }
                else if (dependents != null)
                {
                    if (keyAttr != null)
                    {
                        f.format("%s; %s %s required by:%n",
                            cap.getNamespace(),
                            format(keyAttr),
                            getVersionFromCapability(cap));
                    }
                    else
                    {
                        f.format("%s required by:%n", cap.toString());
                    }
                    for (BundleWire wire : dependents)
                    {
                        f.format("   %s%n", wire.getRequirerWiring().getBundle());
                    }
                }
                else if (keyAttr != null)
                {
                    f.format("%s; %s %s %s%n",
                        cap.getNamespace(),
                        format(keyAttr),
                        getVersionFromCapability(cap),
                        UNUSED_MESSAGE);
                }
                else
                {
                    f.format("%s %s%n", cap, UNUSED_MESSAGE);
                }
            }
        }
        return matches;
    }

    private static String format(Object object) {
        String retVal;
        if (object.getClass().isArray() || object instanceof Collection) {        	
            StringBuffer buffer = new StringBuffer();
            @SuppressWarnings("rawtypes")
            Iterable formatTarget = object.getClass().isArray() ? Arrays.asList(object) : (Iterable) object;
            for (Object elem : formatTarget) {
                if (buffer.length()>0) {
                    buffer.append(',');
                }
                buffer.append(elem.toString());
            }
            retVal = buffer.toString();
        }
        else {
            retVal = String.valueOf(object);
        }
        return retVal;
    }
    
    private static Map<BundleCapability, List<BundleWire>> aggregateCapabilities(
        List<String> namespace, List<BundleWire> wires)
    {
        // Aggregate matching capabilities.
        Map<BundleCapability, List<BundleWire>> map =
                new HashMap<>();
        for (BundleWire wire : wires)
        {
            if (matchNamespace(namespace, wire.getCapability().getNamespace()))
            {
                List<BundleWire> dependents = map.get(wire.getCapability());
                if (dependents == null)
                {
                    dependents = new ArrayList<>();
                    map.put(wire.getCapability(), dependents);
                }
                dependents.add(wire);
            }
        }
        return map;
    }

    static boolean printServiceCapabilities(Bundle b, Formatter f)
    {
        boolean matches = false;

        try
        {
            ServiceReference<?>[] refs = b.getRegisteredServices();

            if ((refs != null) && (refs.length > 0))
            {
                matches = true;
                // Print properties for each service.
                for (ServiceReference<?> ref : refs)
                {
                    // Print object class with "namespace".
                    f.format("%s; %s with properties:%n",
                        NONSTANDARD_SERVICE_NAMESPACE,
                        Util.getValueString(ref.getProperty("objectClass")));
                    // Print service properties.
                    String[] keys = ref.getPropertyKeys();
                    for (String key : keys)
                    {
                        if (!key.equalsIgnoreCase(Constants.OBJECTCLASS))
                        {
                            Object v = ref.getProperty(key);
                            f.format("   %s = %s%n", key, Util.getValueString(v));
                        }
                    }
                    Bundle[] users = ref.getUsingBundles();
                    if ((users != null) && (users.length > 0))
                    {
                        f.format("   Used by:%n");
                        for (Bundle user : users)
                        {
                            f.format("      %s%n", user);
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            f.format("%s%n", ex.toString());
        }

        return matches;
    }

    public static String printRequirements(
        BundleContext bc, List<String> namespace, Bundle[] bundles)
    {
        try (Formatter f = new Formatter()) {
            for (Bundle b : bundles)
            {
                // Print out any matching generic requirements.
                BundleWiring wiring = b.adapt(BundleWiring.class);
                if (wiring != null)
                {
                    String title = b + " requires:";
                    f.format("%s%n%s%n", title, Util.getUnderlineString(title.length()));
                    boolean matches = printMatchingRequirements(wiring, namespace, f);

                    // Handle service requirements separately, since they aren't part
                    // of the generic model in OSGi.
                    if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE))
                    {
                        matches |= printServiceRequirements(b, f);
                    }

                    // If there were no requirements for the specified namespace,
                    // then say so.
                    if (!matches)
                    {
                        f.format("%s %s%n", Util.unparseSubstring(namespace), EMPTY_MESSAGE);
                    }
                }
                else
                {
                    f.format("Bundle %s is not resolved.%n",
                        b.getBundleId());
                }
            }
            return f.toString();
        }
    }

    private static boolean printMatchingRequirements(BundleWiring wiring, List<String> namespace, Formatter f)
    {
        List<BundleWire> wires = wiring.getRequiredWires(null);
        Map<BundleRequirement, List<BundleWire>> aggregateReqs =
            aggregateRequirements(namespace, wires);
        List<BundleRequirement> allReqs = wiring.getRequirements(null);
        boolean matches = false;
        for (BundleRequirement req : allReqs)
        {
            if (matchNamespace(namespace, req.getNamespace()))
            {
                matches = true;
                List<BundleWire> providers = aggregateReqs.get(req);
                if (providers != null)
                {
                    f.format("%s; %s resolved by:%n",
                        req.getNamespace(),
                        req.getDirectives().get(Constants.FILTER_DIRECTIVE));
                    for (BundleWire wire : providers)
                    {
                        String msg;
                        Object keyAttr =
                            wire.getCapability().getAttributes()
                                .get(wire.getCapability().getNamespace());
                        if (keyAttr != null)
                        {
                            msg = wire.getCapability().getNamespace()
                                + "; "
                                + keyAttr
                                + " "
                                + getVersionFromCapability(wire.getCapability());
                        }
                        else
                        {
                            msg = wire.getCapability().toString();
                        }
                        f.format("   %s from %s%n", msg, wire.getProviderWiring().getBundle());
                    }
                }
                else
                {
                    f.format("%s; %s %s%n",
                        req.getNamespace(),
                        req.getDirectives().get(Constants.FILTER_DIRECTIVE),
                        UNRESOLVED_MESSAGE);
                }
            }
        }
        return matches;
    }

    private static Map<BundleRequirement, List<BundleWire>> aggregateRequirements(
        List<String> namespace, List<BundleWire> wires)
    {
        // Aggregate matching capabilities.
        Map<BundleRequirement, List<BundleWire>> map =
                new HashMap<>();
        for (BundleWire wire : wires)
        {
            if (matchNamespace(namespace, wire.getRequirement().getNamespace()))
            {
                List<BundleWire> providers = map.get(wire.getRequirement());
                if (providers == null)
                {
                    providers = new ArrayList<>();
                    map.put(wire.getRequirement(), providers);
                }
                providers.add(wire);
            }
        }
        return map;
    }

    static boolean printServiceRequirements(Bundle b, Formatter f)
    {
        boolean matches = false;

        try
        {
            ServiceReference<?>[] refs = b.getServicesInUse();

            if ((refs != null) && (refs.length > 0))
            {
                matches = true;
                // Print properties for each service.
                for (ServiceReference<?> ref : refs)
                {
                    // Print object class with "namespace".
                    f.format("%s; %s provided by:%n   %s%n",
                        NONSTANDARD_SERVICE_NAMESPACE,
                        Util.getValueString(ref.getProperty("objectClass")),
                        ref.getBundle());
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.toString());
        }

        return matches;
    }

    private static String getVersionFromCapability(BundleCapability c)
    {
        Object o = c.getAttributes().get(Constants.VERSION_ATTRIBUTE);
        if (o == null)
        {
            o = c.getAttributes().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return (o == null) ? "" : o.toString();
    }

    private static boolean matchNamespace(List<String> namespace, String actual)
    {
        return Util.compareSubstring(namespace, actual);
    }

    private static boolean isValidDirection(String direction)
    {
        return (CAPABILITY.startsWith(direction) || REQUIREMENT.startsWith(direction));
    }
}