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
package org.apache.felix.cm.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;

import junit.framework.TestCase;

public class ConfigurationHandlerTest extends TestCase
{

    private static final String PAR_1 = "mongouri";
    private static final String VAL_1 = "127.0.0.1:27017";
    private static final String PAR_2 = "customBlobStore";
    private static final String VAL_2 = "true";

    private static final String CONFIG =
        "#mongodb URI\n" +
        PAR_1 + "=\"" + VAL_1 + "\"\n" +
        "\n" +
        "  # custom datastore\n" +
        PAR_2 + "=B\"" + VAL_2 + "\"\n";

    public void testComments() throws IOException
    {
        final Dictionary dict = ConfigurationHandler.read(new ByteArrayInputStream(CONFIG.getBytes("UTF-8")));
        assertEquals(2, dict.size());
        assertEquals(VAL_1, dict.get(PAR_1));
        assertEquals(VAL_2, dict.get(PAR_2).toString());
    }
}
