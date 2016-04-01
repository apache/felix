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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
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
                    if (requirement != null) {
                    	resource.addRequire(requirement);
                    }
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
            if (resource.getURI() == null)
            {
                parseContentNamespace(reader, resource);
                return null;
            }
            // if the URI is already set, this is a second osgi.content capability.
            // The first content capability, which is the main one, is stored in the Resource.
            // Subsequent content capabilities are stored are ordinary capabilities.
        }

        CapabilityImpl capability = new CapabilityImpl();
        if (!namespace.equals(NamespaceTranslator.getOSGiNamespace(namespace)))
            throw new Exception("Namespace conflict. Namespace not allowed: " + namespace);

        capability.setName(NamespaceTranslator.getFelixNamespace(namespace));
        Map<String, Object> attributes = new HashMap<String, Object>();
        Map<String, String> directives = new HashMap<String, String>();
        parseAttributesDirectives(reader, attributes, directives, CAPABILITY);

        for (Map.Entry<String, Object> entry : attributes.entrySet())
        {
            if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) && BundleNamespace.BUNDLE_NAMESPACE.equals(entry.getKey()))
            {
                capability.addProperty(new FelixPropertyAdapter(Resource.SYMBOLIC_NAME, entry.getValue()));
                continue;
            }
            if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) && BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE.equals(entry.getKey()))
            {
               capability.addProperty(new FelixPropertyAdapter(Resource.VERSION, entry.getValue()));
               continue;
            }
            capability.addProperty(new FelixPropertyAdapter(NamespaceTranslator.getFelixNamespace(entry.getKey()), entry.getValue()));
        }
        for (Map.Entry<String, String> entry : directives.entrySet())
        {
            capability.addDirective(entry.getKey(), entry.getValue());
        }

        return capability;
    }

    private static void parseIdentityNamespace(XmlPullParser reader, ResourceImpl resource) throws Exception
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        parseAttributesDirectives(reader, attributes, new HashMap<String, String>(), CAPABILITY);
        // TODO need to cater for the singleton directive...

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
        parseAttributesDirectives(reader, attributes, new HashMap<String, String>(), CAPABILITY);

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

    private static void parseAttributesDirectives(XmlPullParser reader, Map<String, Object> attributes, Map<String, String> directives, String parentTag) throws XmlPullParserException, IOException
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
            else if (DIRECTIVE.equals(element))
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
        else if ("List<String>".equals(type))
            return parseStringList(value);
        else if ("List<Version>".equals(type))
            return parseVersionList(value);
        else if ("List<Long>".equals(type))
            return parseLongList(value);
        else if ("List<Double>".equals(type))
            return parseDoubleList(value);
        return value;
    }

    private static List<String> parseStringList(String value)
    {
        List<String> l = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        boolean escaped = false;
        for (char c : value.toCharArray())
        {
            if (escaped)
            {
                sb.append(c);
                escaped = false;
            }
            else
            {
                switch (c)
                {
                    case '\\':
                        escaped = true;
                        break;
                    case ',':
                        l.add(sb.toString().trim());
                        sb.setLength(0);
                        break;
                    default:
                        sb.append(c);
                }
            }
        }

        if (sb.length() > 0)
            l.add(sb.toString().trim());

        return l;
    }

    private static List<Version> parseVersionList(String value)
    {
        List<Version> l = new ArrayList<Version>();

        // Version strings cannot contain a comma, as it's not an allowed character in it anywhere
        for (String v : value.split(","))
        {
            l.add(Version.parseVersion(v.trim()));
        }
        return l;
    }

    private static List<Long> parseLongList(String value)
    {
        List<Long> l = new ArrayList<Long>();

        for (String x : value.split(","))
        {
            l.add(Long.parseLong(x.trim()));
        }
        return l;
    }

    private static List<Double> parseDoubleList(String value)
    {
        List<Double> l = new ArrayList<Double>();

        for (String d : value.split(","))
        {
            l.add(Double.parseDouble(d.trim()));
        }
        return l;
    }

    private static Requirement parseRequirement(XmlPullParser reader) throws Exception
    {
        RequirementImpl requirement = new RequirementImpl();
        String namespace = reader.getAttributeValue(null, NAMESPACE);
        if (!namespace.equals(NamespaceTranslator.getOSGiNamespace(namespace)))
            throw new Exception("Namespace conflict. Namespace not allowed: " + namespace);

        requirement.setName(NamespaceTranslator.getFelixNamespace(namespace));

        Map<String, Object> attributes = new HashMap<String, Object>();
        Map<String, String> directives = new HashMap<String, String>();
        parseAttributesDirectives(reader, attributes, directives, REQUIREMENT);
        requirement.setAttributes(attributes);

        String effective = directives.get("effective");
        if (effective != null && !effective.equals("resolve")) {
        	return null;
        }
        
        String filter = directives.remove(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        for (String ns : NamespaceTranslator.getTranslatedOSGiNamespaces())
        {
        	if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace) && BundleNamespace.BUNDLE_NAMESPACE.equals(ns)) 
        	{
        		filter = filter.replaceAll("[(][ ]*" + ns + "[ ]*=",
                    "(" + Resource.SYMBOLIC_NAME + "=");
        	}
        	else
            	filter = filter.replaceAll("[(][ ]*" + ns + "[ ]*=",
                        "(" + NamespaceTranslator.getFelixNamespace(ns) + "=");
        }
        requirement.setFilter(filter);
        requirement.setMultiple(Namespace.CARDINALITY_MULTIPLE.equals(
            directives.remove(Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE)));
        requirement.setOptional(Namespace.RESOLUTION_OPTIONAL.equals(
            directives.remove(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)));
        requirement.setDirectives(directives);

        requirement.setExtend(false);

        return requirement;
    }
}
