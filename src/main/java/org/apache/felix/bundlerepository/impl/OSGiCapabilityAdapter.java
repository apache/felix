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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;

public class OSGiCapabilityAdapter implements Capability
{
    private final org.osgi.resource.Capability capability;

    public OSGiCapabilityAdapter(org.osgi.resource.Capability capability)
    {
        this.capability = capability;
    }

    @Override
    public boolean equals(Object o)
    {
        return capability.equals(o);
    }

    public String getName()
    {
        return NamespaceTranslator.getFelixNamespace(capability.getNamespace());
    }

    public Property[] getProperties()
    {
        Map<String, Object> attributes = capability.getAttributes();
        Collection<Property> result = new ArrayList<Property>(attributes.size());
        for (final Map.Entry<String, Object> entry : capability.getAttributes().entrySet())
        {
            if (entry.getKey().equals(capability.getNamespace()))
            {
                result.add(new FelixPropertyAdapter(getName(), entry.getValue()));
                continue;
            }
            result.add(new FelixPropertyAdapter(entry));
        }
        return result.toArray(new Property[result.size()]);
    }

    public Map<String, Object> getPropertiesAsMap()
    {
        Map<String, Object> result = new HashMap<String, Object>(capability.getAttributes());
        result.put(getName(), result.get(capability.getNamespace()));
        return result;
    }

    public Map<String, String> getDirectives() {
        return Collections.unmodifiableMap(capability.getDirectives());
    }

    @Override
    public int hashCode()
    {
        return capability.hashCode();
    }
}
