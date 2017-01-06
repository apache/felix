/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.resolver.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.felix.resolver.Logger;
import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.resolver.Util;
import org.apache.felix.resolver.test.util.CapabilitySet;
import org.apache.felix.resolver.test.util.JsonReader;
import org.apache.felix.resolver.test.util.ResolveContextImpl;
import org.apache.felix.resolver.test.util.SimpleFilter;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.Resolver;

public class FELIX_4914_Test extends TestCase {

    @Test
    public void testResolution() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/felix-4914.json")));
        Map<String, Object> resolution = (Map<String, Object>) JsonReader.read(reader);
        List<Resource> repository = readRepository(resolution.get("repository"));

        Map<String, CapabilitySet> capSets = new HashMap<String, CapabilitySet>();
        for (Resource resource : repository) {
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

        Resource root = null;
        for (Resource resource : repository) {
            if ("root".equals(Util.getSymbolicName(resource))) {
                root = resource;
                break;
            }
        }
        List<Resource> mandatory = new ArrayList<Resource>();
        mandatory.add(root);


        Map<Requirement, List<Capability>> candidates = new HashMap<Requirement, List<Capability>>();
        for (Resource resource : repository) {
            for (Requirement requirement : resource.getRequirements(null)) {
                CapabilitySet set = capSets.get(requirement.getNamespace());
                if (set != null) {
                    String filter = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
                    SimpleFilter sf = (filter != null)
                            ? SimpleFilter.parse(filter)
                            : SimpleFilter.convert(requirement.getAttributes());
                    candidates.put(requirement, new ArrayList<Capability>(set.match(sf, true)));
                } else {
                    candidates.put(requirement, Collections.<Capability>emptyList());
                }
            }
        }

        ResolveContextImpl rci = new ResolveContextImpl(Collections.<Resource, Wiring>emptyMap(), candidates, mandatory, Collections.EMPTY_LIST);
        Resolver resolver = new ResolverImpl(new Logger(Logger.LOG_DEBUG));
        Map<Resource, List<Wire>> wireMap = resolver.resolve(rci);


    }

    private static List<Resource> readRepository(Object repository) throws BundleException {
        List<Resource> resources = new ArrayList<Resource>();
        Collection<Map<String, List<String>>> metadatas;
        if (repository instanceof Map) {
            metadatas = ((Map<String, Map<String, List<String>>>) repository).values();
        } else {
            metadatas = (Collection<Map<String, List<String>>>) repository;
        }
        for (Map<String, List<String>> metadata : metadatas) {
            resources.add(BigResolutionTest.parseResource(metadata));
            /*
            ResourceImpl res = new ResourceImpl() {
                @Override
                public String toString() {
                    Capability cap = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
                    return cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE) + "/"
                            + cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                }
            };
            for (String cap : metadata.get("capabilities")) {
                for (Capability c : ResourceBuilder.parseCapability(res, cap)) {
                    res.addCapability(c);
                }
            }
            if (metadata.containsKey("requirements")) {
                for (String req : metadata.get("requirements")) {
                    for (Requirement r : ResourceBuilder.parseRequirement(res, req)) {
                        res.addRequirement(r);
                    }
                }
            }
            resources.add(res);
            */
        }
        return resources;
    }

}
