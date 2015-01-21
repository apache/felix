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
package org.apache.felix.metatype;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.osgi.service.metatype.AttributeDefinition;

/**
 * The <code>ADTest</code> class tests the static helper methods of the
 * {@link AD} class.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ADTest extends TestCase
{

    private static final String BLANK = "     \r\n   \t";

    public void testNull()
    {
        String listString = null;
        String[] list = AD.splitList(listString);
        assertNull(list);
    }

    public void testEmpty()
    {
        String listString = "";
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(1, list.length);
        assertEquals(listString, list[0]);
    }

    public void testSingle()
    {
        String value0 = "value";
        String listString = value0;
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(1, list.length);
        assertEquals(value0, list[0]);
    }

    public void testTwo()
    {
        String value0 = "value0";
        String value1 = "value1";
        String listString = value0 + "," + value1;
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(2, list.length);
        assertEquals(value0, list[0]);
        assertEquals(value1, list[1]);
    }

    public void testEmptySecond()
    {
        String value0 = "value0";
        String value1 = "";
        String listString = value0 + ",";
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(2, list.length);
        assertEquals(value0, list[0]);
        assertEquals(value1, list[1]);
    }

    public void testSpacedSecond()
    {
        String value0 = "value0";
        String value1 = "";
        String listString = value0 + ", ";
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(2, list.length);
        assertEquals(value0, list[0]);
        assertEquals(value1, list[1]);
    }

    public void testSingleBlanks()
    {
        String value0 = "value";
        String listString = BLANK + value0 + BLANK;
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(1, list.length);
        assertEquals(value0, list[0]);
    }

    public void testTwoBlanks()
    {
        String value0 = "value0";
        String value1 = "value1";
        String listString = BLANK + value0 + BLANK + "," + BLANK + value1 + BLANK;
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(2, list.length);
        assertEquals(value0, list[0]);
        assertEquals(value1, list[1]);
    }

    public void testSpaces()
    {
        String value = "Hello World";
        String listString = BLANK + value + BLANK + "," + BLANK + value + BLANK + "," + value;
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(3, list.length);
        assertEquals(value, list[0]);
        assertEquals(value, list[1]);
        assertEquals(value, list[2]);
    }

    public void testStandardSample()
    {
        String value0 = "a,b";
        String value1 = "b,c";
        String value2 = " c\\";
        String value3 = "d";
        String listString = "a\\,b,b\\,c,\\ c\\\\,d";
        String[] list = AD.splitList(listString);
        assertNotNull(list);
        assertEquals(4, list.length);
        assertEquals(value0, list[0]);
        assertEquals(value1, list[1]);
        assertEquals(value2, list[2]);
        assertEquals(value3, list[3]);
    }

    public void testToTypeString()
    {
        assertEquals(AttributeDefinition.STRING, AD.toType("String"));
        assertEquals(AttributeDefinition.LONG, AD.toType("Long"));
        assertEquals(AttributeDefinition.DOUBLE, AD.toType("Double"));
        assertEquals(AttributeDefinition.FLOAT, AD.toType("Float"));
        assertEquals(AttributeDefinition.INTEGER, AD.toType("Integer"));
        assertEquals(AttributeDefinition.BYTE, AD.toType("Byte"));
        assertEquals(AttributeDefinition.CHARACTER, AD.toType("Char"));
        assertEquals(AttributeDefinition.CHARACTER, AD.toType("Character"));
        assertEquals(AttributeDefinition.BOOLEAN, AD.toType("Boolean"));
        assertEquals(AttributeDefinition.SHORT, AD.toType("Short"));
        assertEquals(AttributeDefinition.PASSWORD, AD.toType("Password"));
        assertEquals(AttributeDefinition.STRING, AD.toType("JohnDoe"));
    }

    /**
     * FELIX-3757: if an AD has only its 'required' property set, but no
     * min/max or option values defined, the validation still should detect
     * empty values.
     */
    public void testValidateRequiredValueWithMinimalOptions()
    {
        AD ad = new AD();
        ad.setType("Integer");
        ad.setRequired(true);

        assertEquals(AD.VALIDATE_MISSING, ad.validate(null));
    }

    /**
     * FELIX-3756: if an AD is optional, but its validate method is called
     * with invalid data, the value is regarded missing.
     */
    public void testValidateOptionalValueWithInvalidData()
    {
        AD ad = new AD();
        ad.setType("Integer");
        ad.setRequired(false);

        assertEquals(AD.VALIDATE_INVALID_VALUE, ad.validate("abc"));
    }

    /**
     * FELIX-3758: if an AD has a cardinality != 0, the validation method
     * cannot handle a comma-separated input.
     */
    public void testValidateValueWithMultiValueCardinality()
    {
        AD ad = new AD();
        ad.setType("Integer");
        ad.setCardinality(2);
        ad.setRequired(true);

        assertEquals("", ad.validate("1,2"));
    }

    /**
     * FELIX-3884 : if an AD has options, default values must be in the option values.
     */
    public void testOptionsAndDefaultValues()
    {
        AD ad = new AD();
        ad.setCardinality(2);
        ad.setType("String");
        ad.setRequired(false);

        Map options = new HashMap();
        options.put("A", "L-A");
        options.put("B", "L-B");
        ad.setOptions(options);

        ad.setDefaultValue("A,B");
        assertArrayEquals(new String[] { "A", "B" }, ad.getDefaultValue());

        ad.setDefaultValue("A,B,C");
        assertArrayEquals(new String[] { "A", "B" }, ad.getDefaultValue());

        ad.setDefaultValue("X,Y,B");
        assertArrayEquals(new String[] { "B" }, ad.getDefaultValue());

        ad.setDefaultValue("X,Y,Z");
        assertArrayEquals(new String[0], ad.getDefaultValue());

        ad.setDefaultValue(null);
        assertNull(ad.getDefaultValue());
    }

    /**
     * FELIX-3884/FELIX-4665 - Default values.
     */
    public void testDefaultValuesForSingleValuedAttributes()
    {
        AD ad = new AD();
        ad.setCardinality(0);
        ad.setType("String");
        ad.setRequired(false);

        ad.setDefaultValue(null);
        assertNull(ad.getDefaultValue());

        ad.setDefaultValue("A,B");
        assertArrayEquals(new String[] { "A" }, ad.getDefaultValue());

        ad.setDefaultValue("");
        assertArrayEquals(new String[] { "" }, ad.getDefaultValue());

        // corner case: in case of required values, an empty default makes no sense
        // for single values, hence that the empty default is coerced into null...
        ad.setRequired(true);
        ad.setDefaultValue("");
        assertNull(ad.getDefaultValue());
    }

    /**
     * FELIX-3884/FELIX-4665 - Default values.
     */
    public void testDefaultValuesForMultiValuedAttributes()
    {
        AD ad = new AD();
        ad.setCardinality(-2); // sign doesn't matter in this case
        ad.setType("String");
        ad.setRequired(false);

        ad.setDefaultValue(null);
        assertNull(ad.getDefaultValue());

        ad.setDefaultValue("A,B");
        assertArrayEquals(new String[] { "A", "B" }, ad.getDefaultValue());

        ad.setDefaultValue(",,");
        assertArrayEquals(new String[] { "", "" }, ad.getDefaultValue());

        ad.setDefaultValue("");
        assertArrayEquals(new String[] { "" }, ad.getDefaultValue());

        // corner case: in case of required values, an empty default is coerced
        // into a empty array...
        ad.setRequired(true);
        ad.setDefaultValue("");
        assertArrayEquals(new String[0], ad.getDefaultValue());
    }

    private static void assertArrayEquals(String[] a, String[] b)
    {
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++)
        {
            assertEquals(a[i], b[i]);
        }
    }
}
