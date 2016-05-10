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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
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

public class ResolverTest
{
    @Test
    public void testScenario1() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario1(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(2, wireMap.size());

        Resource aRes = findResource("A", wireMap.keySet());
        List<Wire> aWires = wireMap.get(aRes);
        assertEquals(0, aWires.size());

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(1, bWires.size());
        Wire bWire = bWires.iterator().next();
        assertEquals(aRes, bWire.getProvider());
        assertEquals(bRes, bWire.getRequirer());
        Capability cap = bWire.getCapability();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, cap.getNamespace());
        assertEquals(1, cap.getAttributes().size());
        assertEquals("foo", cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(0, cap.getDirectives().size());
        assertEquals(aRes, cap.getResource());

        Requirement req = bWire.getRequirement();
        assertEquals(1, req.getDirectives().size());
        assertEquals("(osgi.wiring.package=foo)", req.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        assertEquals(0, req.getAttributes().size());
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, req.getNamespace());
        assertEquals(bRes, req.getResource());
    }

    @Test
    public void testScenario2() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario2(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(2, wireMap.size());

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(0, bWires.size());

        Resource cRes = findResource("C", wireMap.keySet());
        List<Wire> cWires = wireMap.get(cRes);
        assertEquals(2, cWires.size());

        boolean foundFoo = false;
        boolean foundBar = false;
        for (Wire w : cWires)
        {
            assertEquals(bRes, w.getProvider());
            assertEquals(cRes, w.getRequirer());

            Capability cap = w.getCapability();
            assertEquals(PackageNamespace.PACKAGE_NAMESPACE, cap.getNamespace());
            assertEquals(bRes, cap.getResource());
            Map<String, Object> attrs = cap.getAttributes();
            assertEquals(1, attrs.size());
            Object pkg = attrs.get(PackageNamespace.PACKAGE_NAMESPACE);
            if ("foo".equals(pkg))
            {
                foundFoo = true;
                assertEquals(0, cap.getDirectives().size());
            }
            else if ("bar".equals(pkg))
            {
                foundBar = true;
                assertEquals(1, cap.getDirectives().size());
                assertEquals("foo", cap.getDirectives().get(PackageNamespace.CAPABILITY_USES_DIRECTIVE));
            }
        }
        assertTrue(foundFoo);
        assertTrue(foundBar);
    }

