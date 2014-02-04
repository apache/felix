/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.bundlerepository.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.service.repository.ContentNamespace;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SpecXMLPullParser
{
    private static final String ATTRIBUTE = "attribute";
    private static final String CAPABILITY = "capability";
    private static final String DIRECTIVE = "directive";
    private static final String INCREMENT = "increment";
    private static final String NAME = "name";
    private static final String NAMESPACE = "namespace";
    private static final String REFERRAL = "referral";
    private static final String REPOSITORY = "repository";
    private static final String REQUIREMENT = "requirement";
    private static final String RESOURCE = "resource";

    public static RepositoryImpl parse(XmlPullParser reader) throws Exception
    {
        RepositoryImpl repository = new RepositoryImpl();

        for (int i = 0, ac = reader.getAttributeCount(); i < ac; i++)
        {
            String name = reader.getAttributeName(i);
            String value = reader.getAttributeValue(i);
            if (NAME.equals(name))
                repository.setName(value);
            else if (INCREMENT.equals(name))
                repository.setLastModified(value); // TODO increment is not necessarily a timestamp
        }

        int event;
        while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
        {
            String element = reader.getName();
            if (REFERRAL.equals(element))
            {
                // TODO
            }
            else if (RESOURCE.equals(element))
            {
                Resource resource = parseResource(reader);
                repository.addResource(resource);
            }
            else
            {
                PullParser.ignoreTag(reader);
            }
        }

        PullParser.sanityCheckEndElement(reader, event, REPOSITORY);
        return repository;
    }

    private static Resource parseResource(XmlPullParser reader) throws Exception
    {
        ResourceImpl resource = new ResourceImpl();
        try
        {
            int event;
            while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
            {
                String element = reader.getName();
                if (CAPABILITY.equals(element))
                {
                    Capability capability = parseCapability(reader, resource);
                    if (capability != null)
                        resource.addCapability(capability);
                }
                else if (REQUIREMENT.equals(element))
                {
                    Requirement requirement = parseRequirement(reader);
                    resource.addRequire(requirement);
                }
                else
                {
                    PullParser.ignoreTag(reader);
                }
            }

            PullParser.sanityCheckEndElement(reader, event, RESOURCE);
            return resource;
        }
        catch (Exception e)
        {
            throw new Exception("Error while parsing resource " + resource.getId() + " at line " + reader.getLineNumber() + " and column " + reader.getColumnNumber(), e);
        }
    }

    private static Capability parseCapability(XmlPullParser reader, ResourceImpl resource) throws Exception
    {
        String namespace = reader.getAttributeValue(null, NAMESPACE);
        if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace))
        {
            parseIdentityNamespace(reader, resource);
            return null;
        }
        if (ContentNamespace.CONTENT_NAMESPACE.equals(namespace))
        {
            parseContentNamespace(reader, resource);
            return null;
        }

        CapabilityImpl capability = new CapabilityImpl();
        if (!namespace.equals(NamespaceTranslator.getOSGiNamespace(namespace)))
            throw new Exception("Namespace conflict. Namespace not allowed: " + namespace);

        capability.setName(NamespaceTranslator.getFelixNamespace(namespace));
        Map<String, Object> attributes = new HashMap<String, Object>();
        parseAttributesDirectives(reader, attributes, CAPABILITY);

        for (Map.Entry<String, Object> entry : attributes.entrySet())
        {
            capability.addProperty(new FelixPropertyAdapter(entry.getKey(), entry.getValue()));
        }

        return capability;
    }

    private static void parseIdentityNamespace(XmlPullParser reader, ResourceImpl resource) throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        parseAttributesDirectives(reader, attributes, CAPABILITY);

        for (Map.Entry<String, Object> entry : attributes.entrySet())
        {
            if (IdentityNamespace.IDENTITY_NAMESPACE.equals(entry.getKey()))
                resource.put(Resource.SYMBOLIC_NAME, entry.getValue());
            else
                resource.put(entry.getKey(), entry.getValue());
        }
    }

    private static void parseContentNamespace(XmlPullParser reader, ResourceImpl resource) throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        parseAttributesDirectives(reader, attributes, CAPABILITY);

        for (Map.Entry<String, Object> entry : attributes.entrySet())
        {
            if (ContentNamespace.CONTENT_NAMESPACE.equals(entry.getKey()))
                // TODO we should really check the SHA
                continue;
            else if (ContentNamespace.CAPABILITY_URL_ATTRIBUTE.equals(entry.getKey()))
                resource.put(Resource.URI, entry.getValue());
            else
                resource.put(entry.getKey(), entry.getValue());
        }
    }

    private static void parseAttributesDirectives(XmlPullParser reader, Map<String, Object> attributes, String parentTag) throws XmlPullParserException, IOException
    {
        int event;
        while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
        {
            String element = reader.getName();
            if (ATTRIBUTE.equals(element))
            {
                String name = reader.getAttributeValue(null, "name");
                String type = reader.getAttributeValue(null, "type");
                String value = reader.getAttributeValue(null, "value");
                attributes.put(name, getTypedValue(type, value));
                PullParser.sanityCheckEndElement(reader, reader.nextTag(), ATTRIBUTE);
            }
            else
            {
                PullParser.ignoreTag(reader);
            }
        }
        PullParser.sanityCheckEndElement(reader, event, parentTag);
    }

    private static Object getTypedValue(String type, String value)
    {
        if (type == null)
            return value;

        type = type.trim();
        if ("Version".equals(type))
            return Version.parseVersion(value);
        else if ("Long".equals(type))
            return Long.parseLong(value);
        else if ("Double".equals(type))
            return Double.parseDouble(value);
        return value;
    }

    private static Requirement parseRequirement(XmlPullParser reader) throws Exception
    {
        RequirementImpl requirement = new RequirementImpl();
        String namespace = reader.getAttributeValue(null, NAMESPACE);
        if (!namespace.equals(NamespaceTranslator.getOSGiNamespace(namespace)))
            throw new Exception("Namespace conflict. Namespace not allowed: " + namespace);

        requirement.setName(NamespaceTranslator.getFelixNamespace(namespace));

        Map<String, String> directives = new HashMap<String, String>();
        int event;
        while ((event = reader.nextTag()) == XmlPullParser.START_TAG)
        {
            String element = reader.getName();
            if (DIRECTIVE.equals(element))
            {
                String name = reader.getAttributeValue(null, "name");
                String value = reader.getAttributeValue(null, "value");
                directives.put(name, value);
                PullParser.sanityCheckEndElement(reader, reader.nextTag(), DIRECTIVE);
            }
            else
            {
                PullParser.ignoreTag(reader);
            }
        }

        requirement.setExtend(false);
        // TODO transform the namespaces in the filter!
        requirement.setFilter(directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        requirement.setMultiple(Namespace.CARDINALITY_MULTIPLE.equals(
            directives.get(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)));
        requirement.setOptional(Namespace.RESOLUTION_OPTIONAL.equals(
            directives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)));

        PullParser.sanityCheckEndElement(reader, event, REQUIREMENT);
        return requirement;
    }
}
