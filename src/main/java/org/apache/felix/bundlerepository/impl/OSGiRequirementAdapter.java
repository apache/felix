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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;

class OSGiRequirementAdapter implements Requirement
{
    private final org.osgi.resource.Requirement requirement;
    private final HashMap<String, String> cleanedDirectives;
    private final String filter;

    public OSGiRequirementAdapter(org.osgi.resource.Requirement requirement)
    {
        this.requirement = requirement;

        String f = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
        if (f != null)
        {
            for (String ns : NamespaceTranslator.getTranslatedOSGiNamespaces())
            {
                f = f.replaceAll("[(][ ]*" + ns + "[ ]*=",
                    "(" + NamespaceTranslator.getFelixNamespace(ns) + "=");
            }
        }
        filter = f;

        cleanedDirectives = new HashMap<String, String>(requirement.getDirectives());
        // Remove directives that are represented as APIs on this class.
        cleanedDirectives.remove(Constants.FILTER_DIRECTIVE);
        cleanedDirectives.remove(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE);
        cleanedDirectives.remove(Constants.RESOLUTION_DIRECTIVE);
    }

    public Map<String, Object> getAttributes()
    {
        return requirement.getAttributes();
    }

    public Map<String, String> getDirectives()
    {

        return cleanedDirectives;
    }

    public String getComment()
    {
        return null;
    }

    public String getFilter()
    {
        return filter;
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
