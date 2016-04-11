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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class FelixRequirementAdapter implements Requirement
{
    private final Map<String, String> directives;
    private final org.apache.felix.bundlerepository.Requirement requirement;
    private final Resource resource;

    public FelixRequirementAdapter(org.apache.felix.bundlerepository.Requirement requirement, Resource resource)
    {
        if (requirement == null)
            throw new NullPointerException("Missing required parameter: requirement");
        if (resource == null)
            throw new NullPointerException("Missing required parameter: resource");
        this.requirement = requirement;
        this.resource = resource;
        this.directives = computeDirectives();
    }

    public Map<String, Object> getAttributes()
    {
        return requirement.getAttributes();
    }

    public Map<String, String> getDirectives()
    {
        return directives;
    }

    public String getNamespace()
    {
        return NamespaceTranslator.getOSGiNamespace(requirement.getName());
    }

    public Resource getResource()
    {
        return resource;
    }

    public boolean matches(Capability capability)
    {
        return requirement.isSatisfied(new OSGiCapabilityAdapter(capability));
    }

    private Map<String, String> computeDirectives()
    {
        Map<String, String> result;
        if (requirement.getDirectives() == null)
            result = new HashMap<String, String>();
        else
            result = new HashMap<String, String>(requirement.getDirectives());

        /*
         * (1) The Felix OBR specific "mandatory:<*" syntax must be stripped out
         * of the filter.
         * (2) service references removed
         * (3) objectClass capitalised
         * (4) The namespaces must be translated.
         */
        String filter = requirement.getFilter().replaceAll("\\(mandatory\\:\\<\\*[^\\)]*\\)", "").
                replaceAll("objectclass", "objectClass");

        for (String ns : NamespaceTranslator.getTranslatedFelixNamespaces())
        {
            filter = filter.replaceAll("[(][ ]*" + ns + "[ ]*=",
                "(" + NamespaceTranslator.getOSGiNamespace(ns) + "=");
        }
        result.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);

        if (requirement.isOptional())
            result.put(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);

        if (requirement.isMultiple())
            result.put(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE, Namespace.CARDINALITY_MULTIPLE);

        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof Requirement))
            return false;
        Requirement c = (Requirement) o;
        return c.getNamespace().equals(getNamespace()) && c.getAttributes().equals(getAttributes())
                && c.getDirectives().equals(getDirectives()) && c.getResource() != null ? c.getResource().equals(getResource())
                : getResource() == null;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + getNamespace().hashCode();
        result = 31 * result + getAttributes().hashCode();
        result = 31 * result + getDirectives().hashCode();
        result = 31 * result + (getResource() == null ? 0 : getResource().hashCode());
        return result;
    }

    public String toString()
    {
        return resource + ":" + requirement;
    }
}
