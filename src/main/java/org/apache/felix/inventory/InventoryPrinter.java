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

import java.io.PrintWriter;

/**
 * The <code>InventoryPrinter</code> is a service interface to be
 * implemented by providers which want to hook into the display of the
 * current configuration and state of the OSGi framework and application.
 *
 * A inventory printer must configure at least these three configuration properties
 * <ul>
 *   <li>{@link #CONFIG_PRINTER_MODES} - the supported modes</li>
 *   <li>{@link #CONFIG_TITLE} - the printer title</li>
 *   <li>{@link #CONFIG_NAME} - the printer name</li>
 * </ul>
 */
public interface InventoryPrinter {

    /**
     * The service name under which services of this class must be registered
     * to be picked for inclusion in the configuration report.
     */
    String SERVICE = InventoryPrinter.class.getName(); //$NON-NLS-1$

    /**
     * The property defining the supported rendering modes.
     * The value of this property is either a string or a string array containing
     * valid names of {@link PrinterMode}.
     *
     * If this property is missing or contains invalid values,
     * the printer is ignored.
     */
    String CONFIG_PRINTER_MODES = "felix.inventory.printer.modes"; //$NON-NLS-1$

    /**
     * The unique name of the printer.
     * If this property is missing the printer is ignored.
     * If there are two or more services with the same name, the
     * services with the highest ranking is used.
     */
    String CONFIG_NAME = "felix.inventory.printer.name"; //$NON-NLS-1$

    /**
     * The title displayed by tools when this printer is used. It should be
     * descriptive but short.
     * If this property is missing the printer is ignored.
     */
    String CONFIG_TITLE = "felix.inventory.printer.title"; //$NON-NLS-1$

    /**
     * Optional property controlling whether the printer will be displayed
     * in the web console. By default, a printer is displayed in the
     * web console, unless this property is added with the value 'false'.
     */
    String CONFIG_WEBCONSOLE = "felix.inventory.printer.webconsole"; //$NON-NLS-1$

    /**
     * Prints the configuration report to the given <code>printWriter</code>.
     * Implementations are free to print whatever information they deem useful.
     *
     * If a printer is invoked with a mode it doesn't support ({@link #CONFIG_PRINTER_MODES})
     * the printer should just do/print nothing and directly return.
     *
     * @param mode The render mode.
     * @param printWriter where to write the configuration data. It might be flushed,
     * but must not be closed.
     * @param isZip whether this is included in a zip file or used directly
     */
    void print( PrinterMode mode, PrintWriter printWriter, boolean isZip );
}
