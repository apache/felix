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
package org.apache.felix.deployment.rp.autoconf;

import static org.osgi.service.deploymentadmin.spi.ResourceProcessorException.CODE_OTHER_ERROR;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.felix.metatype.Attribute;
import org.apache.felix.metatype.Designate;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Convenience methods to work with MetaType structures.
 */
public class MetaTypeUtil
{
    private MetaTypeUtil()
    {
        // Nop
    }

    /**
     * Determines the actual configuration data based on the specified designate and object class definition
     * 
     * @param designate The designate object containing the values for the properties
     * @param ocd The object class definition
     * @return A dictionary containing data as described in the designate and ocd objects, or <code>null</code> if the designate does not match it's
     * definition and the designate was marked as optional.
     * @throws ResourceProcessorException If the designate does not match the ocd and the designate is not marked as optional.
     */
    public static Dictionary getProperties(Designate designate, ObjectClassDefinition ocd) throws ResourceProcessorException
    {
        Dictionary properties = new Hashtable();
        AttributeDefinition[] attributeDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);

        List<Attribute> attributes = designate.getObject().getAttributes();
        for (Attribute attribute : attributes)
        {
            String adRef = attribute.getAdRef();
            boolean found = false;
            for (int j = 0; j < attributeDefs.length; j++)
            {
                AttributeDefinition ad = attributeDefs[j];
                if (adRef.equals(ad.getID()))
                {
                    // found attribute definition
                    Object value = getValue(attribute, ad);
                    if (value == null)
                    {
                        if (designate.isOptional())
                        {
                            properties = null;
                            break;
                        }
                        else
                        {
                            throw new ResourceProcessorException(CODE_OTHER_ERROR, "Could not match attribute to it's definition: adref=" + adRef);
                        }
                    }
                    properties.put(adRef, value);
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                if (designate.isOptional())
                {
                    properties = null;
                    break;
                }
                else
                {
                    throw new ResourceProcessorException(CODE_OTHER_ERROR, "Could not find attribute definition: adref=" + adRef);
                }
            }
        }

        return properties;
    }

    /**
     * Determines the value of an attribute based on an attribute definition
     * 
     * @param attribute The attribute containing value(s)
     * @param ad The attribute definition
     * @return An <code>Object</code> reflecting what was specified in the attribute and it's definition or <code>null</code> if the value did not match it's definition.
     * @throws ResourceProcessorException in case we're unable to parse the value of an attribute.
     */
    private static Object getValue(Attribute attribute, AttributeDefinition ad) throws ResourceProcessorException
    {
        if (attribute == null || ad == null || !attribute.getAdRef().equals(ad.getID()))
        {
            // wrong attribute or definition
            return null;
        }
        String[] content = attribute.getContent();

        // verify correct type of the value(s)
        int type = ad.getType();
        Object[] typedContent = null;
        try
        {
            for (int i = 0; i < content.length; i++)
            {
                String value = content[i];
                switch (type)
                {
                    case AttributeDefinition.BOOLEAN:
                        typedContent = (typedContent == null) ? new Boolean[content.length] : typedContent;
                        typedContent[i] = Boolean.valueOf(value);
                        break;
                    case AttributeDefinition.BYTE:
                        typedContent = (typedContent == null) ? new Byte[content.length] : typedContent;
                        typedContent[i] = Byte.valueOf(value);
                        break;
                    case AttributeDefinition.CHARACTER:
                        typedContent = (typedContent == null) ? new Character[content.length] : typedContent;
                        char[] charArray = value.toCharArray();
                        if (charArray.length == 1)
                        {
                            typedContent[i] = new Character(charArray[0]);
                        }
                        else
                        {
                            throw new ResourceProcessorException(CODE_OTHER_ERROR, "Unable to parse value for definition: adref=" + ad.getID());
                        }
                        break;
                    case AttributeDefinition.DOUBLE:
                        typedContent = (typedContent == null) ? new Double[content.length] : typedContent;
                        typedContent[i] = Double.valueOf(value);
                        break;
                    case AttributeDefinition.FLOAT:
                        typedContent = (typedContent == null) ? new Float[content.length] : typedContent;
                        typedContent[i] = Float.valueOf(value);
                        break;
                    case AttributeDefinition.INTEGER:
                        typedContent = (typedContent == null) ? new Integer[content.length] : typedContent;
                        typedContent[i] = Integer.valueOf(value);
                        break;
                    case AttributeDefinition.LONG:
                        typedContent = (typedContent == null) ? new Long[content.length] : typedContent;
                        typedContent[i] = Long.valueOf(value);
                        break;
                    case AttributeDefinition.SHORT:
                        typedContent = (typedContent == null) ? new Short[content.length] : typedContent;
                        typedContent[i] = Short.valueOf(value);
                        break;
                    case AttributeDefinition.STRING:
                        typedContent = (typedContent == null) ? new String[content.length] : typedContent;
                        typedContent[i] = value;
                        break;
                    default:
                        // unsupported type
                        throw new ResourceProcessorException(CODE_OTHER_ERROR, "Unsupported value-type for definition: adref=" + ad.getID());
                }
            }
        }
        catch (NumberFormatException nfe)
        {
            throw new ResourceProcessorException(CODE_OTHER_ERROR, "Unable to parse value for definition: adref=" + ad.getID());
        }

        // verify cardinality of value(s)
        int cardinality = ad.getCardinality();
        Object result = null;
        if (cardinality == 0)
        {
            if (typedContent.length == 1)
            {
                result = typedContent[0];
            }
            else
            {
                result = null;
            }
        }
        else if (cardinality == Integer.MIN_VALUE)
        {
            result = new Vector(Arrays.asList(typedContent));
        }
        else if (cardinality == Integer.MAX_VALUE)
        {
            result = typedContent;
        }
        else if (cardinality < 0)
        {
            if (typedContent.length <= Math.abs(cardinality))
            {
                result = new Vector(Arrays.asList(typedContent));
            }
            else
            {
                result = null;
            }
        }
        else if (cardinality > 0)
        {
            if (typedContent.length <= cardinality)
            {
                result = typedContent;
            }
            else
            {
                result = null;
            }
        }
        return result;
    }
}
