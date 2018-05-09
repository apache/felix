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
package org.apache.felix.systemready.rootcause;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

public class DSRootCause {

    private static final int MAX_RECURSION = 10;
    
    private ServiceComponentRuntime scr;
    
    public DSRootCause(ServiceComponentRuntime scr) {
        this.scr = scr;
    }
    
    public Optional<DSComp> getRootCause(String iface) {
        return scr.getComponentDescriptionDTOs().stream()
            .filter(desc -> offersInterface(desc, iface))
            .map(this::getRootCause)
            .findFirst();
    }
    
    public DSComp getRootCause(ComponentDescriptionDTO desc) {
        return getRootCause(desc, 0);
    }

    private DSComp getRootCause(ComponentDescriptionDTO desc, int level) {
        if (level > MAX_RECURSION) {
            throw new IllegalStateException("Aborting after because of cyclic references");
        }
        DSComp dsComp = new DSComp();
        dsComp.desc = desc;
        Collection<ComponentConfigurationDTO> instances = scr.getComponentConfigurationDTOs(desc);
        if (instances.isEmpty()) {
            return dsComp;
        }
        for (ComponentConfigurationDTO instance : instances) {
            for (UnsatisfiedReferenceDTO ref : instance.unsatisfiedReferences) {
                ReferenceDTO refdef = getReference(desc, ref.name);
                DSRef unresolvedRef = createRef(ref, refdef);
                unresolvedRef.candidates = getCandidates(ref, refdef, level + 1);
                dsComp.unsatisfied.add(unresolvedRef);
            }
        }
        return dsComp;
    }

    private DSRef createRef(UnsatisfiedReferenceDTO unsatifiedRef, ReferenceDTO refdef) {
        DSRef ref = new DSRef();
        ref.name = unsatifiedRef.name;
        ref.filter = unsatifiedRef.target;
        ref.iface = refdef.interfaceName;
        return ref;
    }

    private List<DSComp> getCandidates(UnsatisfiedReferenceDTO ref, ReferenceDTO refdef, int level) {
        return scr.getComponentDescriptionDTOs().stream()
                .filter(desc -> offersInterface(desc, refdef.interfaceName))
                .map(desc -> getRootCause(desc, level)).collect(Collectors.toList());
    }

    private boolean offersInterface(ComponentDescriptionDTO desc, String interfaceName) {
        return Arrays.asList(desc.serviceInterfaces).contains(interfaceName);
    }

    private ReferenceDTO getReference(ComponentDescriptionDTO desc, String name) {
        return Arrays.asList(desc.references).stream().filter(ref -> ref.name.equals(name)).findFirst().get();
    }

}
