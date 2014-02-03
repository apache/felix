/*
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
package org.apache.felix.bundlerepository.impl;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;

class OSGiRequirementAdapter implements Requirement
{
    private final org.osgi.resource.Requirement requirement;

    public OSGiRequirementAdapter(org.osgi.resource.Requirement requirement)
    {
        this.requirement = requirement;
    }

    public String getComment()
    {
        return null;
    }

    public String getFilter()
    {
        return requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
    }

    public String getName()
    {
        return NamespaceTranslator.getFelixNamespace(requirement.getNamespace());
    }

    public boolean isExtend()
    {
        return false;
    }

    public boolean isMultiple()
    {
        String multiple = requirement.getDirectives().get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE);
        return Namespace.CARDINALITY_MULTIPLE.equals(multiple);
    }

    public boolean isOptional()
    {
        String resolution = requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
        return Constants.RESOLUTION_OPTIONAL.equals(resolution);
    }

    public boolean isSatisfied(Capability capability)
    {
        boolean result = OSGiResourceHelper.matches(requirement, new FelixCapabilityAdapter(capability, null));
        return result;
    }
}
