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
package org.apache.felix.scr.impl.metadata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.scr.impl.MockBundle;
import org.apache.felix.scr.impl.logger.MockBundleLogger;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.osgi.service.component.ComponentException;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class ComponentBase extends TestCase
{
    private MockBundleLogger logger;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        logger = new MockBundleLogger();
    }

    private List readMetadata(InputStream in)
        throws IOException, ComponentException, SAXException, Exception
    {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        final SAXParser parser = factory.newSAXParser();

        XmlHandler handler = new XmlHandler(new MockBundle(), logger, false, false);
        parser.parse(in, handler);

        return handler.getComponentMetadataList();
    }

    protected List readMetadata(String filename)
        throws IOException, ComponentException, SAXException, Exception
    {
        try (InputStream in = getClass().getResourceAsStream(filename))
        {
            return readMetadata(in);
        }
    }

    protected List readMetadataFromString(final String source)
        throws IOException, ComponentException, SAXException, Exception
    {
        return readMetadata(new ByteArrayInputStream(source.getBytes()));
    }

    protected ReferenceMetadata getReference(final ComponentMetadata cm,
        final String name)
    {
        List rmlist = cm.getDependencies();
        for (Iterator rmi = rmlist.iterator(); rmi.hasNext();)
        {
            ReferenceMetadata rm = (ReferenceMetadata) rmi.next();
            if (name.equals(rm.getName()))
            {
                return rm;
            }
        }

        // none found
        return null;
    }

    protected PropertyMetadata getPropertyMetadata(final ComponentMetadata cm,
        final String name)
    {
        List pmlist = cm.getPropertyMetaData();
        for (Iterator pmi = pmlist.iterator(); pmi.hasNext();)
        {
            PropertyMetadata pm = (PropertyMetadata) pmi.next();
            if (name.equals(pm.getName()))
            {
                return pm;
            }
        }

        // none found
        return null;
    }

}
