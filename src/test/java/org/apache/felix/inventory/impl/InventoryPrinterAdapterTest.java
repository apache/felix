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
package org.apache.felix.inventory.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.PrinterMode;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class InventoryPrinterAdapterTest extends TestCase
{

    public void test_validInventoryPrinter()
    {
        InventoryPrinterDescription ipd = new InventoryPrinterDescription(new TestServiceReference());
        InventoryPrinterAdapter ipa = InventoryPrinterAdapter.createAdapter(ipd, new ValidInventoryPrinter());
        TestCase.assertNotNull(ipa);
        StringWriter w = new StringWriter();
        ipa.print(PrinterMode.TEXT, new PrintWriter(w), false);
        TestCase.assertEquals(w.toString(), PrinterMode.TEXT.name());
    }

    public void test_invalidInventoryPrinter()
    {
        InventoryPrinterDescription ipd = new InventoryPrinterDescription(new TestServiceReference());
        InventoryPrinterAdapter ipa = InventoryPrinterAdapter.createAdapter(ipd, new InvalidInventoryPrinter());
        TestCase.assertNull(ipa);
    }

    public void test_validZipAttachmentProvider() throws IOException
    {
        InventoryPrinterDescription ipd = new InventoryPrinterDescription(new TestServiceReference());
        InventoryPrinterAdapter ipa = InventoryPrinterAdapter.createAdapter(ipd, new ValidZipAttachmentProvider());
        TestCase.assertNotNull(ipa);

        try
        {
            Field f = ipa.getClass().getDeclaredField("attachmentMethod");
            f.setAccessible(true);
            TestCase.assertNotNull(f.get(ipa));
        }
        catch (Exception e)
        {
            TestCase.fail(e.toString());
        }
    }

    public void test_invalidZipAttachmentProvider() throws IOException
    {
        InventoryPrinterDescription ipd = new InventoryPrinterDescription(new TestServiceReference());

        InventoryPrinterAdapter ipa = InventoryPrinterAdapter.createAdapter(ipd,
            new InvalidZipAttachmentProvider_wrong_signature());
        TestCase.assertNotNull(ipa);

        try
        {
            Field f = ipa.getClass().getDeclaredField("attachmentMethod");
            f.setAccessible(true);
            TestCase.assertNull(f.get(ipa));
        }
        catch (Exception e)
        {
            TestCase.fail(e.toString());
        }

        InventoryPrinterAdapter ipa2 = InventoryPrinterAdapter.createAdapter(ipd,
            new InvalidZipAttachmentProvider_no_print_method());
        TestCase.assertNull(ipa2);
    }

    private static class ValidInventoryPrinter
    {
        private void print(String mode, PrintWriter w, boolean isZip)
        {
            w.print(mode);
            w.flush();
        }
    }

    private static class InvalidInventoryPrinter
    {
        private void print(String mode, PrintWriter w, Boolean isZip)
        {
            throw new IllegalStateException("Method not expected to be called");
        }
    }

    private static class ValidZipAttachmentProvider
    {
        private void print(String mode, PrintWriter w, boolean isZip)
        {
            w.print(mode);
            w.flush();
        }

        private void addAttachments(String prefix, ZipOutputStream out)
        {
        }
    }

    private static class InvalidZipAttachmentProvider_wrong_signature
    {
        private void print(String mode, PrintWriter w, boolean isZip)
        {
            w.print(mode);
            w.flush();
        }

        private void addAttachments(String prefix, JarOutputStream out)
        {
            throw new IllegalStateException("Method not expected to be called");
        }
    }

    private static class InvalidZipAttachmentProvider_no_print_method
    {
        private void addAttachments(String prefix, ZipOutputStream out)
        {
            throw new IllegalStateException("Method not expected to be called");
        }
    }

    private static class TestServiceReference implements ServiceReference
    {

        private Map props;

        {
            props = new HashMap();
            props.put(InventoryPrinter.CONFIG_PRINTER_MODES, new String[]
                { PrinterMode.TEXT.name() });
        }

        public Object getProperty(String key)
        {
            return this.props.get(key);
        }

        public String[] getPropertyKeys()
        {
            return (String[]) this.props.keySet().toArray(new String[this.props.size()]);
        }

        public Bundle getBundle()
        {
            return null;
        }

        public Bundle[] getUsingBundles()
        {
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className)
        {
            return false;
        }

        public int compareTo(Object reference)
        {
            return 0;
        }
    }
}
