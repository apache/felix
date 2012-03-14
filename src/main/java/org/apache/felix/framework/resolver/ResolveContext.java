/*
 * Copyright (c) OSGi Alliance (2011, 2012). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.framework.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

public abstract class ResolveContext
{
    public Collection<BundleRevision> getMandatoryRevisions()
    {
        return emptyCollection();
    }

    public Collection<BundleRevision> getOptionalRevisions()
    {
        return emptyCollection();
    }

    private static <T> Collection<T> emptyCollection()
    {
        return Collections.EMPTY_LIST;
    }

    public abstract List<BundleCapability> findProviders(BundleRequirement br, boolean obeyMandatory);

    public abstract int insertHostedCapability(List<BundleCapability> caps, HostedCapability hc);

    public abstract boolean isEffective(BundleRequirement br);

    public abstract Map<BundleRevision, BundleWiring> getWirings();
}
