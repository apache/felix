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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.felix.bundlerepository.impl.LazyHashMap.LazyValue;
import org.osgi.framework.Version;
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
        if (namespace == null || namespace.equals(IdentityNamespace.IDENTITY_NAMESPACE))
        {
            Capability c = newOsgiIdentityCapability(this, resource.getSymbolicName(), resource.getVersion());
            return Collections.singletonList(c);
        }
        if (namespace.equals(ContentNamespace.CONTENT_NAMESPACE))
        {
            Capability c = newOsgiContentCapability(this, resource.getURI(), resource.getSize());
            return Collections.singletonList(c);
        }

        namespace = NamespaceTranslator.getFelixNamespace(namespace);
        org.apache.felix.bundlerepository.Capability[] capabilities = resource.getCapabilities();
        ArrayList<Capability> result = new ArrayList<Capability>(capabilities.length);
        for (org.apache.felix.bundlerepository.Capability capability : capabilities)
        {
            if (namespace != null && !capability.getName().equals(namespace))
                continue;
            result.add(new FelixCapabilityAdapter(capability, this));
        }
        result.trimToSize();
        return result;
    }

    private static Capability newOsgiIdentityCapability(Resource res, String symbolicName, Version version)
    {
        Map<String, Object> idAttrs = new HashMap<String, Object>();
        idAttrs.put(IdentityNamespace.IDENTITY_NAMESPACE, symbolicName);
        idAttrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE);
        idAttrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);

        return new OSGiCapabilityImpl(IdentityNamespace.IDENTITY_NAMESPACE, idAttrs, Collections.<String, String> emptyMap(), res);
    }

    private static Capability newOsgiContentCapability(Resource res, final String uri, long size)
    {
        // TODO duplicated in OSGiRepositoryImpl
        LazyValue<String, Object> lazyValue =
            new LazyValue<String, Object>(ContentNamespace.CONTENT_NAMESPACE, new Callable<Object>() {
                public Object call() throws Exception
                {
                    // This is expensive to do, so only compute it when actually obtained...
                    return OSGiRepositoryImpl.getSHA256(uri);
                }
            });
        Map<String, Object> contentAttrs = new LazyHashMap<String, Object>(Collections.singleton(lazyValue));
        contentAttrs.put(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, "application/vnd.osgi.bundle");
        contentAttrs.put(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, size);
        contentAttrs.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, uri);
        return new OSGiCapabilityImpl(ContentNamespace.CONTENT_NAMESPACE, contentAttrs, Collections.<String, String> emptyMap());
    }

    public InputStream getContent()
    {
        try
        {
            return new URL(resource.getURI()).openStream();
        } catch (Exception e)
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
