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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.resolver.test.util.CandidateComparator;
import org.apache.felix.resolver.test.util.CapabilitySet;
import org.apache.felix.resolver.test.util.ClauseParser;
import org.apache.felix.resolver.test.util.GenericCapability;
import org.apache.felix.resolver.test.util.GenericRequirement;
import org.apache.felix.resolver.test.util.IterativeResolver;
import org.apache.felix.resolver.test.util.JsonReader;
import org.apache.felix.resolver.test.util.ResourceImpl;
import org.apache.felix.resolver.test.util.SimpleFilter;
import org.apache.felix.utils.version.VersionRange;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import static org.junit.Assert.assertEquals;

public class BigResolutionTest {

    @Test
    @Ignore
    public void testResolutionSpeed() throws Exception {
        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_INFO));

        ResolveContext rc = buildResolutionContext();

        System.out.println("Warming up...");
        Map<Resource, List<Wire>> wires = resolver.resolve(rc);
        resolver.resolve(rc);

        System.out.println("Running...");
        RunningStat stats = new RunningStat();
        for (int i = 1; i <= 100; i++) {
            System.gc();
            Thread.sleep(100);
            System.gc();
            Thread.sleep(100);
            long t0 = System.nanoTime();
            Map<Resource, List<Wire>> newWires = resolver.resolve(rc);
            long t1 = System.nanoTime();
            double dt = (t1 - t0) * 1E-6;
            System.out.println("Resolver took " + String.format("%7.2f", dt) + " ms");
            stats.put(dt);
            assertEquals(wires, newWires);

            if (i % 10 == 0) {
                System.out.println();
                System.out.println("Summary");
                System.out.println("    Min:    " + String.format("%7.2f", stats.getMin()) + " ms");
                System.out.println("    Max:    " + String.format("%7.2f", stats.getMax()) + " ms");
                System.out.println("    Avg:    " + String.format("%7.2f", stats.getAverage()) + " ms");
                System.out.println("    StdDev: " + String.format("%7" +
                        ".2f", stats.getStdDev() / stats.getAverage() * 100.0) + " %");
                System.out.println();
                stats = new RunningStat();
            }
        }

    }

    @Test
    @Ignore
    public void testIterativeResolution() throws Exception {
        ResolveContext rc = buildResolutionContext();

        ResolverImpl resolver = new ResolverImpl(new Logger(Logger.LOG_INFO));

        long t0 = System.currentTimeMillis();
        Map<Resource, List<Wire>> wiring1 = resolver.resolve(rc);
        long t1 = System.currentTimeMillis();
        System.out.println("Resolver took " + (t1 - t0) + " ms");

        long t2 = System.currentTimeMillis();
        Map<Resource, List<Wire>> wiring2 = new IterativeResolver(resolver).resolve(rc);
        long t3 = System.currentTimeMillis();
        System.out.println("Iterative resolver took " + (t3 - t2) + " ms");

        checkResolutions(wiring1, wiring2);
    }

    private ResolveContext buildResolutionContext() throws IOException, BundleException {
        Object resolution;

        InputStream is = getClass().getClassLoader().getResourceAsStream("resolution.json");
        try {
            resolution = JsonReader.read(is);
        } finally {
            is.close();
        }

        List<Resource> resources = new ArrayList<Resource>();
        ResourceImpl system = new ResourceImpl("system-bundle");
        parseCapability(system, "osgi.ee; osgi.ee=JavaSE; version=1.5");
        parseCapability(system, "osgi.ee; osgi.ee=JavaSE; version=1.6");
        parseCapability(system, "osgi.ee; osgi.ee=JavaSE; version=1.7");
        resources.add(system);
        for (Object r : (Collection) ((Map) resolution).get("resources")) {
            resources.add(parseResource(r));
        }
        final List<Resource> mandatory = new ArrayList<Resource>();
        for (Object r : (Collection) ((Map) resolution).get("mandatory")) {
            mandatory.add(parseResource(r));
        }

        final Map<String, CapabilitySet> capSets = new HashMap<String, CapabilitySet>();
        CapabilitySet svcSet = new CapabilitySet(Collections.singletonList("objectClass"));
        capSets.put("osgi.service", svcSet);
        for (Resource resource : resources) {
            for (Capability cap : resource.getCapabilities(null)) {
                String ns = cap.getNamespace();
                CapabilitySet set = capSets.get(ns);
                if (set == null) {
                    set = new CapabilitySet(Collections.singletonList(ns));
                    capSets.put(ns, set);
                }
                set.addCapability(cap);
            }
        }

        return new ResolveContext() {
            @Override
            public Collection<Resource> getMandatoryResources() {
                return mandatory;
            }

            @Override
            public List<Capability> findProviders(Requirement requirement) {
                SimpleFilter sf;
                if (requirement.getDirectives().containsKey("filter")) {
                    sf = SimpleFilter.parse(requirement.getDirectives().get("filter"));
                } else {
                    sf = SimpleFilter.convert(requirement.getAttributes());
                }
                CapabilitySet set = capSets.get(requirement.getNamespace());
                List<Capability> caps = new ArrayList<Capability>(set.match(sf, true));
                Collections.sort(caps, new CandidateComparator());
                return caps;
            }

            @Override
            public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
                capabilities.add(hostedCapability);
                return capabilities.size() - 1;
            }

            @Override
            public boolean isEffective(Requirement requirement) {
                return true;
            }

            @Override
            public Map<Resource, Wiring> getWirings() {
                return Collections.emptyMap();
            }
        };
    }

    private void checkResolutions(Map<Resource, List<Wire>> wireMap1, Map<Resource, List<Wire>> wireMap2) {
        Set<Resource> resources;
        resources = new HashSet<Resource>(wireMap1.keySet());
        resources.removeAll(wireMap2.keySet());
        for (Resource res : resources) {
            System.out.println("Resource resolved in r1 and not in r2: " + res);
        }
        resources = new HashSet<Resource>(wireMap2.keySet());
        resources.removeAll(wireMap1.keySet());
        for (Resource res : resources) {
            System.out.println("Resource resolved in r2 and not in r1: " + res);
        }
        resources = new HashSet<Resource>(wireMap2.keySet());
        resources.retainAll(wireMap1.keySet());
        for (Resource resource : resources) {
            Set<Wire> wires1 = new HashSet<Wire>(wireMap1.get(resource));
            Set<Wire> wires2 = new HashSet<Wire>(wireMap2.get(resource));
            wires1.removeAll(wireMap2.get(resource));
            wires2.removeAll(wireMap1.get(resource));
            if (!wires1.isEmpty() || !wires2.isEmpty()) {
                System.out.println("Different wiring for resource: " + resource);
            }
            for (Wire wire : wires1) {
                System.out.println("\tR1: " + wire);
            }
            for (Wire wire : wires2) {
                System.out.println("\tR2: " + wire);
            }
        }
        if (!wireMap1.equals(wireMap2)) {
            throw new RuntimeException("Different wiring");
        }
    }

    @SuppressWarnings("unchecked")
    public static Resource parseResource(Object resource) throws BundleException {
        ResourceImpl res = new ResourceImpl();
        Collection<String> caps = (Collection<String>) ((Map) resource).get("capabilities");
        if (caps != null) {
            for (String s : caps) {
                parseCapability(res, s);
            }
        }
        Collection<String> reqs = (Collection<String>) ((Map) resource).get("requirements");
        if (reqs != null) {
            for (String s : reqs) {
                parseRequirement(res, s);
            }
        }
        return res;
    }

    private static void parseRequirement(ResourceImpl res, String s) throws BundleException {
        List<ClauseParser.ParsedHeaderClause> clauses = ClauseParser.parseStandardHeader(s);
        normalizeRequirementClauses(clauses);
        for (ClauseParser.ParsedHeaderClause clause : clauses) {
            for (String path : clause.paths) {
                GenericRequirement requirement = new GenericRequirement(res, path);
                for (Map.Entry<String, String> dir : clause.dirs.entrySet()) {
                    requirement.addDirective(dir.getKey(), dir.getValue());
                }
                for (Map.Entry<String, Object> attr : clause.attrs.entrySet()) {
                    requirement.addAttribute(attr.getKey(), attr.getValue());
                }
                res.addRequirement(requirement);
            }
        }
    }

    private static void parseCapability(ResourceImpl res, String s) throws BundleException {
        List<ClauseParser.ParsedHeaderClause> clauses = ClauseParser.parseStandardHeader(s);
        normalizeCapabilityClauses(clauses);
        for (ClauseParser.ParsedHeaderClause clause : clauses) {
            for (String path : clause.paths) {
                GenericCapability capability = new GenericCapability(res, path);
                for (Map.Entry<String, String> dir : clause.dirs.entrySet()) {
                    capability.addDirective(dir.getKey(), dir.getValue());
                }
                for (Map.Entry<String, Object> attr : clause.attrs.entrySet()) {
                    capability.addAttribute(attr.getKey(), attr.getValue());
                }
                res.addCapability(capability);
            }
        }
    }

    private static void normalizeRequirementClauses(
            List<ClauseParser.ParsedHeaderClause> clauses)
            throws BundleException {

        // Convert attributes into specified types.
        for (ClauseParser.ParsedHeaderClause clause : clauses)
        {
            for (Map.Entry<String, Object> entry : clause.attrs.entrySet())
            {
                String key = entry.getKey();
                Object val = entry.getValue();
                String type = clause.types.get(key);
                if ("Version".equals(type) || "version".equals(key))
                {
                    clause.attrs.put(
                            key,
                            VersionRange.parseVersionRange(val.toString().trim()));
                }
            }
        }
    }

    private static void normalizeCapabilityClauses(
            List<ClauseParser.ParsedHeaderClause> clauses)
            throws BundleException
    {
        // Convert attributes into specified types.
        for (ClauseParser.ParsedHeaderClause clause : clauses)
        {
            for (Map.Entry<String, String> entry : clause.types.entrySet())
            {
                String type = entry.getValue();
                if (!type.equals("String"))
                {
                    if (type.equals("Double"))
                    {
                        clause.attrs.put(
                                entry.getKey(),
                                new Double(clause.attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.equals("Version"))
                    {
                        clause.attrs.put(
                                entry.getKey(),
                                new Version(clause.attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.equals("Long"))
                    {
                        clause.attrs.put(
                                entry.getKey(),
                                new Long(clause.attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.startsWith("List"))
                    {
                        int startIdx = type.indexOf('<');
                        int endIdx = type.indexOf('>');
                        if (((startIdx > 0) && (endIdx <= startIdx))
                                || ((startIdx < 0) && (endIdx > 0)))
                        {
                            throw new BundleException(
                                    "Invalid Provide-Capability attribute list type for '"
                                            + entry.getKey()
                                            + "' : "
                                            + type);
                        }

                        String listType = "String";
                        if (endIdx > startIdx)
                        {
                            listType = type.substring(startIdx + 1, endIdx).trim();
                        }

                        List<String> tokens = ClauseParser.parseDelimitedString(
                                clause.attrs.get(entry.getKey()).toString(), ",", false);
                        List<Object> values = new ArrayList<Object>(tokens.size());
                        for (String token : tokens)
                        {
                            if (listType.equals("String"))
                            {
                                values.add(token);
                            }
                            else if (listType.equals("Double"))
                            {
                                values.add(new Double(token.trim()));
                            }
                            else if (listType.equals("Version"))
                            {
                                values.add(new Version(token.trim()));
                            }
                            else if (listType.equals("Long"))
                            {
                                values.add(new Long(token.trim()));
                            }
                            else
                            {
                                throw new BundleException(
                                        "Unknown Provide-Capability attribute list type for '"
                                                + entry.getKey()
                                                + "' : "
                                                + type);
                            }
                        }
                        clause.attrs.put(
                                entry.getKey(),
                                values);
                    }
                    else
                    {
                        throw new BundleException(
                                "Unknown Provide-Capability attribute type for '"
                                        + entry.getKey()
                                        + "' : "
                                        + type);
                    }
                }
            }
        }
    }

    public static class RunningStat {

        private int count = 0;
        private double min = Double.MAX_VALUE;
        private double max = 0.0;
        private double average = 0.0;
        private double pwrSumAvg = 0.0;
        private double stdDev = 0.0;

        /**
         * Incoming new values used to calculate the running statistics
         *
         * @param value the new value
         */
        public void put(double value) {

            count++;
            average += (value - average) / count;
            pwrSumAvg += (value * value - pwrSumAvg) / count;
            stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
            min = Math.min(min, value);
            max = Math.max(max, value);

        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getAverage() {

            return average;
        }

        public double getStdDev() {

            return Double.isNaN(stdDev) ? 0.0 : stdDev;
        }

    }
}
