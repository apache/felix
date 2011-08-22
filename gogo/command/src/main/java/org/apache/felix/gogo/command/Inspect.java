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
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.Map.Entry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

public class Inspect
{
    public static final String LEGACY_PACKAGE_NAMESPACE = "package";
    public static final String LEGACY_BUNDLE_NAMESPACE = "bundle";
    public static final String LEGACY_HOST_NAMESPACE = "host";
    public static final String NONSTANDARD_SERVICE_NAMESPACE = "service";

    public static final String CAPABILITY = "capability";
    public static final String REQUIREMENT = "requirement";

    private static final String EMPTY_MESSAGE = "[EMPTY]";
    private static final String UNUSED_MESSAGE = "[UNUSED]";
    private static final String UNRESOLVED_MESSAGE = "[UNRESOLVED]";

    public static void in(
        BundleContext bc, String direction, String namespace, Bundle[] bundles)
    {
        // Verify arguments.
        if (isValidDirection(direction))
        {
            bundles = ((bundles == null) || (bundles.length == 0))
                ? bc.getBundles() : bundles;

            if (CAPABILITY.startsWith(direction))
            {
                printCapabilities(bc, Util.parseSubstring(namespace), bundles);
            }
            else
            {
                printRequirements(bc, Util.parseSubstring(namespace), bundles);
            }
        }
        else
        {
            if (!isValidDirection(direction))
            {
                System.out.println("Invalid argument: " + direction);
            }
        }
    }

