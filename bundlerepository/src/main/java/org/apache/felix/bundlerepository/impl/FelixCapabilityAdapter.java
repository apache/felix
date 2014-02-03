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
import org.osgi.resource.Resource;

public class FelixCapabilityAdapter implements Capability
{
    private final org.apache.felix.bundlerepository.Capability capability;
    private final Resource resource;
    private volatile Map<String, Object> convertedAttributes;

    public FelixCapabilityAdapter(org.apache.felix.bundlerepository.Capability capability, Resource resource)
    {
        if (capability == null)
            throw new NullPointerException("Missing required parameter: capability");
        this.capability = capability;
        this.resource = resource;
    }

    public Map<String, Object> getAttributes()
    {
        if (convertedAttributes == null)
        {
            Map<String, Object> orgMap = capability.getPropertiesAsMap();
            HashMap<String, Object> converted = new HashMap<String, Object>(orgMap.size());

            for (Map.Entry<String, Object> entry : orgMap.entrySet())
            {
                converted.put(NamespaceTranslator.getOSGiNamespace(entry.getKey()), entry.getValue());
            }
            convertedAttributes = converted; // Cache the result
        }
        return convertedAttributes;
    }

    public Map<String, String> getDirectives()
    {
        return Collections.emptyMap();
    }

    public String getNamespace()
    {
        return NamespaceTranslator.getOSGiNamespace(capability.getName());
    }

    public Resource getResource()
    {
        return resource;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof Capability))
            return false;
        Capability c = (Capability) o;
        return c.getNamespace().equals(getNamespace()) && c.getAttributes().equals(getAttributes())
                && c.getDirectives().equals(getDirectives()) && c.getResource().equals(getResource());
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + getNamespace().hashCode();
        result = 31 * result + getAttributes().hashCode();
        result = 31 * result + getDirectives().hashCode();
        result = 31 * result + getResource().hashCode();
        return result;
    }
}
