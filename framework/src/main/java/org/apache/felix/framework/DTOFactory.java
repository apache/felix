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
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.framework.wiring.dto.BundleWireDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO.NodeDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.CapabilityRefDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.resource.dto.RequirementRefDTO;
import org.osgi.resource.dto.WireDTO;

/**
 * Creates various DTOs provided by the core framework.
 */
public class DTOFactory
{
    private DTOFactory()
    {
        // Only static methods
    }

    static <T> T createDTO(Bundle bundle, Class<T> type)
    {
        if (Bundle.UNINSTALLED == bundle.getState())
            return null;

        if (type == BundleDTO.class)
        {
            return type.cast(createBundleDTO(bundle));
        }
        else if (type == BundleStartLevelDTO.class)
        {
            return type.cast(createBundleStartLevelDTO(bundle));
        }
        else if (type == BundleRevisionDTO.class)
        {
            return type.cast(createBundleRevisionDTO(bundle));
        }
        else if (type == BundleRevisionDTO[].class)
        {
            return type.cast(createBundleRevisionDTOArray(bundle));
        }
        else if (type == BundleWiringDTO.class)
        {
            return type.cast(createBundleWiringDTO(bundle));
        }
        else if (type == BundleWiringDTO[].class)
        {
            return type.cast(createBundleWiringDTOArray(bundle));
        }
        else if (type == ServiceReferenceDTO[].class)
        {
            return type.cast(createServiceReferenceDTOArray(bundle));
        }
        else if (type == FrameworkDTO.class && bundle instanceof Felix)
        {
            return type.cast(createFrameworkDTO((Felix) bundle));
        }
        else if (type == FrameworkStartLevelDTO.class && bundle instanceof Framework)
        {
            return type.cast(createFrameworkStartLevelDTO((Framework) bundle));
        }
        return null;
    }

    private static BundleDTO createBundleDTO(Bundle bundle)
    {
        BundleDTO dto = new BundleDTO();
        dto.id = bundle.getBundleId();
        dto.lastModified = bundle.getLastModified();
        dto.state = bundle.getState();
        dto.symbolicName = bundle.getSymbolicName();
        dto.version = "" + bundle.getVersion();
        return dto;
    }

    private static BundleRevisionDTO createBundleRevisionDTO(Bundle bundle)
    {
        BundleRevision br = bundle.adapt(BundleRevision.class);
        if (!(br instanceof BundleRevisionImpl))
            return null;

        return createBundleRevisionDTO(bundle, (BundleRevisionImpl) br, new HashSet<BundleRevisionDTO>());
    }

    private static BundleRevisionDTO[] createBundleRevisionDTOArray(Bundle bundle)
    {
        BundleRevisions brs = bundle.adapt(BundleRevisions.class);
        if (brs == null || brs.getRevisions() == null)
            return null;

        List<BundleRevision> revisions = brs.getRevisions();
        BundleRevisionDTO[] dtos = new BundleRevisionDTO[revisions.size()];
        for (int i=0; i < revisions.size(); i++)
        {
            if (revisions.get(i) instanceof BundleRevisionImpl)
                dtos[i] = createBundleRevisionDTO(bundle, (BundleRevisionImpl) revisions.get(i), new HashSet<BundleRevisionDTO>());
        }
        return dtos;
    }

    private static BundleRevisionDTO createBundleRevisionDTO(BundleRevision revision, Set<BundleRevisionDTO> resources)
    {
        if (revision instanceof BundleRevisionImpl)
            return createBundleRevisionDTO(revision.getBundle(), (BundleRevisionImpl) revision, resources);
        else
            return null;
    }

    private static BundleRevisionDTO createBundleRevisionDTO(Bundle bundle, BundleRevisionImpl revision, Set<BundleRevisionDTO> resources)
    {
        BundleRevisionDTO dto = new BundleRevisionDTO();
        dto.id = getRevisionID(revision);
        addBundleRevisionDTO(dto, resources);

        dto.bundle = bundle.getBundleId();
        dto.symbolicName = revision.getSymbolicName();
        dto.type = revision.getTypes();
        dto.version = revision.getVersion().toString();

        dto.capabilities = new ArrayList<CapabilityDTO>();
        for (Capability cap : revision.getCapabilities(null))
        {
            CapabilityDTO cdto = new CapabilityDTO();
            cdto.id = getCapabilityID(cap);
            cdto.namespace = cap.getNamespace();
            cdto.attributes = convertAttrsToDTO(cap.getAttributes());
            cdto.directives = new HashMap<String, String>(cap.getDirectives());
            cdto.resource = getResourceIDAndAdd(cap.getResource(), resources);

            dto.capabilities.add(cdto);
        }

        dto.requirements = new ArrayList<RequirementDTO>();
        for (Requirement req : revision.getRequirements(null))
        {
            RequirementDTO rdto = new RequirementDTO();
            rdto.id = getRequirementID(req);
            rdto.namespace = req.getNamespace();
            rdto.attributes = convertAttrsToDTO(req.getAttributes());
            rdto.directives = new HashMap<String, String>(req.getDirectives());
            rdto.resource = getResourceIDAndAdd(req.getResource(), resources);

            dto.requirements.add(rdto);
        }
        return dto;
    }