    public static void printCapabilities(
        BundleContext bc, List<String> namespace, Bundle[] bundles)
    {
        boolean separatorNeeded = false;
        for (Bundle b : bundles)
        {
            // Print out any matching generic capabilities.
            BundleWiring wiring = b.adapt(BundleWiring.class);
            if (wiring != null)
            {
                if (separatorNeeded)
                {
                    System.out.println("");
                }
                String title = b + " provides:";
                System.out.println(title);
                System.out.println(Util.getUnderlineString(title.length()));

                // Print generic capabilities for matching namespaces.
                boolean matches = printMatchingCapabilities(wiring, namespace);

                // Handle service capabilities separately, since they aren't part
                // of the generic model in OSGi.
                if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE))
                {
                    matches |= printServiceCapabilities(b);
                }

                // If there were no capabilities for the specified namespace,
                // then say so.
                if (!matches)
                {
                    System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
                }
                separatorNeeded = true;
            }
            else
            {
                System.out.println("Bundle "
                    + b.getBundleId()
                    + " is apparently not resolved.");
            }
        }
    }

    private static boolean printMatchingCapabilities(BundleWiring wiring, List<String> namespace)
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
                matches = true;
                List<BundleWire> dependents = aggregateCaps.get(cap);
                Object keyAttr =
                    cap.getAttributes().get(cap.getNamespace());
                if (dependents != null)
                {
                    String msg;
                    if (keyAttr != null)
                    {
                        msg = cap.getNamespace()
                            + "; "
                            + keyAttr
                            + getVersionFromCapability(cap);
                    }
                    else
                    {
                        msg = cap.toString();
                    }
                    msg = msg + " required by:";
                    System.out.println(msg);
                    for (BundleWire wire : dependents)
                    {
                        System.out.println("   " + wire.getRequirerWiring().getBundle());
                    }
                }
                else if (keyAttr != null)
                {
                    System.out.println(cap.getNamespace()
                        + "; "
                        + cap.getAttributes().get(cap.getNamespace())
                        + getVersionFromCapability(cap)
                        + " "
                        + UNUSED_MESSAGE);
                }
                else
                {
                    System.out.println(cap + " " + UNUSED_MESSAGE);
                }
            }
        }
        return matches;
    }

    private static Map<BundleCapability, List<BundleWire>> aggregateCapabilities(
        List<String> namespace, List<BundleWire> wires)
    {
        // Aggregate matching capabilities.
        Map<BundleCapability, List<BundleWire>> map =
            new HashMap<BundleCapability, List<BundleWire>>();
        for (BundleWire wire : wires)
        {
            if (matchNamespace(namespace, wire.getCapability().getNamespace()))
            {
                List<BundleWire> dependents = map.get(wire.getCapability());
                if (dependents == null)
                {
                    dependents = new ArrayList<BundleWire>();
                    map.put(wire.getCapability(), dependents);
                }
                dependents.add(wire);
            }
        }
        return map;
    }

    private static boolean printServiceCapabilities(Bundle b)
    {
        boolean matches = false;

        try
        {
            ServiceReference[] refs = b.getRegisteredServices();

            if ((refs != null) && (refs.length > 0))
            {
                matches = true;
                // Print properties for each service.
                for (ServiceReference ref : refs)
                {
                    // Print object class with "namespace".
                    System.out.println(
                        NONSTANDARD_SERVICE_NAMESPACE
                        + "; "
                        + Util.getValueString(ref.getProperty("objectClass"))
                        + " with properties:");
                    // Print service properties.
                    String[] keys = ref.getPropertyKeys();
                    for (String key : keys)
                    {
                        if (!key.equalsIgnoreCase(Constants.OBJECTCLASS))
                        {
                            Object v = ref.getProperty(key);
                            System.out.println("   "
                                + key + " = " + Util.getValueString(v));
                        }
                    }
                    Bundle[] users = ref.getUsingBundles();
                    if ((users != null) && (users.length > 0))
                    {
                        System.out.println("   Used by:");
                        for (Bundle user : users)
                        {
                            System.out.println("      " + user);
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.toString());
        }

        return matches;
    }

    public static void printRequirements(
        BundleContext bc, List<String> namespace, Bundle[] bundles)
    {
        boolean separatorNeeded = false;
        for (Bundle b : bundles)
        {
            // Print out any matching generic requirements.
            BundleWiring wiring = b.adapt(BundleWiring.class);
            if (wiring != null)
            {
                if (separatorNeeded)
                {
                    System.out.println("");
                }
                String title = b + " requires:";
                System.out.println(title);
                System.out.println(Util.getUnderlineString(title.length()));
                boolean matches = printMatchingRequirements(wiring, namespace);

                // Handle service requirements separately, since they aren't part
                // of the generic model in OSGi.
                if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE))
                {
                    matches |= printServiceRequirements(b);
                }

                // If there were no requirements for the specified namespace,
                // then say so.
                if (!matches)
                {
                    System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
                }
                separatorNeeded = true;
            }
            else
            {
                System.out.println("Bundle "
                    + b.getBundleId()
                    + " is apparently not resolved.");
            }
        }
    }

    private static boolean printMatchingRequirements(BundleWiring wiring, List<String> namespace)
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
                    System.out.println(
                        req.getNamespace()
                        + "; "
                        + req.getDirectives().get(Constants.FILTER_DIRECTIVE)
                        + " resolved by:");
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
                                + getVersionFromCapability(wire.getCapability());
                        }
                        else
                        {
                            msg = wire.getCapability().toString();
                        }
                        msg = "   " + msg + " from "
                            + wire.getProviderWiring().getBundle();
                        System.out.println(msg);
                    }
                }
                else
                {
                    System.out.println(
                        req.getNamespace()
                        + "; "
                        + req.getDirectives().get(Constants.FILTER_DIRECTIVE)
                        + " "
                        + UNRESOLVED_MESSAGE);
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
            new HashMap<BundleRequirement, List<BundleWire>>();
        for (BundleWire wire : wires)
        {
            if (matchNamespace(namespace, wire.getRequirement().getNamespace()))
            {
                List<BundleWire> providers = map.get(wire.getRequirement());
                if (providers == null)
                {
                    providers = new ArrayList<BundleWire>();
                    map.put(wire.getRequirement(), providers);
                }
                providers.add(wire);
            }
        }
        return map;
    }

    private static boolean printServiceRequirements(Bundle b)
    {
        boolean matches = false;

        try
        {
            ServiceReference[] refs = b.getServicesInUse();

            if ((refs != null) && (refs.length > 0))
            {
                matches = true;
                // Print properties for each service.
                for (ServiceReference ref : refs)
                {
                    // Print object class with "namespace".
                    System.out.println(
                        NONSTANDARD_SERVICE_NAMESPACE
                        + "; "
                        + Util.getValueString(ref.getProperty("objectClass"))
                        + " provided by:");
                    System.out.println("   " + ref.getBundle());
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.toString());
        }

        return matches;
    }

    public static void inspect(
        BundleContext bc, String direction, String namespace, Bundle[] bundles)
    {
        // Verify arguments.
        if (isValidDirection(direction))
        {
            bundles = ((bundles == null) || (bundles.length == 0))
                ? bc.getBundles() : bundles;

            if (CAPABILITY.startsWith(direction))
            {
                printNonstandardCapabilities(bc, Util.parseSubstring(namespace), bundles);
            }
            else
            {
                printNonstandardRequirements(bc, Util.parseSubstring(namespace), bundles);
            }
        }
        else
        {
            if (!isValidDirection(direction))
            {
                System.out.println("Invalid argument: " + direction);
            }
        }
    }

    private static void printNonstandardCapabilities(
        BundleContext bc, List<String> namespace, Bundle[] bundles)
    {
        boolean separatorNeeded = false;
        for (Bundle b : bundles)
        {
            if (separatorNeeded)
            {
                System.out.println("");
            }
            String title = b + " provides:";
            System.out.println(title);
            System.out.println(Util.getUnderlineString(title.length()));
            boolean matches = false;

            if (matchNamespace(namespace, LEGACY_BUNDLE_NAMESPACE))
            {
                matches |= printRequiringBundles(bc, b);
            }
            if (matchNamespace(namespace, LEGACY_HOST_NAMESPACE))
            {
                matches |= printHostedFragments(bc, b);
            }
            if (matchNamespace(namespace, LEGACY_PACKAGE_NAMESPACE))
            {
                matches |= printExportedPackages(bc, b);
            }
            if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE))
            {
                matches |= printServiceCapabilities(b);
            }

            // If there were no capabilities for the specified namespace,
            // then say so.
            if (!matches)
            {
                System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
            }
            separatorNeeded = true;
        }
    }

    private static void printNonstandardRequirements(
        BundleContext bc, List<String> namespace, Bundle[] bundles)
    {
        boolean separatorNeeded = false;
        for (Bundle b : bundles)
        {
            if (separatorNeeded)
            {
                System.out.println("");
            }
            String title = b + " requires:";
            System.out.println(title);
            System.out.println(Util.getUnderlineString(title.length()));
            boolean matches = false;
            if (matchNamespace(namespace, LEGACY_BUNDLE_NAMESPACE))
            {
                matches |= printRequiredBundles(bc, b);
            }
            if (matchNamespace(namespace, LEGACY_HOST_NAMESPACE))
            {
                matches |= printFragmentHosts(bc, b);
            }
            if (matchNamespace(namespace, LEGACY_PACKAGE_NAMESPACE))
            {
                matches |= printImportedPackages(bc, b);
            }
            if (matchNamespace(namespace, NONSTANDARD_SERVICE_NAMESPACE))
            {
                matches |= printServiceRequirements(b);
            }

            // If there were no capabilities for the specified namespace,
            // then say so.
            if (!matches)
            {
                System.out.println(Util.unparseSubstring(namespace) + " " + EMPTY_MESSAGE);
            }
            separatorNeeded = true;
        }
    }

    public static boolean printExportedPackages(BundleContext bc, Bundle b)
    {
        boolean matches = false;

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Fragments cannot export packages.
        if (!isFragment(b))
        {
            // Get package admin service.
            PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
            if (pa == null)
            {
                System.out.println("PackageAdmin service is unavailable.");
            }
            else
            {
                try
                {
                    ExportedPackage[] exports = pa.getExportedPackages(b);
                    if (exports != null)
                    {
                        for (ExportedPackage ep : exports)
                        {
                            matches = true;
                            Bundle[] importers = ep.getImportingBundles();
                            if ((importers != null) && (importers.length > 0))
                            {
                                String msg = LEGACY_PACKAGE_NAMESPACE
                                    + "; "
                                    + ep.getName()
                                    + "; "
                                    + ep.getVersion().toString()
                                    + " required by:";
                                System.out.println(msg);
                                for (Bundle importer : importers)
                                {
                                    System.out.println("   " + importer);
                                }
                            }
                            else
                            {
                                System.out.println(
                                    LEGACY_PACKAGE_NAMESPACE
                                    + "; "
                                    + ep.getName()
                                    + "; "
                                    + ep.getVersion().toString()
                                    + " "
                                    + UNUSED_MESSAGE);
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
        }

        Util.ungetServices(bc, refs);

        return matches;
    }

    private static boolean printImportedPackages(BundleContext bc, Bundle b)
    {
        boolean matches = false;

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Fragments cannot import packages.
        if (!isFragment(b))
        {
            // Get package admin service.
            PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
            if (pa == null)
            {
                System.out.println("PackageAdmin service is unavailable.");
            }
            else
            {
                ExportedPackage[] exports = pa.getExportedPackages((Bundle) null);
                if (exports != null)
                {
                    for (ExportedPackage ep : exports)
                    {
                        Bundle[] importers = ep.getImportingBundles();
                        if (importers != null)
                        {
                            for (Bundle importer : importers)
                            {
                                if (importer == b)
                                {
                                    matches = true;
                                    System.out.println(
                                        LEGACY_PACKAGE_NAMESPACE
                                        + "; "
                                        + ep.getName()
                                        + " resolved by:");
                                    System.out.println(
                                        "   "
                                        + ep.getName()
                                        + "; "
                                        + ep.getVersion().toString()
                                        + " from "
                                        + ep.getExportingBundle());
                                }
                            }
                        }
                    }
                }
            }
        }

        Util.ungetServices(bc, refs);

        return matches;
    }

    public static boolean printRequiringBundles(BundleContext bc, Bundle b)
    {
        boolean matches = false;

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Fragments cannot be required.
        if (!isFragment(b))
        {
            // Get package admin service.
            PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
            if (pa == null)
            {
                System.out.println("PackageAdmin service is unavailable.");
            }
            else
            {
                try
                {
                    RequiredBundle[] rbs = pa.getRequiredBundles(b.getSymbolicName());
                    if (rbs != null)
                    {
                        for (RequiredBundle rb : rbs)
                        {
                            if (rb.getBundle() == b)
                            {
                                Bundle[] requires = rb.getRequiringBundles();
                                if ((requires != null) && (requires.length > 0))
                                {
                                    matches = true;
                                    System.out.println(
                                        LEGACY_BUNDLE_NAMESPACE
                                        + "; "
                                        + b.getSymbolicName()
                                        + "; "
                                        + b.getVersion().toString()
                                        + " required by:");
                                    for (Bundle requirer : requires)
                                    {
                                        System.out.println("   " + requirer);
                                    }
                                }
                            }
                        }
                    }

                    if (!matches)
                    {
                        matches = true;
                        System.out.println(
                            LEGACY_BUNDLE_NAMESPACE
                            + "; "
                            + b.getSymbolicName()
                            + "; "
                            + b.getVersion().toString()
                            + " "
                            + UNUSED_MESSAGE);
                    }

                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
        }

        Util.ungetServices(bc, refs);

        return matches;
    }

    private static boolean printRequiredBundles(BundleContext bc, Bundle b)
    {
        boolean matches = false;

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Fragments cannot require bundles.
        if (!isFragment(b))
        {
            // Get package admin service.
            PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
            if (pa == null)
            {
                System.out.println("PackageAdmin service is unavailable.");
            }
            else
            {
                RequiredBundle[] rbs = pa.getRequiredBundles(null);
                if (rbs != null)
                {
                    for (RequiredBundle rb : rbs)
                    {
                        Bundle[] requirers = rb.getRequiringBundles();
                        if (requirers != null)
                        {
                            for (Bundle requirer : requirers)
                            {
                                if (requirer == b)
                                {
                                    matches = true;
                                    System.out.println(
                                        LEGACY_BUNDLE_NAMESPACE
                                        + "; "
                                        + rb.getSymbolicName()
                                        + " resolved by:");
                                    System.out.println("   " + rb.getBundle());
                                }
                            }
                        }
                    }
                }
            }
        }

        Util.ungetServices(bc, refs);

        return matches;
    }

    public static boolean printHostedFragments(BundleContext bc, Bundle b)
    {
        boolean matches = false;

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            try
            {
                if (!isFragment(b))
                {
                    matches = true;
                    Bundle[] fragments = pa.getFragments(b);
                    if ((fragments != null) && (fragments.length > 0))
                    {
                        System.out.println(
                            LEGACY_HOST_NAMESPACE
                            + "; "
                            + b.getSymbolicName()
                            + "; "
                            + b.getVersion().toString()
                            + " required by:");
                        for (Bundle fragment : fragments)
                        {
                            System.out.println("   " + fragment);
                        }
                    }
                    else
                    {
                        System.out.println(
                            LEGACY_HOST_NAMESPACE
                            + "; "
                            + b.getSymbolicName()
                            + "; "
                            + b.getVersion().toString()
                            + " "
                            + UNUSED_MESSAGE);
                    }
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }

            Util.ungetServices(bc, refs);
        }

        return matches;
    }

    public static boolean printFragmentHosts(BundleContext bc, Bundle b)
    {
        boolean matches = false;

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            try
            {
                if (isFragment(b))
                {
                    matches = true;

                    Bundle[] hosts = pa.getHosts(b);
                    if ((hosts != null) && (hosts.length > 0))
                    {
                        System.out.println(
                            LEGACY_HOST_NAMESPACE
                            + "; "
                            + b.getHeaders().get(Constants.FRAGMENT_HOST)
                            + " resolved by:");
                        for (Bundle host : hosts)
                        {
                            System.out.println("   " + host);
                        }
                    }
                    else
                    {
                        System.out.println(
                            LEGACY_HOST_NAMESPACE
                            + "; "
                            + b.getHeaders().get(Constants.FRAGMENT_HOST)
                            + " "
                            + UNRESOLVED_MESSAGE);
                    }
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }

            Util.ungetServices(bc, refs);
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

    private static boolean isFragment(Bundle bundle)
    {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
    }
}