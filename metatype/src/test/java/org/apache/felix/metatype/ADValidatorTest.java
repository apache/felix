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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Test cases for {@link ADValidator}.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ADValidatorTest extends TestCase
{
    /**
     * Tests the validation of boolean is only done minimally.
     */
    public void testValidateBoolean()
    {
        AD ad = new AD();
        ad.setType("Boolean");
        ad.setRequired(false);

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        // adhere minimal value
        assertEquals("", ADValidator.validate(ad, "true"));
        // adhere maximal value
        assertEquals("", ADValidator.validate(ad, "false"));
        // not a valid value
        assertEquals(AD.VALIDATE_INVALID_VALUE, ADValidator.validate(ad, "foobar"));

        ad.setCardinality(3); // up to three values are allowed...

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        // 2nd value is missing, but that's ok...
        assertEquals("", ADValidator.validate(ad, "true,,false"));

        ad.setRequired(true);

        // mandatory value
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, null));
        // 2nd value is missing
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, "false,,true"));
        // correct values
        assertEquals("", ADValidator.validate(ad, "false, true, false"));
        // correct values
        assertEquals(AD.VALIDATE_INVALID_VALUE, ADValidator.validate(ad, "false, yessir, false"));

        ad.setOptions(Collections.singletonMap("true", "Yes!"));

        assertEquals(null, ADValidator.validate(ad, "false, true, false"));
    }

    /**
     * Tests the validation of bytes works and uses the correct minimum and maximum values.
     */
    public void testValidateByte()
    {
        AD ad = new AD();
        ad.setType("Byte");
        ad.setRequired(false);

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        assertEquals("", ADValidator.validate(ad, ""));

        ad.setRequired(true);

        // mandatory value
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, ""));
        // value too small
        assertEquals(AD.VALIDATE_INVALID_VALUE, ADValidator.validate(ad, "-129"));
        // value too small
        assertEquals("", ADValidator.validate(ad, "-128"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "0"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "127"));
        // value is invalid
        assertEquals(AD.VALIDATE_INVALID_VALUE, ADValidator.validate(ad, "128"));

        ad.setMin("0");
        ad.setMax("128"); // !!! this is an invalid value for maximum, this means that we cannot do this validation...

        // value too small
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "-1"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "0"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "127"));
        // value is invalid
        assertEquals(AD.VALIDATE_INVALID_VALUE, ADValidator.validate(ad, "128"));
    }

    /**
     * Tests the validation of characters with only limited set of options.
     */
    public void testValidateCharacter()
    {
        AD ad = new AD();
        ad.setType("Char");
        ad.setRequired(false);

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        // option too long
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "ab"));
        // adhere first option value
        assertEquals("", ADValidator.validate(ad, "b"));
        // adhere last option value
        assertEquals("", ADValidator.validate(ad, "e"));

        ad.setCardinality(3); // up to three values are allowed...

        // mandatory value
        assertEquals("", ADValidator.validate(ad, ""));
        // 2nd value is missing
        assertEquals("", ADValidator.validate(ad, "b,,c"));

        ad.setRequired(true);

        // mandatory value
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, null));
        // 2nd value is missing
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, "b,,c"));
        // adhere minimal values
        assertEquals("", ADValidator.validate(ad, "b, c, d"));
        // adhere maximal values
        assertEquals("", ADValidator.validate(ad, "c, d, e"));

        Map options = new HashMap();
        options.put("b", "B");
        options.put("c", "C");
        options.put("d", "D");
        options.put("e", "E");

        ad.setOptions(options);
        // no option given
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, ""));
        // invalid option
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "a"));
        // too great
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "f"));
        // 2nd value is too less
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "b,a,c"));
        // 3rd value is too great
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "d, e, f"));

        ad.setMin("b");
        ad.setMax("c");
        ad.setOptions(Collections.emptyMap());

        // adhere minimal values
        assertEquals("", ADValidator.validate(ad, "b, c, b"));
        // d is too great
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "b, c, d"));
        // a is too small
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "a, b, c"));
    }

    /**
     * Tests the validation of double value works as expected.
     */
    public void testValidateDouble()
    {
        AD ad = new AD();
        ad.setType("Double");
        ad.setRequired(false);

        ad.setMin("-123.45");
        ad.setMax("+123.45");

        // Value is too small...
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "-123.4501"));
        // Value is within range...
        assertEquals("", ADValidator.validate(ad, "-123.45000"));
        // Value is within range...
        assertEquals("", ADValidator.validate(ad, "123.45000"));
        // Value is too great...
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "123.4501"));

        Map options = new HashMap();
        options.put("1.1", "B");
        options.put("2.2", "C");
        options.put("3.3", "D");
        options.put("4.4", "E");

        ad.setOptions(options);

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        // invalid option
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "1.0"));
        // adhere first option value
        assertEquals("", ADValidator.validate(ad, "1.1"));
        // adhere last option value
        assertEquals("", ADValidator.validate(ad, "4.4"));
        // too great
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "4.5"));

        ad.setCardinality(3); // up to three values are allowed...
        ad.setRequired(true);

        // mandatory value
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, null));
        // 2nd value is too less
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "1.1,1.0,2.2"));
        // adhere minimal values
        assertEquals("", ADValidator.validate(ad, "1.1, 2.2, 3.3"));
        // adhere maximal values
        assertEquals("", ADValidator.validate(ad, "2.2, 3.3, 4.4"));
        // 3rd value is too great
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "3.3, 4.4, 5.5"));
    }

    /**
     * Tests the validation of integers is based on the minimum and maximum values.
     */
    public void testValidateInteger()
    {
        AD ad = new AD();
        ad.setType("Integer");
        ad.setMin("3"); // only values greater than 2
        ad.setMax("6"); // only values less than 7
        ad.setRequired(false);

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        assertEquals("", ADValidator.validate(ad, ""));
        // too less
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "2"));
        // adhere minimal value
        assertEquals("", ADValidator.validate(ad, "3"));
        // adhere maximal value
        assertEquals("", ADValidator.validate(ad, "6"));
        // too great
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "7"));

        ad.setCardinality(3); // up to three values are allowed...

        // mandatory value
        assertEquals("", ADValidator.validate(ad, null));
        // 2nd value is missing
        assertEquals("", ADValidator.validate(ad, "3,,3"));

        ad.setRequired(true);

        // mandatory value
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, null));
        // 2nd value is missing
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, "3,,3"));
        // 2nd value is invalid
        assertEquals(AD.VALIDATE_INVALID_VALUE, ADValidator.validate(ad, "3,a,3"));
        // 2nd value is too less
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "3,2,3"));
        // adhere minimal values
        assertEquals("", ADValidator.validate(ad, "3, 4, 5"));
        // adhere maximal values
        assertEquals("", ADValidator.validate(ad, "6, 5, 4"));
        // 3rd value is too great
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "5, 6, 7"));
    }

    /**
     * Tests the validation of long values works as expected.
     */
    public void testValidateLong()
    {
        AD ad = new AD();
        ad.setType("Long");
        ad.setRequired(true);

        ad.setMin("-20");
        ad.setMax("-15");

        // value too small
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "-21"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "-15"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "-20"));
        // value too great
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "-14"));

        // Set minimum and maximum beyond the range of plain integers...
        ad.setMin("2147483650");
        ad.setMax("2147483655");

        // value too small
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "2147483649"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "2147483650"));
        // value within bounds
        assertEquals("", ADValidator.validate(ad, "2147483655"));
        // value too great
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "2147483656"));
    }

    /**
     * Tests the validation of strings is based on the minimum and maximum lengths.
     */
    public void testValidateString()
    {
        AD ad = new AD();
        ad.setType("String");
        ad.setRequired(false);

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        // any length of input is accepted
        assertEquals("", ADValidator.validate(ad, "1234567890"));

        ad.setMin("3"); // minimal length == 3
        ad.setMax("6"); // maximum length == 6

        // too short
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "12"));
        // adhere minimum length
        assertEquals("", ADValidator.validate(ad, "123"));
        // adhere maximum length
        assertEquals("", ADValidator.validate(ad, "12356"));
        // too long
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "1234567"));

        ad.setCardinality(3); // up to three values are allowed...
        ad.setMin("");

        // optional value
        assertEquals("", ADValidator.validate(ad, null));
        // 2nd value is missing; but that's ok
        assertEquals("", ADValidator.validate(ad, "321, , 123"));

        ad.setRequired(true);
        ad.setMin("3");

        // mandatory value
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, null));
        // 2nd value is missing
        assertEquals(AD.VALIDATE_MISSING, ADValidator.validate(ad, "321, , 123"));
        // 2nd value is too short
        assertEquals(AD.VALIDATE_LESS_THAN_MINIMUM, ADValidator.validate(ad, "321,12,123"));
        // adhere minimum lengths
        assertEquals("", ADValidator.validate(ad, "123, 123, 123"));
        // adhere maximum lengths
        assertEquals("", ADValidator.validate(ad, "12356, 654321, 123456"));
        // 3rd value is too long
        assertEquals(AD.VALIDATE_GREATER_THAN_MAXIMUM, ADValidator.validate(ad, "123, 123, 1234567"));

        ad.setOptions(Collections.singletonMap("123", "foo"));

        // adhere minimum lengths
        assertEquals("", ADValidator.validate(ad, "123, 123, 123"));
        assertEquals(AD.VALIDATE_NOT_A_VALID_OPTION, ADValidator.validate(ad, "2134"));
    }
}
