/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.status.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipOutputStream;

import org.apache.felix.status.PrinterMode;
import org.apache.felix.status.StatusPrinterHandler;
import org.apache.felix.status.StatusPrinterManager;

/**
 * The web console plugin for a status printer.
 */
public class DefaultWebConsolePlugin extends AbstractWebConsolePlugin implements StatusPrinterHandler {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * @param statusPrinterAdapter The adapter
     */
    DefaultWebConsolePlugin(final StatusPrinterManager statusPrinterManager) {
        super(statusPrinterManager);
    }

    @Override
    protected StatusPrinterHandler getStatusPrinterHandler() {
        return this;
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getTitle()
     */
    public String getTitle() {
        return "Overview";
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getName()
     */
    public String getName() {
        return "config";
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getCategory()
     */
    public String getCategory() {
        return "Status";
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getModes()
     */
    public PrinterMode[] getModes() {
        return new PrinterMode[] {PrinterMode.TEXT};
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#supports(org.apache.felix.status.PrinterMode)
     */
    public boolean supports(final PrinterMode mode) {
        return mode == PrinterMode.TEXT;
    }

    /**
     * @see org.apache.felix.status.StatusPrinter#print(org.apache.felix.status.PrinterMode, java.io.PrintWriter)
     */
    public void print(final PrinterMode mode, final PrintWriter printWriter) {
        final StatusPrinterHandler[] handlers = this.statusPrinterManager.getAllHandlers();
        printWriter.print("Currently registered ");
        printWriter.print(String.valueOf(handlers.length));
        printWriter.println(" status printer.");
        printWriter.println();
        for(final StatusPrinterHandler handler : handlers) {
            printWriter.println(handler.getTitle());
        }
    }

    /**
     * @see org.apache.felix.status.ZipAttachmentProvider#addAttachments(java.lang.String, java.util.zip.ZipOutputStream)
     */
    public void addAttachments(String namePrefix, ZipOutputStream zos)
    throws IOException {
        // no attachments support
    }
}