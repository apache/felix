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
package org.apache.felix.resolver.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

public class IterativeResolver implements Resolver {

    private final Resolver resolver;

    public IterativeResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    public Map<Resource, List<Wire>> resolve(final ResolveContext context) throws ResolutionException {

        final Map<Resource, List<Wire>> wires = new HashMap<Resource, List<Wire>>();
        final Map<Resource, List<Wire>> invertedWires = new HashMap<Resource, List<Wire>>();
        Collection<Resource> resources = context.getMandatoryResources();
        for (final Resource resource : resources) {
            // Build wiring
            invertedWires.clear();
            for (Resource res : wires.keySet()) {
                for (Wire wire : wires.get(res)) {
                    List<Wire> w = invertedWires.get(wire.getProvider());
                    if (w == null) {
                        w = new ArrayList<Wire>();
                        invertedWires.put(wire.getProvider(), w);
                    }
                    w.add(wire);
                }
            }
            final Map<Resource, Wiring> wiring = new HashMap<Resource, Wiring>();
            for (Resource res : wires.keySet()) {
                wiring.put(res, new SimpleWiring(res, wires, invertedWires));
            }
            // Resolve the new resource
            Map<Resource, List<Wire>> tempWires = resolver.resolve(new ResolveContext() {
                @Override
                public Collection<Resource> getMandatoryResources() {
                    return Collections.singleton(resource);
                }

                @Override
                public Collection<Resource> getOptionalResources() {
                    return context.getOptionalResources();
                }

                @Override
                public List<Capability> findProviders(Requirement requirement) {
                    return context.findProviders(requirement);
                }

                @Override
                public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
                    return context.insertHostedCapability(capabilities, hostedCapability);
                }

                @Override
                public boolean isEffective(Requirement requirement) {
                    return context.isEffective(requirement);
                }

                @Override
                public Map<Resource, Wiring> getWirings() {
                    return wiring;
                }
            });
            // Merge wiring
            wires.putAll(tempWires);
        }

        return wires;
    }

    private class SimpleWiring implements Wiring {
        final Resource resource;
        final Map<Resource, List<Wire>> wires;
        final Map<Resource, List<Wire>> invertedWires;
        List<Capability> resourceCapabilities;
        List<Requirement> resourceRequirements;

        private SimpleWiring(Resource resource, Map<Resource, List<Wire>> wires, Map<Resource, List<Wire>> invertedWires) {
            this.resource = resource;
            this.wires = wires;
            this.invertedWires = invertedWires;
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
