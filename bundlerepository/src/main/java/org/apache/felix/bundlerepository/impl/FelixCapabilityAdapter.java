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
import java.util.Map;

import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class FelixCapabilityAdapter implements Capability
{
    private final org.apache.felix.bundlerepository.Capability capability;
    private final Resource resource;

    public FelixCapabilityAdapter(org.apache.felix.bundlerepository.Capability capability, Resource resource)
    {
        if (capability == null)
            throw new NullPointerException("Missing required parameter: capability");
        this.capability = capability;
        this.resource = resource;
    }

    public Map<String, Object> getAttributes()
    {
        Map<String, Object> result = capability.getPropertiesAsMap();
        String namespace = getNamespace();
        if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace))
            result.put(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE,
                    result.get(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE.toLowerCase()));
        else if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace))
            result.put(BundleNamespace.BUNDLE_NAMESPACE, result.get(org.apache.felix.bundlerepository.Resource.SYMBOLIC_NAME));
        else
            result.put(namespace, result.get(capability.getName()));
        return result;
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
