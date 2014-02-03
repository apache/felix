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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

public class OSGiResourceHelper
{
    public static String getContentAttribute(Resource resource)
    {
        return (String) getContentAttribute(resource, ContentNamespace.CONTENT_NAMESPACE);
    }

    public static Object getContentAttribute(Resource resource, String name)
    {
        List<Capability> capabilities = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        Capability capability = capabilities.get(0);
        return capability.getAttributes().get(name);
    }

    public static Object getIdentityAttribute(Resource resource, String name)
    {
        List<Capability> capabilities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        Capability capability = capabilities.get(0);
        return capability.getAttributes().get(name);
    }

    public static Resource getResource(Requirement requirement, Repository repository)
    {
        Map<Requirement, Collection<Capability>> map = repository.findProviders(Arrays.asList(requirement));
        Collection<Capability> capabilities = map.get(requirement);
        return capabilities == null ? null : capabilities.size() == 0 ? null : capabilities.iterator().next().getResource();
    }

    public static String getSymbolicNameAttribute(Resource resource)
    {
        return (String) getIdentityAttribute(resource, IdentityNamespace.IDENTITY_NAMESPACE);
    }

    public static String getTypeAttribute(Resource resource)
    {
        String result = (String) getIdentityAttribute(resource, IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
        if (result == null)
            result = IdentityNamespace.TYPE_BUNDLE;
        return result;
    }

    public static Version getVersionAttribute(Resource resource)
    {
        Version result = (Version) getIdentityAttribute(resource, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
        if (result == null)
            result = Version.emptyVersion;
        return result;
    }

    public static boolean matches(Requirement requirement, Capability capability)
    {
        boolean result = false;
        if (requirement == null && capability == null)
            result = true;
        else if (requirement == null || capability == null)
            result = false;
        else if (!capability.getNamespace().equals(requirement.getNamespace()))
            result = false;
        else
        {
            String filterStr = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
            if (filterStr == null)
                result = true;
            else
            {
                try
                {
                    if (FilterImpl.newInstance(filterStr).matchCase(capability.getAttributes()))
                        result = true;
                }
                catch (InvalidSyntaxException e)
                {
                    result = false;
                }
            }
        }
        // TODO Check directives.
        return result;
    }
}
