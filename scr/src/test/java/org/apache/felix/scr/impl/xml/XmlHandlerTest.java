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
package org.apache.felix.scr.impl.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.scr.impl.logger.MockBundleLogger;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

public class XmlHandlerTest {

    @Test
    public void testPropertiesWithoutValue() throws Exception {
        final URL url = this.getClass().getClassLoader().getResource("parsertest-nopropvalue.xml");
        parse(url);
    }

    private void parse(final URL descriptorURL) throws Exception {
        final Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getLocation()).thenReturn("bundle");

        InputStream stream = null;
        try {
            stream = descriptorURL.openStream();

            XmlHandler handler = new XmlHandler(bundle, new MockBundleLogger(), false, false);
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final SAXParser parser = factory.newSAXParser();

            parser.parse(stream, handler);

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
            }
        }

    }
}
