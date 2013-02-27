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
package org.apache.felix.inventory.impl;

import java.util.Arrays;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.PrinterMode;
import org.osgi.framework.ServiceReference;

/**
 * Helper class for a configuration printer.
 */
public class InventoryPrinterDescription implements Comparable {

    private final ServiceReference reference;

    private final PrinterMode[] modes;

    private final String name;

    private final String title;

    private final String sortKey;

    public InventoryPrinterDescription(final ServiceReference ref) {
        this.reference = ref;

        // check modes
        final Object modesCfg = ref.getProperty(InventoryPrinter.CONFIG_PRINTER_MODES);
        if ( modesCfg instanceof String ) {
            final PrinterMode mode = PrinterMode.valueOf((String)modesCfg);
            if ( mode != null ) {
                this.modes = new PrinterMode[] {mode};
            } else {
                this.modes = null;
            }
        } else if ( modesCfg instanceof String[] ) {
            final String[] modesCfgArray = (String[])modesCfg;
            final PrinterMode[] pModes = new PrinterMode[modesCfgArray.length];
            boolean invalid = false;
            for(int i=0; i<modesCfgArray.length;i++) {
                pModes[i] = PrinterMode.valueOf(modesCfgArray[i]);
                if ( pModes[i] == null ) {
                    invalid = true;
                }
            }
            if ( invalid ) {
                this.modes = null;
            } else {
                this.modes = pModes;
            }
        } else {
            this.modes = null;
        }

        // check name
        if ( ref.getProperty(InventoryPrinter.CONFIG_NAME) != null ) {
            this.name = ref.getProperty(InventoryPrinter.CONFIG_NAME).toString();
        } else {
            this.name = null;
        }

        // check title
        if ( ref.getProperty(InventoryPrinter.CONFIG_TITLE) != null ) {
            this.title = ref.getProperty(InventoryPrinter.CONFIG_TITLE).toString();
            if ( this.title.startsWith("%") ) {
                this.sortKey = this.title.substring(1);
            } else {
                this.sortKey = this.title;
            }
        } else {
            this.title = null;
            this.sortKey = null;
        }
    }

    public String getTitle() {
        return this.title;
    }

    public String getSortKey() {
        return this.sortKey;
    }

    public String getName() {
        return this.name;
    }

    public PrinterMode[] getModes() {
        return this.modes;
    }

    public ServiceReference getServiceReference() {
        return this.reference;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object spa) {
        return this.reference.compareTo(((InventoryPrinterDescription)spa).reference);
    }

    public boolean equals(final Object obj) {
        return this.reference.equals(obj);
    }

    public int hashCode() {
        return this.reference.hashCode();
    }

    public String toString() {
        return "InventoryPrinterDescription [title=" + title + ", name=" + name
                + ", modes=" + Arrays.toString(modes) + ", sortKey=" + sortKey + "]";
    }
}
