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

import java.io.PrintWriter;

/**
 * The <code>InventoryPrinter</code> is a service interface to be
 * implemented by providers which want to hook into the display of the
 * current configuration and state of the OSGi framework and application.
 * <p>
 * The following service registration properties further declare the
 * {@code InventoryPrinter} service:
 * <ul>
 * <li>{@link #FORMAT} - the supported formats</li>
 * <li>{@link #TITLE} - the printer title</li>
 * <li>{@link #NAME} - the printer name</li>
 * <li>{@link #WEBCONSOLE} - whether to confine the printer to the Web Console</li>
 * </ul>
 */
public interface InventoryPrinter
{

    /**
     * The service name under which services of this class must be registered
     * to be picked for inclusion in the configuration report.
     */
    String SERVICE = "org.apache.felix.inventory.InventoryPrinter"; //$NON-NLS-1$

    /**
     * The property defining one or more supported formats. The value of this
     * property is either a string, a string array or a Collection<String>
     * containing valid names of the constants defined in the {@link Format}
     * class.
     * <p>
     * Any unknown formats are ignored. If this property is not declared or does
     * not declare any known formats, the {@link Format#TEXT} format is assumed
     * as the printer's supported format.
     */
    String FORMAT = "felix.inventory.printer.format"; //$NON-NLS-1$

    /**
     * The unique name (or label) of the printer.
     * <p>
     * If this property is missing or an empty string, the name is constructed
     * from the string {@code InventoryPrinter} and the service's
     * {@code service.id} property.
     */
    String NAME = "felix.inventory.printer.name"; //$NON-NLS-1$

    /**
     * The title displayed by tools when this printer is used. It should be
     * descriptive but short.
     * <p>
     * If this property is missing or an empty string, the {@link #NAME} value
     * is used as the title.
     */
    String TITLE = "felix.inventory.printer.title"; //$NON-NLS-1$

    /**
     * The inventory printer feature has first class integration with the
     * Apache Felix Web Console. This service registration property can be used
     * to hide an inventory printer service from the Web Console. The service
     * will still be called to generate the single file or ZIP file output of
     * inventory printers.
     * <p>
     * By default, a printer is displayed in the web console, unless this
     * property is set to {@code false}. The property value can either be a
     * boolean or a string.
     */
    String WEBCONSOLE = "felix.inventory.printer.webconsole"; //$NON-NLS-1$

    /**
     * Prints the configuration report to the given <code>printWriter</code>.
     * Implementations are free to print whatever information they deem useful.
     * <p>
     * If a printer is invoked with a format it doesn't support ( {@link #FORMAT})
     * the printer should just do/print nothing and directly return.
     * <p>
     * A printer might be used in one of two different situations: either for
     * directly displaying the information to a user (like in the web console)
     * or the output is included in a ZIP. The printer might want to return
     * different output depending on the usage situation.
     *
     * @param printWriter where to write the data. Implementations may flush the
     *            writer but must not close it.
     * @param format The render format.
     * @param isZip whether this is included in a ZIP file or used directly
     */
    void print(PrintWriter printWriter, Format format, boolean isZip);
}
