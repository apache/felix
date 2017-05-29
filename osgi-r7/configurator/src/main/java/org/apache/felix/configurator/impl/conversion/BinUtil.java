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
package org.apache.felix.configurator.impl.conversion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;

import org.osgi.framework.Bundle;

public class BinUtil {

    public static volatile File binDirectory;

    public static File extractFile(final Bundle bundle, final String pid, final String path)
    throws IOException {
        final URL url = bundle.getEntry(path);
        if ( url == null ) {
            return null;
        }
        final URLConnection connection = url.openConnection();

        final File dir = new File(binDirectory, URLEncoder.encode(pid, "UTF-8"));
        dir.mkdir();
        final File newFile = new File(dir, UUID.randomUUID().toString());

        try(final BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            final FileOutputStream fos = new FileOutputStream(newFile)) {

            int len = 0;
            final byte[] buffer = new byte[16384];

            while ( (len = in.read(buffer)) > 0 ) {
                fos.write(buffer, 0, len);
            }
        }

        return newFile;
    }
}
