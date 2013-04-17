/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.inventory;

import junit.framework.TestCase;

public class FormatTest extends TestCase
{

    public void test_valueOf()
    {
        TestCase.assertSame(Format.TEXT, Format.valueOf("TEXT"));
        TestCase.assertSame(Format.JSON, Format.valueOf("JSON"));
        TestCase.assertSame(Format.HTML, Format.valueOf("HTML"));

        TestCase.assertSame(Format.TEXT, Format.valueOf("text"));
        TestCase.assertSame(Format.JSON, Format.valueOf("json"));
        TestCase.assertSame(Format.HTML, Format.valueOf("html"));

        TestCase.assertSame(Format.TEXT, Format.valueOf("Text"));
        TestCase.assertSame(Format.JSON, Format.valueOf("Json"));
        TestCase.assertSame(Format.HTML, Format.valueOf("HtMl"));

        TestCase.assertNull(Format.valueOf("unsupported_name"));
    }

    public void test_toString()
    {
        TestCase.assertEquals("TEXT", Format.TEXT.toString());
        TestCase.assertEquals("JSON", Format.JSON.toString());
        TestCase.assertEquals("HTML", Format.HTML.toString());
    }

    public void test_equals()
    {
        TestCase.assertTrue(Format.TEXT.equals(Format.TEXT));
        TestCase.assertFalse(Format.TEXT.equals(Format.JSON));
        TestCase.assertFalse(Format.TEXT.equals(Format.HTML));

        TestCase.assertFalse(Format.JSON.equals(Format.TEXT));
        TestCase.assertTrue(Format.JSON.equals(Format.JSON));
        TestCase.assertFalse(Format.JSON.equals(Format.HTML));

        TestCase.assertFalse(Format.HTML.equals(Format.TEXT));
        TestCase.assertFalse(Format.HTML.equals(Format.JSON));
        TestCase.assertTrue(Format.HTML.equals(Format.HTML));
    }

}
