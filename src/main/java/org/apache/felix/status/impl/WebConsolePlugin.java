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

import org.apache.felix.status.StatusPrinterHandler;
import org.apache.felix.status.StatusPrinterManager;

/**
 * The web console plugin for a status printer.
 */
public class WebConsolePlugin extends AbstractWebConsolePlugin {

    private static final long serialVersionUID = 1L;

    /** Printer name. */
    private final String printerName;

    /**
     * Constructor
     * @param statusPrinterAdapter The adapter
     */
    WebConsolePlugin(final StatusPrinterManager statusPrinterManager,
            final String printerName) {
        super(statusPrinterManager);
        this.printerName = printerName;
    }

    @Override
    protected StatusPrinterHandler getStatusPrinterHandler() {
        return this.statusPrinterManager.getHandler(this.printerName);
    }
}