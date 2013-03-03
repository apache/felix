/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2013 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package org.apache.felix.inventory;

import junit.framework.TestCase;

public class PrinterModeTest extends TestCase
{

    public void test_valueOf()
    {
        TestCase.assertSame(PrinterMode.TEXT, PrinterMode.valueOf("TEXT"));
        TestCase.assertSame(PrinterMode.JSON, PrinterMode.valueOf("JSON"));
        TestCase.assertSame(PrinterMode.HTML_FRAGMENT, PrinterMode.valueOf("HTML_FRAGMENT"));

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
