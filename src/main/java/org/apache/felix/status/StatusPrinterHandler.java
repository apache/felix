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
package org.apache.felix.status;

import java.io.PrintWriter;
import java.net.URL;

/**
 * The status printer handler can be used by clients to access
 * a status printer.
 */
public interface StatusPrinterHandler {

    /** The unique name of the printer. */
    String getName();

    /** The human readable title for the status printer. */
    String getTitle();

    /** The optional category for this printer. */
    String getCategory();

    /** All supported modes. */
    PrinterMode[] getModes();

    /** Whether the printer supports this mode. */
    boolean supports( final PrinterMode mode );

    /** Print the configuration in the mode. */
    void printConfiguration( PrinterMode mode, PrintWriter printWriter );

    /** Get the attachments for the zip. */
    URL[] getAttachmentsForZip();
}
