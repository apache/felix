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
package org.apache.felix.scrplugin.xml;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.om.metatype.AttributeDefinition;
import org.apache.felix.scrplugin.om.metatype.Designate;
import org.apache.felix.scrplugin.om.metatype.MTObject;
import org.apache.felix.scrplugin.om.metatype.MetaData;
import org.apache.felix.scrplugin.om.metatype.OCD;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * <code>MetaType</code>
 *
 * is a helper class to read and write meta type service files.
 *
 */
public class MetaTypeIO {

    private static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/metatype/v1.0.0";

    private static final String INNER_NAMESPACE_URI = "";

    private static final String PREFIX = "metatype";

    private static final String METADATA_ELEMENT = "MetaData";
    private static final String METADATA_ELEMENT_QNAME = PREFIX + ':' + METADATA_ELEMENT;

    private static final String OCD_ELEMENT = "OCD";
    private static final String OCD_ELEMENT_QNAME = OCD_ELEMENT;

    private static final String DESIGNATE_ELEMENT = "Designate";
    private static final String DESIGNATE_ELEMENT_QNAME = DESIGNATE_ELEMENT;

    private static final String OBJECT_ELEMENT = "Object";
    private static final String OBJECT_ELEMENT_QNAME = OBJECT_ELEMENT;

    private static final String AD_ELEMENT = "AD";
    private static final String AD_ELEMENT_QNAME = AD_ELEMENT;

    private static final String OPTION_ELEMENT = "Option";
    private static final String OPTION_ELEMENT_QNAME = OPTION_ELEMENT;

    public static List<String> generateDescriptors(final MetaData metaData, final Options options, final Log logger)
    throws SCRDescriptorException {
        // write meta type info if there is a file name
        if (!StringUtils.isEmpty(options.getMetaTypeName())) {
            final String path = "OSGI-INF" + File.separator + "metatype" + File.separator + options.getMetaTypeName();
            final File mtFile = new File(options.getOutputDirectory(), path);
            final int size = metaData.getOCDs().size() + metaData.getDesignates().size();
            if (size > 0) {
                logger.info("Generating " + size + " MetaType Descriptors to " + mtFile);
                mtFile.getParentFile().mkdirs();
                MetaTypeIO.write(metaData, mtFile);
                return Collections.singletonList(path.replace(File.separatorChar, '/'));
            }
            if (mtFile.exists()) {
                mtFile.delete();
            }

        } else {
            logger.info("Meta type file name is not set: meta type info is not written.");
        }
        return null;
    }

    private static void write(final MetaData metaData, final File file)
    throws SCRDescriptorException {
        try {
            generateXML(metaData, IOUtils.getSerializer(file));
        } catch (final SAXException e) {
            throw new SCRDescriptorException("Unable to generate xml", file.toString(), e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the meta data.
     * @param metaData
     * @param contentHandler
     * @throws SAXException
     */
    private static void generateXML(final MetaData metaData, final ContentHandler contentHandler)
    throws SAXException {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, NAMESPACE_URI);

        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "localization", metaData.getLocalization());

        contentHandler.startElement(NAMESPACE_URI, METADATA_ELEMENT, METADATA_ELEMENT_QNAME, ai);
        IOUtils.newline(contentHandler);

        for(final OCD ocd : metaData.getOCDs()) {
            generateXML(ocd, contentHandler);
        }
        for(final Designate d : metaData.getDesignates()) {
            generateXML(d, contentHandler);
        }

        // end wrapper element
        contentHandler.endElement(NAMESPACE_URI, METADATA_ELEMENT, METADATA_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    private static void generateXML(OCD ocd, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "id", ocd.getId());
        IOUtils.addAttribute(ai, "name", ocd.getName());
        IOUtils.addAttribute(ai, "description", ocd.getDescription());
        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(INNER_NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME, ai);

        if ( ocd.getProperties().size() > 0 ) {
            IOUtils.newline(contentHandler);
            final Iterator<AttributeDefinition> i = ocd.getProperties().iterator();
            while ( i.hasNext() ) {
                final AttributeDefinition ad = i.next();
                generateXML(ad, contentHandler);
            }
            IOUtils.indent(contentHandler, 1);
        }

        contentHandler.endElement(INNER_NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    private static void generateXML(AttributeDefinition ad, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "id", ad.getId());
        IOUtils.addAttribute(ai, "type", ad.getType());
        if ( ad.getDefaultMultiValue() != null ) {
            final StringBuffer buf = new StringBuffer();
            for(int i=0; i<ad.getDefaultMultiValue().length; i++) {
                if ( i > 0 ) {
                    buf.append(',');
                }
                buf.append(ad.getDefaultMultiValue()[i]);
            }
            IOUtils.addAttribute(ai, "default", buf);
        } else {
            IOUtils.addAttribute(ai, "default", ad.getDefaultValue());
        }
        IOUtils.addAttribute(ai, "name", ad.getName());
        IOUtils.addAttribute(ai, "description", ad.getDescription());
        IOUtils.addAttribute(ai, "cardinality", ad.getCardinality());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, AD_ELEMENT, AD_ELEMENT_QNAME, ai);

        if (ad.getOptions() != null && ad.getOptions().size() > 0) {
            IOUtils.newline(contentHandler);
            for (Iterator<Map.Entry<String, String>> oi=ad.getOptions().entrySet().iterator(); oi.hasNext(); ) {
                final Map.Entry<String, String> entry = oi.next();
                ai.clear();
                IOUtils.addAttribute(ai, "value", entry.getKey());
                IOUtils.addAttribute(ai, "label", entry.getValue());
                IOUtils.indent(contentHandler, 3);
                contentHandler.startElement(INNER_NAMESPACE_URI, OPTION_ELEMENT, OPTION_ELEMENT_QNAME, ai);
                contentHandler.endElement(INNER_NAMESPACE_URI, OPTION_ELEMENT, OPTION_ELEMENT_QNAME);
                IOUtils.newline(contentHandler);
            }
            IOUtils.indent(contentHandler, 2);
        }

        contentHandler.endElement(INNER_NAMESPACE_URI, AD_ELEMENT, AD_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    private static void generateXML(Designate designate, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "pid", designate.getPid());
        IOUtils.addAttribute(ai, "factoryPid", designate.getFactoryPid());
        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(INNER_NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME, ai);
        IOUtils.newline(contentHandler);

        generateXML(designate.getObject(), contentHandler);

        IOUtils.indent(contentHandler, 1);
        contentHandler.endElement(INNER_NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    private static void generateXML(MTObject obj, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "ocdref", obj.getOcdref());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }
}
