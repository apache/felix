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
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import org.apache.felix.status.PrinterMode;
import org.apache.felix.status.StatusPrinter;
import org.apache.felix.status.StatusPrinterHandler;
import org.apache.felix.status.StatusPrinterManager;
import org.apache.felix.status.ZipAttachmentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Helper class for a status printer.
 */
public class StatusPrinterAdapter implements StatusPrinterHandler, Comparable<StatusPrinterAdapter> {

    /**
     * Formatter pattern to render the current time of status generation.
     */
    static final DateFormat DISPLAY_DATE_FORMAT = DateFormat.getDateTimeInstance( DateFormat.LONG,
        DateFormat.LONG, Locale.US );

    /**
     * Create a new adapter if the provided service is either a printer or provides
     * the print method.
     * @return An adapter or <code>null</code> if the method is missing.
     */
    public static StatusPrinterAdapter createAdapter(final StatusPrinterDescription description,
            final Object service) {

        Method printMethod = null;
        if ( !(service instanceof StatusPrinter) ) {

            // print(String, PrintWriter)
            printMethod = ClassUtils.searchMethod(service.getClass(), "print",
                    new Class[] {String.class, PrintWriter.class});
            if ( printMethod == null ) {
                return null;
            }
        }
        Method attachmentMethod = null;
        if ( !(service instanceof ZipAttachmentProvider) ) {

            // addAttachments()
            attachmentMethod = ClassUtils.searchMethod(service.getClass(), "addAttachments",
                    new Class[] {String.class, ZipOutputStream.class});
        }
        return new StatusPrinterAdapter(
                description,
                service,
                printMethod,
                attachmentMethod);
    }

    /**
     * Comparator for adapters based on the service ranking.
     */
    public static final Comparator<StatusPrinterAdapter> RANKING_COMPARATOR = new Comparator<StatusPrinterAdapter>() {

        public int compare(final StatusPrinterAdapter o1, final StatusPrinterAdapter o2) {
            return o1.description.compareTo(o2.description);
        }
    };

    /** The status printer service. */
    private final Object printer;

    /** The printer description. */
    private final StatusPrinterDescription description;

    /** The method to use if printer does not implement the service interface. */
    private final Method printMethod;

    private final Method attachmentMethod;

    /** Service registration for the web console. */
    private ServiceRegistration registration;

    /**
     * Constructor.
     */
    public StatusPrinterAdapter( final StatusPrinterDescription description,
            final Object printer,
            final Method printMethod,
            final Method attachmentMethod) {
        this.description = description;
        this.printer = printer;
        this.printMethod = printMethod;
        this.attachmentMethod = attachmentMethod;
    }

    public void registerConsole(final BundleContext context, final StatusPrinterManager manager) {
        if ( this.registration == null &&
             (supports(PrinterMode.HTML_BODY) || supports(PrinterMode.TEXT))) {
            this.registration = WebConsolePlugin.register(context, manager, this.description);
        }
    }

    public void unregisterConsole() {
        if ( this.registration != null ) {
            this.registration.unregister();
            this.registration = null;
        }
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getTitle()
     */
    public String getTitle() {
        return this.description.getTitle();
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getName()
     */
    public String getName() {
        return this.description.getName();
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getCategory()
     */
    public String getCategory() {
        return this.description.getCategory();
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#getModes()
     */
    public PrinterMode[] getModes() {
        return this.description.getModes();
    }

    /**
     * @see org.apache.felix.status.ZipAttachmentProvider#addAttachments(java.lang.String, java.util.zip.ZipOutputStream)
     */
    public void addAttachments(final String namePrefix, final ZipOutputStream zos)
    throws IOException {
        // check if printer implements ZipAttachmentProvider
        if ( printer instanceof ZipAttachmentProvider ) {
            ((ZipAttachmentProvider)printer).addAttachments(namePrefix, zos);
        } else if ( this.attachmentMethod != null ) {
            ClassUtils.invoke(this.printer, this.attachmentMethod, new Object[] {namePrefix, zos});
        }
    }

    /**
     * @see org.apache.felix.status.StatusPrinterHandler#supports(org.apache.felix.status.PrinterMode)
     */
    public boolean supports(final PrinterMode mode) {
        for(int i=0; i<this.description.getModes().length; i++) {
            if ( this.description.getModes()[i] == mode ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.felix.status.StatusPrinter#print(org.apache.felix.status.PrinterMode, java.io.PrintWriter)
     */
    public void print(final PrinterMode mode,
            final PrintWriter printWriter) {
        if ( this.supports(mode) ) {
            if ( this.printer instanceof StatusPrinter ) {
                ((StatusPrinter)this.printer).print(mode, printWriter);
            } else {
                ClassUtils.invoke(this.printer, this.printMethod, new Object[] {mode.toString(), printWriter});
            }
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return printer.getClass() + "(" + super.toString() + ")";
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final StatusPrinterAdapter spa) {
        return this.description.getSortKey().compareTo(spa.description.getSortKey());
    }

    public StatusPrinterDescription getDescription() {
        return this.description;
    }
 }
