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

import java.io.IOException;
import java.util.zip.ZipOutputStream;

/**
 * This is an optional extension of the {@link StatusPrinter}.
 * If a status printer implements this interface, the printer
 * can add additional attachments to the output of the
 * configuration zip.
 *
 * A service implementing this method must still register itself
 * as a {@link StatusPrinter} but not as a
 * {@link ZipAttachmentProvider} service and the provider
 * should either support {@link PrinterMode.ZIP_FILE_JSON}
 * or {@link PrinterMode.ZIP_FILE_BIN}
 */
public interface ZipAttachmentProvider extends StatusPrinter {

    /**
     * Add attachments to the zip output stream.
     * The attachment provider can add as many attachments in any format
     * as it wants. However it should use the namePrefix to create unique
     * names / paths inside the zip.
     *
     * The general pattern is: creating a zip entry by using the name prefix
     * and a name, adding the entry to the zip output stream, writing
     * the content of the file to the stream, and finally ending the
     * zip entry.
     *
     * @param namePrefix Name prefix to use for zip entries. Ends with a slash.
     * @param zos The zip output stream.
     * @throws IOException
     */
    void addAttachments(final String namePrefix, final ZipOutputStream zos)
    throws IOException;
}