    private static BundleWiringDTO createBundleWiringDTO(Bundle bundle)
    {
        BundleWiring bw = bundle.adapt(BundleWiring.class);
        return createBundleWiringDTO(bw);
    }

    private static BundleWiringDTO createBundleWiringDTO(BundleWiring wiring)
    {
        BundleWiringDTO dto = new BundleWiringDTO();
        dto.bundle = wiring.getBundle().getBundleId();
        dto.root = getWiringID(wiring);
        dto.nodes = new HashSet<BundleWiringDTO.NodeDTO>();
        dto.resources = new HashSet<BundleRevisionDTO>();

        createBundleRevisionDTO(wiring.getRevision(), dto.resources);
        createBundleWiringNodeDTO(wiring, dto.resources, dto.nodes);

        return dto;
    }

    private static BundleWiringDTO[] createBundleWiringDTOArray(Bundle bundle)
    {
        BundleRevisions brs = bundle.adapt(BundleRevisions.class);
        if (brs == null || brs.getRevisions() == null)
            return null;

        List<BundleRevision> revisions = brs.getRevisions();
        BundleWiringDTO[] dtos = new BundleWiringDTO[revisions.size()];
        for (int i=0; i < revisions.size(); i++)
        {
            BundleWiring wiring = revisions.get(i).getWiring();
            dtos[i] = createBundleWiringDTO(wiring);
        }
        return dtos;
    }

    private static void createBundleWiringNodeDTO(BundleWiring bw, Set<BundleRevisionDTO> resources, Set<NodeDTO> nodes)
    {
        NodeDTO node = new BundleWiringDTO.NodeDTO();
        node.id = getWiringID(bw);
        nodes.add(node);

        node.current = bw.isCurrent();
        node.inUse = bw.isInUse();
        node.resource = getResourceIDAndAdd(bw.getResource(), resources);

        node.capabilities = new ArrayList<CapabilityRefDTO>();
        for (Capability cap : bw.getCapabilities(null))
        {
            CapabilityRefDTO cdto = new CapabilityRefDTO();
            cdto.capability = getCapabilityID(cap);
            cdto.resource = getResourceIDAndAdd(cap.getResource(), resources);
            node.capabilities.add(cdto);
        }

        node.requirements = new ArrayList<RequirementRefDTO>();
        for (Requirement req : bw.getRequirements(null))
        {
            RequirementRefDTO rdto = new RequirementRefDTO();
            rdto.requirement = getRequirementID(req);
            rdto.resource = getResourceIDAndAdd(req.getResource(), resources);
            node.requirements.add(rdto);
        }

        node.providedWires = new ArrayList<WireDTO>();
        for (Wire pw : bw.getProvidedWires(null))
        {
            node.providedWires.add(createBundleWireDTO(pw, resources, nodes));
        }

        node.requiredWires = new ArrayList<WireDTO>();
        for (Wire rw : bw.getRequiredWires(null))
        {
            node.requiredWires.add(createBundleWireDTO(rw, resources, nodes));
        }
    }

    private static BundleWireDTO createBundleWireDTO(Wire wire, Set<BundleRevisionDTO> resources, Set<NodeDTO> nodes)
    {
        BundleWireDTO wdto = new BundleWireDTO();
        if (wire instanceof BundleWire)
        {
            BundleWire w = (BundleWire) wire;

            BundleWiring pw = w.getProviderWiring();
            addWiringNodeIfNotPresent(pw, resources, nodes);
            wdto.providerWiring = getWiringID(pw);

            BundleWiring rw = w.getRequirerWiring();
            addWiringNodeIfNotPresent(rw, resources, nodes);
            wdto.requirerWiring = getWiringID(rw);
        }
        wdto.provider = getResourceIDAndAdd(wire.getProvider(), resources);
        wdto.requirer = getResourceIDAndAdd(wire.getRequirer(), resources);
        wdto.capability = new CapabilityRefDTO();
        wdto.capability.capability = getCapabilityID(wire.getCapability());
        wdto.capability.resource = getResourceIDAndAdd(wire.getCapability().getResource(), resources);
        wdto.requirement = new RequirementRefDTO();
        wdto.requirement.requirement = getRequirementID(wire.getRequirement());
        wdto.requirement.resource = getResourceIDAndAdd(wire.getRequirement().getResource(), resources);
        return wdto;
    }

    private static BundleStartLevelDTO createBundleStartLevelDTO(Bundle bundle)
    {
        BundleStartLevelDTO dto = new BundleStartLevelDTO();
        dto.bundle = bundle.getBundleId();

        BundleStartLevel sl = bundle.adapt(BundleStartLevel.class);
        dto.activationPolicyUsed = sl.isActivationPolicyUsed();
        dto.persistentlyStarted = sl.isPersistentlyStarted();
        dto.startLevel = sl.getStartLevel();

        return dto;
    }