    @Test
    public void testScenario3() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario3(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(3, wireMap.size());

        Resource cRes = findResource("C", wireMap.keySet());
        List<Wire> cWires = wireMap.get(cRes);
        assertEquals(0, cWires.size());

        Resource dRes = findResource("D", wireMap.keySet());
        List<Wire> dWires = wireMap.get(dRes);
        assertEquals(1, dWires.size());
        Wire dWire = dWires.iterator().next();
        assertEquals(cRes, dWire.getProvider());
        assertEquals(dRes, dWire.getRequirer());
        Capability dwCap = dWire.getCapability();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE, dwCap.getNamespace());
        assertEquals(1, dwCap.getAttributes().size());
        assertEquals("resources", dwCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(0, dwCap.getDirectives().size());
        assertEquals(cRes, dwCap.getResource());

        Resource eRes = findResource("E", wireMap.keySet());
        List<Wire> eWires = wireMap.get(eRes);
        assertEquals(2, eWires.size());

        boolean foundC = false;
        boolean foundD = false;
        for (Wire w : eWires)
        {
            assertEquals(eRes, w.getRequirer());

            Capability cap = w.getCapability();
            if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                assertEquals("resources", cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
                assertEquals(0, cap.getDirectives().size());
                assertEquals(cRes, cap.getResource());
                foundC = true;

                Requirement req = w.getRequirement();
                assertEquals(PackageNamespace.PACKAGE_NAMESPACE, req.getNamespace());
                assertEquals(eRes, req.getResource());
                assertEquals(0, req.getAttributes().size());
                assertEquals(1, req.getDirectives().size());
                assertEquals("(osgi.wiring.package=resources)", req.getDirectives().get("filter"));
            }
            else if (cap.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
            {
                assertEquals("D", cap.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
                assertEquals(1, cap.getDirectives().size());
                assertEquals("resources", cap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE));
                assertEquals(dRes, cap.getResource());
                foundD = true;

                Requirement req = w.getRequirement();
                assertEquals(BundleNamespace.BUNDLE_NAMESPACE, req.getNamespace());
                assertEquals(eRes, req.getResource());
                assertEquals(0, req.getAttributes().size());
                assertEquals(1, req.getDirectives().size());
                assertEquals("(osgi.wiring.bundle=D)", req.getDirectives().get("filter"));
            }
        }
        assertTrue(foundC);
        assertTrue(foundD);
    }

    @Test
    public void testScenario4() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario4(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        try
        {
            resolver.resolve(rci);
            fail("Should have thrown a resolution exception as bundle A in scenario 4 cannot be resolved due to constraint violations.");
        }
        catch (ResolutionException re)
        {
            // good
        }
    }

    @Test
    public void testScenario5() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario5(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        try
        {
            resolver.resolve(rci);
            fail("Should have thrown a resolution exception as bundle A in scenario 5 cannot be resolved due to constraint violations.");
        }
        catch (ResolutionException re)
        {
            // good
        }
    }

    @Test
    public void testScenario6() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario6(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        int aResources = 0;
        for (Resource r : wireMap.keySet())
        {
            if ("A".equals(getResourceName(r)))
            {
                aResources++;

                List<Wire> wires = wireMap.get(r);
                assertEquals(4, wires.size());
                List<String> providers = new ArrayList<String>();
                for (Wire w : wires)
                {
                    providers.add(getResourceName(w.getProvider()));
                }
                Collections.sort(providers);
                assertEquals(Arrays.asList("B", "C", "D", "D"), providers);
            }
        }
        assertEquals("Should have found two resolved resources named 'A'", 2, aResources);
    }

    @Test
    public void testScenario7() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario7(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);
        assertEquals(4, wireMap.size());

        Resource aRes = findResource("A", wireMap.keySet());
        List<Wire> aWires = wireMap.get(aRes);
        assertEquals(0, aWires.size());

        Resource f1Res = findResource("F1", wireMap.keySet());
        List<Wire> f1Wires = wireMap.get(f1Res);
        assertEquals(1, f1Wires.size());
        Wire f1Wire = f1Wires.get(0);
        assertEquals(f1Res, f1Wire.getRequirer());
        assertEquals(aRes, f1Wire.getProvider());
        Requirement req = f1Wire.getRequirement();
        assertEquals(HostNamespace.HOST_NAMESPACE, req.getNamespace());
        assertEquals(0, req.getAttributes().size());
        assertEquals(f1Res, req.getResource());
        assertEquals(1, req.getDirectives().size());
        assertEquals("(osgi.wiring.host=A)", req.getDirectives().get("filter"));
        Capability cap = f1Wire.getCapability();
        assertEquals(aRes, cap.getResource());
        assertEquals(HostNamespace.HOST_NAMESPACE, cap.getNamespace());
        assertEquals(0, cap.getDirectives().size());
        assertEquals(1, cap.getAttributes().size());
        assertEquals("A", cap.getAttributes().get(HostNamespace.HOST_NAMESPACE));

        Resource f2Res = findResource("F2", wireMap.keySet());
        List<Wire> f2Wires = wireMap.get(f2Res);
        assertEquals(1, f2Wires.size());
        Wire f2Wire = f2Wires.get(0);
        assertEquals(f2Res, f2Wire.getRequirer());
        assertEquals(aRes, f2Wire.getProvider());
        Requirement req2 = f2Wire.getRequirement();
        assertEquals(HostNamespace.HOST_NAMESPACE, req2.getNamespace());
        assertEquals(0, req2.getAttributes().size());
        assertEquals(f2Res, req2.getResource());
        assertEquals(1, req2.getDirectives().size());
        assertEquals("(osgi.wiring.host=A)", req2.getDirectives().get("filter"));
        Capability cap2 = f1Wire.getCapability();
        assertEquals(aRes, cap2.getResource());
        assertEquals(HostNamespace.HOST_NAMESPACE, cap2.getNamespace());
        assertEquals(0, cap2.getDirectives().size());
        assertEquals(1, cap2.getAttributes().size());
        assertEquals("A", cap2.getAttributes().get(HostNamespace.HOST_NAMESPACE));

        Resource bRes = findResource("B", wireMap.keySet());
        List<Wire> bWires = wireMap.get(bRes);
        assertEquals(1, bWires.size());
        Wire bWire = bWires.get(0);
        assertEquals(bRes, bWire.getRequirer());
        assertEquals(f2Res, bWire.getProvider());
        Requirement bReq = bWire.getRequirement();
        assertEquals(IdentityNamespace.IDENTITY_NAMESPACE, bReq.getNamespace());
        assertEquals(0, bReq.getAttributes().size());
        assertEquals(bRes, bReq.getResource());
        assertEquals(1, bReq.getDirectives().size());
        assertEquals("(osgi.identity=F2)", bReq.getDirectives().get("filter"));
    }

