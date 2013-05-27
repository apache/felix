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

import org.apache.felix.inventory.Format;
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

    protected InventoryPrinterHandler getInventoryPrinterHandler(final String label)
    {
        return this.inventoryPrinterManager.getHandler(label);
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
     * @see org.apache.felix.inventory..implInventoryPrinterHandler#getformats()
     */
    public Format[] getFormats()
    {
        return new Format[]
            { Format.TEXT };
    }

    /**
     * @see org.apache.felix.inventory.impl.InventoryPrinterHandler#supports(org.apache.felix.inventory.Format)
     */
    public boolean supports(final Format format)
    {
        return format == Format.TEXT;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(java.io.PrintWriter,
     *      org.apache.felix.inventory.Format, boolean)
     */
    public void print(final PrintWriter printWriter, final Format format, final boolean isZip)
    {
        final InventoryPrinterHandler[] handlers = this.inventoryPrinterManager.getHandlers(null);
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
     * @see org.apache.felix.inventory.ZipAttachmentProvider#addAttachments(java.util.zip.ZipOutputStream,
     *      java.lang.String)
     */
    public void addAttachments(ZipOutputStream zos, String namePrefix) throws IOException
    {
        // no attachments support
    }
}