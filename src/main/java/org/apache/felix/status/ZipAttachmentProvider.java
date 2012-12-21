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
 * can add additional attachments to the output of the configuration zip.
 *
 * A service implementing this method must still register itself
 * as a {@link StatusPrinter} but not as a
 * {@link ZipAttachmentProvider} service.
 */
public interface ZipAttachmentProvider extends StatusPrinter {

    /**
     * Return an array of attachments
     * The returned list should contain URLs pointing to the
     * attachments.
     * @return An array of URLs or null.
     */
    void addAttachments(final String namePrefix, final ZipOutputStream zos)
    throws IOException;
}