    @Test
    public void testScenario8() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario8(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        Resource res2 = findResource("res2", wireMap.keySet());
        Resource res4 = findResource("res4", wireMap.keySet());
        Resource res5 = findResource("res5", wireMap.keySet());

        assertNotNull(res2);
        assertNotNull(res4);
        assertNotNull(res5);

        List<Wire> wires2 = wireMap.get(res2);
        assertEquals(2, wires2.size());
        // should be wired to res4 and res5

        List<Wire> wires4 = wireMap.get(res4);
        assertEquals(1, wires4.size());
        // should be wired to res5

        List<Wire> wires5 = wireMap.get(res5);
        assertEquals(0, wires5.size());
        // should not be wired to any of its optional requirements to res6

        assertEquals(3, wireMap.size());
    }

    @Test
    public void testScenario9() throws Exception
    {
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();
        List<Resource> mandatory = populateScenario9(wirings, candMap);
        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, mandatory, Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        Resource resB = findResource("B", wireMap.keySet());
        Resource resA = findResource("A", wirings.keySet());
        Resource resC1 = findResource("C1", wirings.keySet());

        assertNotNull(resB);
        assertNotNull(resC1);

        assertEquals(1, wireMap.size());

        List<Wire> wiresB = wireMap.get(resB);
        assertEquals(2, wiresB.size());
        // should be wired to A and C1
        assertEquals(resA, wiresB.get(0).getProvider());
        assertEquals(resC1, wiresB.get(1).getProvider());
    }

    /**
     * Test dynamic resolution with a resolved fragment
     */
    @Test
    public void testScenario10() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(a1, new ArrayList<Wire>());
        wires.put(b1, new ArrayList<Wire>());
        wires.put(f1, new ArrayList<Wire>());
        wires.get(f1).add(new SimpleWire(f1_hostReq, a1_hostCap));

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(b1, new ArrayList<Wire>());
        invertedWires.put(f1, new ArrayList<Wire>());
        invertedWires.get(a1).add(new SimpleWire(f1_hostReq, a1_hostCap));

        wirings.put(a1, new SimpleWiring(a1, Arrays.asList(a1_hostCap, f1_pkgCap), wires, invertedWires));
        wirings.put(b1, new SimpleWiring(b1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(f1, new SimpleWiring(f1, Collections.<Capability>emptyList(), wires, invertedWires));

        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource>emptyList(), Collections.<Resource> emptyList());

        List<Capability> caps = new ArrayList<Capability>();
        caps.add(f1_pkgCap);
        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci, b1, b_pkgReq1, caps);

        assertEquals(1, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        // should be wired to A through the fragment capability
        assertEquals(a1, wiresB.get(0).getProvider());
        assertEquals(f1_pkgCap, wiresB.get(0).getCapability());
    }