    private static ServiceReferenceDTO[] createServiceReferenceDTOArray(Bundle bundle)
    {
        BundleContext ctx = bundle.getBundleContext();
        if (ctx == null)
            return null;

        ServiceReference<?>[] svcs = bundle.getRegisteredServices();
        if (svcs == null)
            return new ServiceReferenceDTO[0];

        ServiceReferenceDTO[] dtos = new ServiceReferenceDTO[svcs.length];
        for (int i=0; i < svcs.length; i++)
        {
            dtos[i] = createServiceReferenceDTO(svcs[i]);
        }
        return dtos;
    }

    private static ServiceReferenceDTO createServiceReferenceDTO(ServiceReference<?> svc)
    {
        ServiceReferenceDTO dto = new ServiceReferenceDTO();
        dto.bundle = svc.getBundle().getBundleId();
        dto.id = (Long) svc.getProperty(Constants.SERVICE_ID);
        Map<String, Object> props = new HashMap<String, Object>();
        for (String key : svc.getPropertyKeys())
        {
            props.put(key, svc.getProperty(key));
        }
        dto.properties = new HashMap<String, Object>(props);

        Bundle[] ubs = svc.getUsingBundles();
        if (ubs == null)
        {
            dto.usingBundles = new long[0];
        }
        else
        {
            dto.usingBundles = new long[ubs.length];
            for (int j=0; j < ubs.length; j++)
            {
                dto.usingBundles[j] = ubs[j].getBundleId();
            }
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static FrameworkDTO createFrameworkDTO(Felix framework)
    {
        FrameworkDTO dto = new FrameworkDTO();
        dto.properties = convertAttrsToDTO(framework.getConfig());

        dto.bundles = new ArrayList<BundleDTO>();
        for (Bundle b : framework.getBundleContext().getBundles())
        {
            dto.bundles.add(DTOFactory.createDTO(b, BundleDTO.class));
        }

        dto.services = new ArrayList<ServiceReferenceDTO>();

        ServiceReference<?>[] refs = null;
        try
        {
            refs = framework.getBundleContext().getAllServiceReferences(null, null);
        }
        catch (InvalidSyntaxException e)
        {
            // No filter, should never happen
        }

        for (ServiceReference<?> sr : refs)
        {
            dto.services.add(createServiceReferenceDTO(sr));
        }

        return dto;
    }

    private static FrameworkStartLevelDTO createFrameworkStartLevelDTO(Framework framework)
    {
        FrameworkStartLevel fsl = framework.adapt(FrameworkStartLevel.class);

        FrameworkStartLevelDTO dto = new FrameworkStartLevelDTO();
        dto.initialBundleStartLevel = fsl.getInitialBundleStartLevel();
        dto.startLevel = fsl.getStartLevel();

        return dto;
    }

    private static void addBundleRevisionDTO(BundleRevisionDTO dto, Set<BundleRevisionDTO> resources)
    {
        for (BundleRevisionDTO r : resources)
        {
            if (r.id == dto.id)
                return;
        }
        resources.add(dto);
    }

    private static void addWiringNodeIfNotPresent(BundleWiring bw, Set<BundleRevisionDTO> resources, Set<NodeDTO> nodes)
    {
        int wiringID = getWiringID(bw);
        for (NodeDTO n : nodes)
        {
            if (n.id == wiringID)
                return;
        }
        createBundleWiringNodeDTO(bw, resources, nodes);
    }

    // Attributes contain Version values which are not supported for DTOs, so if
    // these are found they need to be converted to String values.
    private static Map<String, Object> convertAttrsToDTO(Map<String, Object> map)
    {
        Map<String, Object> m = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            Object value = convertAttrToDTO(entry.getValue());
            if (value != null)
            {
                m.put(entry.getKey(), value);
            }
        }
        return m;
    }

    private static Object convertAttrToDTO(Object value)
    {
        if (value instanceof Version)
        {
            return value.toString();
        }
        else if (isPermissibleAttribute(value.getClass())
                || (value.getClass().isArray()
                && isPermissibleAttribute(value.getClass().getComponentType())))
        {
            return value;
        }
        else
        {
            return null;
        }
    }

    private static boolean isPermissibleAttribute(Class clazz)
    {
        return clazz == Boolean.class || clazz == String.class
                || DTO.class.isAssignableFrom(clazz);
    }

    private static int getWiringID(Wiring bw)
    {
        return bw.hashCode();
    }

    private static int getCapabilityID(Capability capability)
    {
        return capability.hashCode();
    }

    private static int getRequirementID(Requirement requirement)
    {
        return requirement.hashCode();
    }

    private static int getResourceIDAndAdd(Resource res, Set<BundleRevisionDTO> resources)
    {
        if (res instanceof BundleRevisionImpl)
        {
            BundleRevisionImpl bres = (BundleRevisionImpl) res;
            int id = bres.getId().hashCode();

            if (resources == null)
                return id;

            for (BundleRevisionDTO rdto : resources)
            {
                if (rdto.id == id)
                    return id;
            }
            createBundleRevisionDTO(bres, resources);
            return id;
        }
        return res.hashCode();
    }

    private static int getRevisionID(BundleRevisionImpl revision)
    {
        return revision.getId().hashCode();
    }
}
