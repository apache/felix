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
package org.apache.felix.resolver.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.resolver.test.util.BundleCapability;
import org.apache.felix.resolver.test.util.BundleRequirement;
import org.apache.felix.resolver.test.util.GenericCapability;
import org.apache.felix.resolver.test.util.GenericRequirement;
import org.apache.felix.resolver.test.util.PackageCapability;
import org.apache.felix.resolver.test.util.PackageRequirement;
import org.apache.felix.resolver.test.util.ResolveContextImpl;
import org.apache.felix.resolver.test.util.ResourceImpl;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

public class Main
{
    public static void main(String[] args) throws ResolutionException
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory;
        ResolveContextImpl rci;
        Map<Resource, List<Wire>> wireMap;

        System.out.println("\nSCENARIO 1\n");
        mandatory = populateScenario1(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        wireMap = resolver.resolve(rci);
        System.out.println("RESULT " + wireMap);

        System.out.println("\nSCENARIO 2\n");
        mandatory = populateScenario2(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        wireMap = resolver.resolve(rci);
        System.out.println("RESULT " + wireMap);

        System.out.println("\nSCENARIO 3\n");
        mandatory = populateScenario3(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        wireMap = resolver.resolve(rci);
        System.out.println("RESULT " + wireMap);

        System.out.println("\nSCENARIO 4\n");
        mandatory = populateScenario4(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        try
        {
            wireMap = resolver.resolve(rci);
            System.err.println("UNEXPECTED RESULT " + wireMap);
        }
        catch (ResolutionException e)
        {
            System.out.println("EXPECTED ResolutionException:");
            e.printStackTrace(System.out);
        }

        System.out.println("\nSCENARIO 5\n");
        mandatory = populateScenario5(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        try
        {
            wireMap = resolver.resolve(rci);
            System.err.println("UNEXPECTED RESULT " + wireMap);
        }
        catch (ResolutionException e)
        {
            System.out.println("EXPECTED ResolutionException:");
            e.printStackTrace(System.out);
        }

        System.out.println("\nSCENARIO 6\n");
        mandatory = populateScenario6(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        wireMap = resolver.resolve(rci);
        System.out.println("RESULT " + wireMap);

        System.out.println("\nSCENARIO 7\n");
        mandatory = populateScenario7(wirings, candMap);
        rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.EMPTY_LIST);
        wireMap = resolver.resolve(rci);
        System.out.println("RESULT " + wireMap);
    }

    private static List<Resource> populateScenario1(
        Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        ResourceImpl exporter = new ResourceImpl("A");
        exporter.addCapability(new PackageCapability(exporter, "foo"));
        ResourceImpl importer = new ResourceImpl("B");
        importer.addRequirement(new PackageRequirement(importer, "foo"));
        candMap.put(
            importer.getRequirements(null).get(0),
            exporter.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(importer);
        return resources;
    }

    private static List<Resource> populateScenario2(
        Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        List<Capability> fooCands = new ArrayList<Capability>();
        List<Capability> barCands = new ArrayList<Capability>();

        // A
        ResourceImpl a = new ResourceImpl("A");
        PackageCapability p = new PackageCapability(a, "foo");
        a.addCapability(p);
        fooCands.add(p);

        // B
        ResourceImpl b = new ResourceImpl("B");
        p = new PackageCapability(b, "foo");
        b.addCapability(p);
        fooCands.add(p);

        p = new PackageCapability(b, "bar");
        p.addDirective(PackageNamespace.CAPABILITY_USES_DIRECTIVE, "foo");
        b.addCapability(p);
        barCands.add(p);

        // C
        ResourceImpl c = new ResourceImpl("C");
        Requirement r = new PackageRequirement(c, "foo");
        c.addRequirement(r);
        candMap.put(r, fooCands);

        r = new PackageRequirement(c, "bar");
        c.addRequirement(r);
        candMap.put(r, barCands);

        // Mandatory resources
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(c);
        return resources;
    }

    private static List<Resource> populateScenario3(
        Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        List<Capability> resourcesCands = new ArrayList<Capability>();
        List<Capability> dResourcesCands = new ArrayList<Capability>();
        List<Capability> eBundleDCands = new ArrayList<Capability>();
        List<Capability> eResourcesCands = new ArrayList<Capability>();

        // B
        ResourceImpl b = new ResourceImpl("B");
        PackageCapability pc = new PackageCapability(b, "resources");
        b.addCapability(pc);
        eResourcesCands.add(pc);

        // C
        ResourceImpl c = new ResourceImpl("C");
        pc = new PackageCapability(c, "resources");
        c.addCapability(pc);
        eResourcesCands.add(pc);
        dResourcesCands.add(pc);

        // D
        ResourceImpl d = new ResourceImpl("D");
        pc = new PackageCapability(d, "export");
        pc.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "resources");
        d.addCapability(pc);

        BundleCapability bc = new BundleCapability(d, "D");
        bc.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "resources");
        d.addCapability(bc);
        eBundleDCands.add(bc);

        Requirement r = new PackageRequirement(d, "resources");
        d.addRequirement(r);
        candMap.put(r, dResourcesCands);

        // E
        ResourceImpl e = new ResourceImpl("E");
        r = new BundleRequirement(e, "D");
        e.addRequirement(r);
        candMap.put(r, eBundleDCands);

        r = new PackageRequirement(e, "resources");
        e.addRequirement(r);
        candMap.put(r, eResourcesCands);

        // Mandatory resources
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(e);
        return resources;
    }

    private static List<Resource> populateScenario4(
            Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        ResourceImpl a = new ResourceImpl("A");
        a.addRequirement(new BundleRequirement(a, "B"));
        a.addRequirement(new BundleRequirement(a, "C"));

        ResourceImpl b = new ResourceImpl("B");
        b.addCapability(new BundleCapability(b, "B"));
        b.addCapability(new PackageCapability(b, "p1"));

        ResourceImpl c = new ResourceImpl("C");
        c.addRequirement(new BundleRequirement(c, "D"));
        c.addCapability(new BundleCapability(c, "C"));
        PackageCapability p2 = new PackageCapability(c, "p2");
        p2.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        c.addCapability(p2);

        ResourceImpl d = new ResourceImpl("D");
        d.addCapability(new BundleCapability(d, "D"));
        d.addCapability(new PackageCapability(d, "p1"));

        candMap.put(
            a.getRequirements(null).get(0),
            b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            a.getRequirements(null).get(1),
            c.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            c.getRequirements(null).get(0),
            d.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a);
        return resources;
    }

    private static List<Resource> populateScenario5(
        Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        ResourceImpl x = new ResourceImpl("X");
        x.addRequirement(new BundleRequirement(x, "A"));

        ResourceImpl a = new ResourceImpl("A");
        a.addCapability(new BundleCapability(a, "A"));
        a.addRequirement(new BundleRequirement(a, "B"));
        a.addRequirement(new BundleRequirement(a, "C"));

        ResourceImpl b = new ResourceImpl("B");
        b.addCapability(new BundleCapability(b, "B"));
        b.addCapability(new PackageCapability(b, "p1"));

        ResourceImpl c = new ResourceImpl("C");
        c.addRequirement(new BundleRequirement(c, "D"));
        c.addCapability(new BundleCapability(c, "C"));
        PackageCapability p2 = new PackageCapability(c, "p2");
        p2.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        c.addCapability(p2);

        ResourceImpl d = new ResourceImpl("D");
        d.addCapability(new BundleCapability(d, "D"));
        d.addCapability(new PackageCapability(d, "p1"));

        candMap.put(
                x.getRequirements(null).get(0),
                a.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            a.getRequirements(null).get(0),
            b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            a.getRequirements(null).get(1),
            c.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            c.getRequirements(null).get(0),
            d.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(x);
        return resources;
    }

    private static List<Resource> populateScenario6(
            Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        ResourceImpl a1 = new ResourceImpl("A");
        a1.addRequirement(new PackageRequirement(a1, "p1"));
        a1.addRequirement(new PackageRequirement(a1, "p2"));
        Requirement a1Req = new GenericRequirement(a1, "generic");
        a1Req.getDirectives().put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
        a1.addRequirement(a1Req);

        ResourceImpl a2 = new ResourceImpl("A");
        a2.addRequirement(new BundleRequirement(a2, "B"));
        a2.addRequirement(new BundleRequirement(a2, "C"));
        Requirement a2Req = new GenericRequirement(a2, "generic");
        a2Req.getDirectives().put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);
        a2.addRequirement(a2Req);

        ResourceImpl b1 = new ResourceImpl("B");
        b1.addCapability(new BundleCapability(b1, "B"));
        Capability b1_p2 = new PackageCapability(b1, "p2");
        b1_p2.getDirectives().put(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        b1.addCapability(b1_p2);
        b1.addRequirement(new PackageRequirement(b1, "p1"));

        ResourceImpl b2 = new ResourceImpl("B");
        b2.addCapability(new BundleCapability(b2, "B"));
        Capability b2_p2 = new PackageCapability(b2, "p2");
        b2_p2.getDirectives().put(Namespace.CAPABILITY_USES_DIRECTIVE, "p1");
        b2.addCapability(b2_p2);
        b2.addRequirement(new PackageRequirement(b2, "p1"));

        ResourceImpl c1 = new ResourceImpl("C");
        c1.addCapability(new BundleCapability(c1, "C"));
        Capability c1_p1 = new PackageCapability(c1, "p1");

        ResourceImpl c2 = new ResourceImpl("C");
        c2.addCapability(new BundleCapability(c2, "C"));
        Capability c2_p1 = new PackageCapability(c2, "p1");

        ResourceImpl d1 = new ResourceImpl("D");
        GenericCapability d1_generic = new GenericCapability(d1, "generic");
        d1_generic.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1,p2");
        d1.addCapability(d1_generic);
        d1.addRequirement(new PackageRequirement(d1, "p1"));
        d1.addRequirement(new PackageRequirement(d1, "p2"));

        ResourceImpl d2 = new ResourceImpl("D");
        GenericCapability d2_generic = new GenericCapability(d2, "generic");
        d2_generic.addDirective(Namespace.CAPABILITY_USES_DIRECTIVE, "p1,p2");
        d2.addCapability(d2_generic);
        d2.addRequirement(new PackageRequirement(d2, "p1"));
        d2.addRequirement(new PackageRequirement(d2, "p2"));

        candMap.put(
            a1.getRequirements(null).get(0),
            Arrays.asList(c2_p1));
        candMap.put(
            a1.getRequirements(null).get(1),
            Arrays.asList(b2_p2));
        candMap.put(
            a1.getRequirements(null).get(2),
            Arrays.asList((Capability) d1_generic, (Capability) d2_generic));
        candMap.put(
            a2.getRequirements(null).get(0),
            c2.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            a2.getRequirements(null).get(1),
            b2.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(
            a2.getRequirements(null).get(2),
            Arrays.asList((Capability) d1_generic, (Capability) d2_generic));
        candMap.put(
            b1.getRequirements(null).get(0),
            Arrays.asList(c1_p1, c2_p1));
        candMap.put(
            b2.getRequirements(null).get(0),
            Arrays.asList(c1_p1, c2_p1));
        candMap.put(
            d1.getRequirements(null).get(0),
            Arrays.asList(c1_p1, c2_p1));
        candMap.put(
            d1.getRequirements(null).get(1),
            Arrays.asList(b1_p2, b2_p2));
        candMap.put(
            d2.getRequirements(null).get(0),
            Arrays.asList(c1_p1, c2_p1));
        candMap.put(
            d2.getRequirements(null).get(1),
            Arrays.asList(b1_p2, b2_p2));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a1);
        resources.add(a2);
        return resources;
    }

    private static List<Resource> populateScenario7(
            Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        wirings.clear();
        candMap.clear();

        ResourceImpl a1 = new ResourceImpl("A");
        GenericCapability a1_hostCap = new GenericCapability(a1, HostNamespace.HOST_NAMESPACE);
        a1_hostCap.addAttribute(HostNamespace.HOST_NAMESPACE, "A");
        a1.addCapability(a1_hostCap);

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT);
        GenericRequirement f1_hostReq = new GenericRequirement(f1, HostNamespace.HOST_NAMESPACE);
        f1_hostReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + HostNamespace.HOST_NAMESPACE + "=A)");
        f1.addRequirement(f1_hostReq);

        ResourceImpl f2 = new ResourceImpl("F2", IdentityNamespace.TYPE_FRAGMENT);
        GenericRequirement f2_hostReq = new GenericRequirement(f2, HostNamespace.HOST_NAMESPACE);
        f2_hostReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + HostNamespace.HOST_NAMESPACE + "=A)");
        f2.addRequirement(f2_hostReq);

        ResourceImpl b1 = new ResourceImpl("B");
        GenericRequirement b1_identityReq = new GenericRequirement(f2, IdentityNamespace.IDENTITY_NAMESPACE);
        b1_identityReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=F2)");
        b1.addRequirement(b1_identityReq);

        candMap.put(
            f1.getRequirements(null).get(0),
            a1.getCapabilities(HostNamespace.HOST_NAMESPACE));
        candMap.put(
            f2.getRequirements(null).get(0),
            a1.getCapabilities(HostNamespace.HOST_NAMESPACE));
        candMap.put(
            b1.getRequirements(null).get(0),
            f2.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a1);
        resources.add(f1);
        resources.add(f2);
        resources.add(b1);
        return resources;
    }
}