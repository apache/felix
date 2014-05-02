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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

public class FelixResourceAdapter implements Resource, RepositoryContent
{
    private final org.apache.felix.bundlerepository.Resource resource;

    public FelixResourceAdapter(final org.apache.felix.bundlerepository.Resource resource)
    {
        this.resource = resource;
    }

    public List<Capability> getCapabilities(String namespace)
    {
        ArrayList<Capability> result = new ArrayList<Capability>();

        if (namespace == null || namespace.equals(IdentityNamespace.IDENTITY_NAMESPACE))
        {
            OSGiCapabilityImpl c = OSGiRepositoryImpl.newOSGiIdentityCapability(resource);
            c.setResource(this);
            result.add(c);
        }
        if (namespace == null || namespace.equals(ContentNamespace.CONTENT_NAMESPACE))
        {
            OSGiCapabilityImpl c = OSGiRepositoryImpl.newOSGiContentCapability(resource);
            c.setResource(this);
            result.add(c);
        }

        namespace = NamespaceTranslator.getFelixNamespace(namespace);
        org.apache.felix.bundlerepository.Capability[] capabilities = resource.getCapabilities();
        for (org.apache.felix.bundlerepository.Capability capability : capabilities)
        {
            if (namespace != null && !capability.getName().equals(namespace))
                continue;
            result.add(new FelixCapabilityAdapter(capability, this));
        }
        result.trimToSize();
        return result;
    }

    public InputStream getContent()
    {
        try
        {
            return new URL(resource.getURI()).openStream();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public List<Requirement> getRequirements(String namespace)
    {
        namespace = NamespaceTranslator.getFelixNamespace(namespace);
        org.apache.felix.bundlerepository.Requirement[] requirements = resource.getRequirements();
        ArrayList<Requirement> result = new ArrayList<Requirement>(requirements.length);
        for (final org.apache.felix.bundlerepository.Requirement requirement : requirements)
        {
            if (namespace == null || requirement.getName().equals(namespace))
                result.add(new FelixRequirementAdapter(requirement, this));
        }
        result.trimToSize();
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof Resource))
            return false;
        Resource that = (Resource) o;
        if (!OSGiResourceHelper.getTypeAttribute(that).equals(OSGiResourceHelper.getTypeAttribute(this)))
            return false;
        if (!OSGiResourceHelper.getSymbolicNameAttribute(that).equals(OSGiResourceHelper.getSymbolicNameAttribute(this)))
            return false;
        if (!OSGiResourceHelper.getVersionAttribute(that).equals(OSGiResourceHelper.getVersionAttribute(this)))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + OSGiResourceHelper.getTypeAttribute(this).hashCode();
        result = 31 * result + OSGiResourceHelper.getSymbolicNameAttribute(this).hashCode();
        result = 31 * result + OSGiResourceHelper.getVersionAttribute(this).hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        Capability c = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next();
        Map<String, Object> atts = c.getAttributes();
        return new StringBuilder().append(atts.get(IdentityNamespace.IDENTITY_NAMESPACE)).append(';')
                .append(atts.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE)).append(';')
                .append(atts.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)).toString();
    }
}
