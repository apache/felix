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
package org.apache.felix.bundleplugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.jar.Manifest;

import junit.framework.TestCase;

public class ManifestWriterTest extends TestCase
{

    public void testNiceManifest() throws Exception
    {
        // This manifest has an export clause ending on char 73
        Manifest manifest = new Manifest();
        manifest.read(getClass().getResourceAsStream("/test2.mf"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ManifestWriter.outputManifest( manifest, baos, true );

        Manifest manifest2 = new Manifest();
        manifest2.read(new ByteArrayInputStream(baos.toByteArray()));

        assertEquals( toString(manifest, false), toString(manifest2, false) );

    }

    String toString(Manifest manifest, boolean nice) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ManifestWriter.outputManifest( manifest, baos, nice );
        return baos.toString();
    }

}
