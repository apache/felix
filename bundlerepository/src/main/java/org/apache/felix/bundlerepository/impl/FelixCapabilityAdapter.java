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
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
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
            HashMap<String, Object> converted = new HashMap<String, Object>(orgMap.size() + 2);

            for (Map.Entry<String, Object> entry : orgMap.entrySet())
            {
                converted.put(NamespaceTranslator.getOSGiNamespace(entry.getKey()), entry.getValue());
            }

            if (BundleNamespace.BUNDLE_NAMESPACE.equals(getNamespace()))
            {
                defaultAttribute(orgMap, converted, BundleNamespace.BUNDLE_NAMESPACE,
                    orgMap.get(org.apache.felix.bundlerepository.Resource.SYMBOLIC_NAME));
                defaultAttribute(orgMap, converted, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                    orgMap.get(org.apache.felix.bundlerepository.Resource.VERSION));
            }
            else if (PackageNamespace.PACKAGE_NAMESPACE.equals(getNamespace()))
            {
                Capability bundleCap = getBundleCapability();
                if (bundleCap != null)
                {
                    defaultAttribute(orgMap, converted, PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE,
                        bundleCap.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
                    defaultAttribute(orgMap, converted, PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
                        bundleCap.getAttributes().get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
                }
            }
            convertedAttributes = converted;
        }
        return convertedAttributes;
    }

    private void defaultAttribute(Map<String, Object> orgMap, Map<String, Object> converted, String newAttr, Object defVal)
    {
        if (converted.get(newAttr) == null)
            converted.put(newAttr, defVal);
    }

    public Map<String, String> getDirectives()
    {
        return capability.getDirectives();
    }

    public String getNamespace()
    {
        return NamespaceTranslator.getOSGiNamespace(capability.getName());
    }

    public Resource getResource()
    {
        return resource;
    }

    private Capability getBundleCapability()
    {
        if (resource == null)
            return null;

        List<Capability> caps = resource.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
        if (caps.size() > 0)
            return caps.get(0);
        else
            return null;
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

    public String toString()
    {
        return resource + ":" + capability;
    }
}
