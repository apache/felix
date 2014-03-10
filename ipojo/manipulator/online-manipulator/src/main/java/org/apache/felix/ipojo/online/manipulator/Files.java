/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.online.manipulator;

import static org.apache.felix.ipojo.manipulator.util.Streams.close;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * User: guillaume
 * Date: 04/03/2014
 * Time: 15:33
 */
public class Files {

    public static void dump(URL url, File target) throws IOException {
        dump(url.openStream(), target);
    }

    public static void dump(InputStream stream, File target) throws IOException {
        BufferedInputStream in = new BufferedInputStream(stream);
        FileOutputStream file = new FileOutputStream(target);
        BufferedOutputStream out = new BufferedOutputStream(file);
        int i;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        out.flush();
        close(in, out);
    }
}