    /**
     * Test dynamic resolution with an unresolved fragment
     */
    @Test
    public void testScenario11() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));
        candMap.put(f1_hostReq, Collections.singletonList(a1_hostCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(a1, new ArrayList<Wire>());
        wires.put(b1, new ArrayList<Wire>());

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(b1, new ArrayList<Wire>());

        wirings.put(a1, new SimpleWiring(a1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(b1, new SimpleWiring(b1, Collections.<Capability>emptyList(), wires, invertedWires));

        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource>emptyList(), Collections.<Resource> emptyList());

        List<Capability> caps = new ArrayList<Capability>();
        caps.add(f1_pkgCap);
        try {
            resolver.resolve(rci, b1, b_pkgReq1, caps);
            fail("Should fail to dynamic requirement to fragment when host is resolved already.");
        } catch (ResolutionException e) {
            // expected
            assertTrue(e.getUnresolvedRequirements().contains(b_pkgReq1));
        }

        // now remove host wiring
        wirings.remove(a1);
        caps.clear();
        caps.add(f1_pkgCap);
        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci, b1, b_pkgReq1, caps);

        assertEquals(3, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        // should be wired to A through the fragment capability
        assertEquals(a1, wiresB.get(0).getProvider());
        assertEquals(f1_pkgCap, wiresB.get(0).getCapability());
    }

    /**
     * Test dynamic resolution with an unresolvable host
     */
    @Test(expected = ResolutionException.class)
    public void testScenario12() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl c1 = new ResourceImpl("C");
        Capability c_hostCap = addCap(c1, HostNamespace.HOST_NAMESPACE, "A");
        Capability c_pkgCap = addCap(c1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");
        Requirement c_pkgReq1 = addReq(c1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.b");

        candMap.put(b_pkgReq1, Collections.singletonList(c_pkgCap));
        candMap.put(c_pkgReq1, Collections.<Capability>emptyList());

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(a1, new ArrayList<Wire>());
        wires.put(b1, new ArrayList<Wire>());

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(b1, new ArrayList<Wire>());

        wirings.put(a1, new SimpleWiring(a1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(b1, new SimpleWiring(b1, Collections.<Capability>emptyList(), wires, invertedWires));

        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource>emptyList(), Collections.<Resource> emptyList());

        List<Capability> caps = new ArrayList<Capability>();
        caps.add(c_pkgCap);
        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci, b1, b_pkgReq1, caps);

        assertEquals(0, wireMap.size());
    }

    @Test
    public void testScenario13() throws Exception
    {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG), 1);

        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();
        Map<Requirement, List<Capability>> candMap = new HashMap<Requirement, List<Capability>>();

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));
        candMap.put(f1_hostReq, Collections.singletonList(a1_hostCap));


        ResolveContextImpl rci = new ResolveContextImpl(wirings, candMap, Collections.<Resource> singletonList(b1), Collections.<Resource> emptyList());

        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);

        assertEquals(3, wireMap.size());
        List<Wire> wiresB = wireMap.get(b1);
        assertNotNull(wiresB);
        assertEquals(1, wiresB.size());
        // should be wired to A through the fragment capability
        assertEquals(a1, wiresB.get(0).getProvider());
        assertEquals(f1_pkgCap, wiresB.get(0).getCapability());
    }

    @Test
    public void testPackageSources() throws Exception {
        Method m = ResolverImpl.class.getDeclaredMethod("getPackageSources",
                Capability.class, Map.class);
        m.setAccessible(true);

        Capability cap = Mockito.mock(Capability.class);
        assertEquals(Collections.emptySet(),
                m.invoke(null, cap, new HashMap<Resource, ResolverImpl.Packages>()));

        Capability cap2 = Mockito.mock(Capability.class);
        Resource res2 = Mockito.mock(Resource.class);
        Mockito.when(cap2.getResource()).thenReturn(res2);
        Map<Resource, ResolverImpl.Packages> map2 = new HashMap<Resource, ResolverImpl.Packages>();
        map2.put(res2, new ResolverImpl.Packages(res2));
        assertEquals(Collections.emptySet(), m.invoke(null, cap2, map2));

        Capability cap3 = Mockito.mock(Capability.class);
        Resource res3 = Mockito.mock(Resource.class);
        Mockito.when(cap3.getResource()).thenReturn(res3);
        Map<Resource, ResolverImpl.Packages> map3 = new HashMap<Resource, ResolverImpl.Packages>();
        ResolverImpl.Packages pkgs3 = new ResolverImpl.Packages(res3);
        Set<Capability> srcCaps3 = Collections.singleton(Mockito.mock(Capability.class));
        Map<Capability, Set<Capability>> srcMap3 = Collections.singletonMap(
                cap3, srcCaps3);
        pkgs3.m_sources.putAll(srcMap3);
        map3.put(res3, pkgs3);
        assertEquals(srcCaps3, m.invoke(null, cap3, map3));

    }

    private static String getResourceName(Resource r)
    {
        return r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0).getAttributes()
                .get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
    }

    private static Resource findResource(String identity, Collection<Resource> resources)
    {
        for (Resource r : resources)
        {
            if (identity.equals(getResourceName(r)))
                return r;
        }
        return null;
    }

    private static List<Resource> populateScenario1(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl exporter = new ResourceImpl("A");
        exporter.addCapability(new PackageCapability(exporter, "foo"));
        ResourceImpl importer = new ResourceImpl("B");
        importer.addRequirement(new PackageRequirement(importer, "foo"));
        candMap.put(importer.getRequirements(null).get(0), exporter.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(importer);
        return resources;
    }

    private static List<Resource> populateScenario2(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
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

    private static List<Resource> populateScenario3(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
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

    private static List<Resource> populateScenario4(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
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

        candMap.put(a.getRequirements(null).get(0), b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a.getRequirements(null).get(1), c.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(c.getRequirements(null).get(0), d.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a);
        return resources;
    }

    private static List<Resource> populateScenario5(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
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

        candMap.put(x.getRequirements(null).get(0), a.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a.getRequirements(null).get(0), b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a.getRequirements(null).get(1), c.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(c.getRequirements(null).get(0), d.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(x);
        return resources;
    }

    private static List<Resource> populateScenario6(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
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
        c1.addCapability(c1_p1);

        ResourceImpl c2 = new ResourceImpl("C");
        c2.addCapability(new BundleCapability(c2, "C"));
        Capability c2_p1 = new PackageCapability(c2, "p1");
        c2.addCapability(c2_p1);

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

        candMap.put(a1.getRequirements(null).get(0), Arrays.asList(c2_p1));
        candMap.put(a1.getRequirements(null).get(1), Arrays.asList(b2_p2));
        candMap.put(a1.getRequirements(null).get(2), Arrays.asList((Capability) d1_generic, (Capability) d2_generic));
        candMap.put(a2.getRequirements(null).get(0), c2.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a2.getRequirements(null).get(1), b2.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
        candMap.put(a2.getRequirements(null).get(2), Arrays.asList((Capability) d1_generic, (Capability) d2_generic));
        candMap.put(b1.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(b2.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(d1.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(d1.getRequirements(null).get(1), Arrays.asList(b1_p2, b2_p2));
        candMap.put(d2.getRequirements(null).get(0), Arrays.asList(c1_p1, c2_p1));
        candMap.put(d2.getRequirements(null).get(1), Arrays.asList(b1_p2, b2_p2));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a1);
        resources.add(a2);
        return resources;
    }

    private static List<Resource> populateScenario7(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
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
        GenericRequirement b1_identityReq = new GenericRequirement(b1, IdentityNamespace.IDENTITY_NAMESPACE);
        b1_identityReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=F2)");
        b1.addRequirement(b1_identityReq);

        candMap.put(f1.getRequirements(null).get(0), a1.getCapabilities(HostNamespace.HOST_NAMESPACE));
        candMap.put(f2.getRequirements(null).get(0), a1.getCapabilities(HostNamespace.HOST_NAMESPACE));
        candMap.put(b1.getRequirements(null).get(0), f2.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(a1);
        resources.add(f1);
        resources.add(f2);
        resources.add(b1);
        return resources;
    }

    private static List<Resource> populateScenario8(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap)
    {
        ResourceImpl res2 = new ResourceImpl("res2");
        Requirement req25 = addReq(res2, IdentityNamespace.IDENTITY_NAMESPACE, "res5");
        Requirement req24 = addReq(res2, IdentityNamespace.IDENTITY_NAMESPACE, "res4");
        Requirement req23 = addReq(res2, IdentityNamespace.IDENTITY_NAMESPACE, "res3", true);

        ResourceImpl res3 = new ResourceImpl("res3");
        Requirement req32 = addReq(res3, IdentityNamespace.IDENTITY_NAMESPACE, "res2");
        Requirement req3x = addReq(res3, "foo", "bar");

        ResourceImpl res4 = new ResourceImpl("res4");
        Requirement req45 = addReq(res4, IdentityNamespace.IDENTITY_NAMESPACE, "res5");

        ResourceImpl res5 = new ResourceImpl("res5");
        Requirement req5x1 = addReq(res5, BundleNamespace.BUNDLE_NAMESPACE, "package1", true);
        Requirement req5x2 = addReq(res5, BundleNamespace.BUNDLE_NAMESPACE, "package2", true);

        ResourceImpl res6 = new ResourceImpl("res6");
        Capability cap6x1 = addCap(res6, BundleNamespace.BUNDLE_NAMESPACE, "package1");
        Capability cap6x2 = addCap(res6, BundleNamespace.BUNDLE_NAMESPACE, "package2");
        Requirement req63 = addReq(res6, IdentityNamespace.IDENTITY_NAMESPACE, "res3");

        candMap.put(req25, res5.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req24, res4.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req23, res3.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req32, res2.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req45, res5.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req63, res3.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
        candMap.put(req3x, Arrays.<Capability>asList());
        candMap.put(req5x1, Arrays.<Capability>asList(cap6x1));
        candMap.put(req5x2, Arrays.<Capability>asList(cap6x2));
        return Arrays.<Resource>asList(res2);
    }

    private static List<Resource> populateScenario9(Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap) {
        ResourceImpl c1 = new ResourceImpl("C1");
        Capability c1_pkgCap  = addCap(c1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");

        ResourceImpl c2 = new ResourceImpl("C2");
        Capability c2_pkgCap  = addCap(c2, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");

        ResourceImpl a1 = new ResourceImpl("A");
        Capability a1_hostCap = addCap(a1, HostNamespace.HOST_NAMESPACE, "A");

        ResourceImpl f1 = new ResourceImpl("F1", IdentityNamespace.TYPE_FRAGMENT);
        Requirement f1_hostReq = addReq(f1, HostNamespace.HOST_NAMESPACE, "A");
        Requirement f1_pkgReq = addReq(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");
        Capability f1_pkgCap = addCap(f1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a", "org.foo.c");

        ResourceImpl b1 = new ResourceImpl("B");
        Requirement b_pkgReq1 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.a");
        Requirement b_pkgReq2 = addReq(b1, PackageNamespace.PACKAGE_NAMESPACE, "org.foo.c");

        candMap.put(b_pkgReq1, Collections.singletonList(f1_pkgCap));
        candMap.put(b_pkgReq2, Arrays.asList(c2_pkgCap, c1_pkgCap));

        Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        wires.put(c1, new ArrayList<Wire>());
        wires.put(c2, new ArrayList<Wire>());
        wires.put(a1, new ArrayList<Wire>());
        wires.put(f1, new ArrayList<Wire>());
        wires.get(f1).add(new SimpleWire(f1_hostReq, a1_hostCap));
        wires.get(a1).add(new SimpleWire(f1_pkgReq, c1_pkgCap, a1, c1));

        Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        invertedWires.put(c1, new ArrayList<Wire>());
        invertedWires.put(c2, new ArrayList<Wire>());
        invertedWires.put(a1, new ArrayList<Wire>());
        invertedWires.put(f1, new ArrayList<Wire>());
        invertedWires.get(a1).add(new SimpleWire(f1_hostReq, a1_hostCap));
        invertedWires.get(c1).add(new SimpleWire(f1_pkgReq, c1_pkgCap, a1, c1));

        wirings.put(a1, new SimpleWiring(a1, Arrays.asList(a1_hostCap, f1_pkgCap), wires, invertedWires));
        wirings.put(f1, new SimpleWiring(f1, Collections.<Capability>emptyList(), wires, invertedWires));
        wirings.put(c1, new SimpleWiring(c1, Collections.singletonList(c1_pkgCap), wires, invertedWires));
        wirings.put(c2, new SimpleWiring(c2, Collections.singletonList(c2_pkgCap), wires, invertedWires));

        return Collections.<Resource>singletonList(b1);
    }

    private static Capability addCap(ResourceImpl res, String namespace, String value)
    {
        return addCap(res, namespace, value, null);
    }

    private static Capability addCap(ResourceImpl res, String namespace, String value, String uses)
    {
        GenericCapability cap = new GenericCapability(res, namespace);
        cap.addAttribute(namespace, value);
        if (uses != null)
        {
            cap.addDirective("uses", uses);
        }
        res.addCapability(cap);
        return cap;
    }

    private static Requirement addReq(ResourceImpl res, String namespace, String value)
    {
        return addReq(res, namespace, value, false);
    }

    private static Requirement addReq(ResourceImpl res, String namespace, String value, boolean optional)
    {
        GenericRequirement req = new GenericRequirement(res, namespace);
        req.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + namespace + "=" + value + ")");
        if (optional) {
            req.addDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
        }
        res.addRequirement(req);
        return req;
    }

    private static class SimpleWire implements Wire {
        final Requirement requirement;
        final Capability capability;
        final Resource requirer;
        final Resource provider;

        public SimpleWire(Requirement requirement, Capability capability) {
            this(requirement, capability, requirement.getResource(), capability.getResource());
        }

        public SimpleWire(Requirement requirement, Capability capability, Resource requirer, Resource provider) {
            this.requirement = requirement;
            this.capability = capability;
            this.requirer = requirer;
            this.provider = provider;
        }

        public Capability getCapability() {
            return capability;
        }

        public Requirement getRequirement() {
            return requirement;
        }

        public Resource getRequirer() {
            return requirer;
        }

        public Resource getProvider() {
            return provider;
        }
    }

    private static class SimpleWiring implements Wiring {
        final Resource resource;
        final Map<Resource, List<Wire>> wires;
        final Map<Resource, List<Wire>> invertedWires;
        List<Capability> resourceCapabilities;
        List<Requirement> resourceRequirements;

        private SimpleWiring(Resource resource, List<Capability> resourceCapabilities, Map<Resource, List<Wire>> wires, Map<Resource, List<Wire>> invertedWires) {
            this.resource = resource;
            this.wires = wires;
            this.invertedWires = invertedWires;
            this.resourceCapabilities = resourceCapabilities;
        }

        public List<Capability> getResourceCapabilities(String namespace) {
            if (resourceCapabilities == null) {
                resourceCapabilities = new ArrayList<Capability>();
                for (Wire wire : invertedWires.get(resource)) {
                    if (!resourceCapabilities.contains(wire.getCapability())) {
                        resourceCapabilities.add(wire.getCapability());
                    }
                }
            }
            if (namespace != null) {
                List<Capability> caps = new ArrayList<Capability>();
                for (Capability cap : resourceCapabilities) {
                    if (namespace.equals(cap.getNamespace())) {
                        caps.add(cap);
                    }
                }
                return caps;
            }
            return resourceCapabilities;
        }

        public List<Requirement> getResourceRequirements(String namespace) {
            if (resourceRequirements == null) {
                resourceRequirements = new ArrayList<Requirement>();
                for (Wire wire : wires.get(resource)) {
                    if (!resourceRequirements.contains(wire.getRequirement())) {
                        resourceRequirements.add(wire.getRequirement());
                    }
                }
            }
            if (namespace != null) {
                List<Requirement> reqs = new ArrayList<Requirement>();
                for (Requirement req : resourceRequirements) {
                    if (namespace.equals(req.getNamespace())) {
                        reqs.add(req);
                    }
                }
                return reqs;
            }
            return resourceRequirements;
        }

        public List<Wire> getProvidedResourceWires(String namespace) {
            List<Wire> providedWires = invertedWires.get(resource);
            if (namespace != null) {
                List<Wire> wires = new ArrayList<Wire>();
                for (Wire wire : providedWires) {
                    if (namespace.equals(wire.getRequirement().getNamespace())) {
                        wires.add(wire);
                    }
                }
                return wires;
            }
            return providedWires;
        }

        public List<Wire> getRequiredResourceWires(String namespace) {
            List<Wire> requiredWires = wires.get(resource);
            if (namespace != null) {
                List<Wire> wires = new ArrayList<Wire>();
                for (Wire wire : requiredWires) {
                    if (namespace.equals(wire.getCapability().getNamespace())) {
                        wires.add(wire);
                    }
                }
                return wires;
            }
            return requiredWires;
        }

        public Resource getResource() {
            return resource;
        }
    }
}
