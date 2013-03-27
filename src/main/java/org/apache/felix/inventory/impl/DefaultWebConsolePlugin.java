/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.inventory.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipOutputStream;

import org.apache.felix.inventory.PrinterMode;
import org.apache.felix.inventory.impl.webconsole.ConsoleConstants;

/**
 * The web console plugin for a inventory printer.
 */
public class DefaultWebConsolePlugin extends AbstractWebConsolePlugin implements InventoryPrinterHandler
{

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param inventoryPrinterAdapter The adapter
     */
    DefaultWebConsolePlugin(final InventoryPrinterManagerImpl inventoryPrinterManager)
    {
        super(inventoryPrinterManager);
    }

    protected InventoryPrinterHandler getInventoryPrinterHandler()
    {
        return this;
    }

    /**
     * @see org.apache.felix.inventory.impl.InventoryPrinterHandler#getTitle()
     */
    public String getTitle()
    {
        return ConsoleConstants.TITLE;
    }

    /**
     * @see org.apache.felix.inventory.impl.InventoryPrinterHandler#getName()
     */
    public String getName()
    {
        return ConsoleConstants.NAME;
    }

    /**
     * @see org.apache.felix.inventory..implInventoryPrinterHandler#getModes()
     */
    public PrinterMode[] getModes()
    {
        return new PrinterMode[]
            { PrinterMode.TEXT };
    }

    /**
     * @see org.apache.felix.inventory.impl.InventoryPrinterHandler#supports(org.apache.felix.inventory.PrinterMode)
     */
    public boolean supports(final PrinterMode mode)
    {
        return mode == PrinterMode.TEXT;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(org.apache.felix.inventory.PrinterMode,
     *      java.io.PrintWriter, boolean)
     */
    public void print(final PrinterMode mode, final PrintWriter printWriter, final boolean isZip)
    {
        final InventoryPrinterHandler[] handlers = this.inventoryPrinterManager.getAllHandlers();
        printWriter.print("Currently registered ");
        printWriter.print(String.valueOf(handlers.length));
        printWriter.println(" printer(s).");
        printWriter.println();
        for (int i = 0; i < handlers.length; i++)
        {
            printWriter.println(handlers[i].getTitle());
        }
    }

    /**
     * @see org.apache.felix.inventory.ZipAttachmentProvider#addAttachments(java.lang.String,
     *      java.util.zip.ZipOutputStream)
     */
    public void addAttachments(String namePrefix, ZipOutputStream zos) throws IOException
    {
        // no attachments support
    }
}