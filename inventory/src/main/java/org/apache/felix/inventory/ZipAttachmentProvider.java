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

import java.io.IOException;
import java.util.zip.ZipOutputStream;

/**
 * This is an optional extension of the {@link InventoryPrinter}.
 * If a inventory printer implements this interface, the printer
 * can add additional attachments to the output of the
 * configuration ZIP.
 * <p>
 * A service implementing this method must register itself as a
 * {@link InventoryPrinter} but not as a {@link ZipAttachmentProvider} service.
 * When writing output to a ZIP file, this method is called if the
 * {@link InventoryPrinter} service implements this interface.
 */
public interface ZipAttachmentProvider
{

    /**
     * Add attachments to the zip output stream. The attachment provider can add
     * as many attachments in any format as it wants. However it should use the
     * namePrefix to create unique names / paths inside the zip.
     * <p>
     * The general pattern is to do for each entry to be added:
     * <ol>
     * <li>Create ZipEntry with a name composed of {@code namePrefix} and a name
     * unique to the attachement provider, e.g. {@code namePrefix + "att1.txt"}.
     * </li>
     * <li>Add the ZipEntry to the ZIP file {@code zos}.</li>
     * <li>Write the contents of the entry; for example copying a filesystem
     * file to the ZIP file {@code zos}.</li>
     * <li>Close the ZipEntry.</li>
     * </ol>
     * @param zos The zip output stream.
     * @param namePrefix Name prefix to use for zip entries. Ends with a slash.
     *
     * @throws IOException If an error occurrs writing the ZIP entry. This may
     *             also be caused by reading some file system file to be added
     *             to the ZIP file.
     */
    void addAttachments(final ZipOutputStream zos, final String namePrefix) throws IOException;
}
