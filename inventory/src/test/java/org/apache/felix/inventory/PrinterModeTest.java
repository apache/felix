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
package org.apache.felix.inventory;

import junit.framework.TestCase;

public class PrinterModeTest extends TestCase
{

    public void test_valueOf()
    {
        TestCase.assertSame(PrinterMode.TEXT, PrinterMode.valueOf("TEXT"));
        TestCase.assertSame(PrinterMode.JSON, PrinterMode.valueOf("JSON"));
        TestCase.assertSame(PrinterMode.HTML_FRAGMENT, PrinterMode.valueOf("HTML_FRAGMENT"));

        TestCase.assertSame(PrinterMode.TEXT, PrinterMode.valueOf("text"));
        TestCase.assertSame(PrinterMode.JSON, PrinterMode.valueOf("json"));
        TestCase.assertSame(PrinterMode.HTML_FRAGMENT, PrinterMode.valueOf("html_fragment"));

        TestCase.assertSame(PrinterMode.TEXT, PrinterMode.valueOf("Text"));
        TestCase.assertSame(PrinterMode.JSON, PrinterMode.valueOf("Json"));
        TestCase.assertSame(PrinterMode.HTML_FRAGMENT, PrinterMode.valueOf("HTML_Fragment"));

        TestCase.assertNull(PrinterMode.valueOf("unsupported_name"));
    }

    public void test_name()
    {
        TestCase.assertEquals("TEXT", PrinterMode.TEXT.name());
        TestCase.assertEquals("JSON", PrinterMode.JSON.name());
        TestCase.assertEquals("HTML_FRAGMENT", PrinterMode.HTML_FRAGMENT.name());
    }

    public void test_equals()
    {
        TestCase.assertTrue(PrinterMode.TEXT.equals(PrinterMode.TEXT));
        TestCase.assertFalse(PrinterMode.TEXT.equals(PrinterMode.JSON));
        TestCase.assertFalse(PrinterMode.TEXT.equals(PrinterMode.HTML_FRAGMENT));

        TestCase.assertFalse(PrinterMode.JSON.equals(PrinterMode.TEXT));
        TestCase.assertTrue(PrinterMode.JSON.equals(PrinterMode.JSON));
        TestCase.assertFalse(PrinterMode.JSON.equals(PrinterMode.HTML_FRAGMENT));

        TestCase.assertFalse(PrinterMode.HTML_FRAGMENT.equals(PrinterMode.TEXT));
        TestCase.assertFalse(PrinterMode.HTML_FRAGMENT.equals(PrinterMode.JSON));
        TestCase.assertTrue(PrinterMode.HTML_FRAGMENT.equals(PrinterMode.HTML_FRAGMENT));
    }

}
