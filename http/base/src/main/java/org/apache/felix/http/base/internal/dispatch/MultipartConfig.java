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
package org.apache.felix.http.base.internal.dispatch;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;

public final class MultipartConfig
{
    public static final MultipartConfig DEFAULT_CONFIG = new MultipartConfig(null, null, -1, -1);

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Specifies the multipart threshold
     */
    public final int multipartThreshold;

    /**
     * Specifies the multipart location
     */
    public final String multipartLocation;

    /**
     * Specifies the multipart max file size
     */
    public final long multipartMaxFileSize;

    /**
     * Specifies the multipart max request size
     */
    public final long multipartMaxRequestSize;

    public MultipartConfig(final Integer threshold,
            final String location,
            final long maxFileSize,
            final long maxRequestSize)
    {
        if ( threshold != null && threshold > 0)
        {
            this.multipartThreshold = threshold;
        }
        else
        {
            this.multipartThreshold = DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD;
        }
        if ( location != null )
        {
            this.multipartLocation = location;
        }
        else
        {
            this.multipartLocation = TEMP_DIR;
        }
        if ( maxFileSize > 0 || maxFileSize == -1 ) {
            this.multipartMaxFileSize = maxFileSize;
        }
        else
        {
            this.multipartMaxFileSize = -1;
        }
        if ( maxRequestSize > 0 || maxRequestSize == -1 ) {
            this.multipartMaxRequestSize = maxRequestSize;
        }
        else
        {
            this.multipartMaxRequestSize = -1;
        }
    }
}
