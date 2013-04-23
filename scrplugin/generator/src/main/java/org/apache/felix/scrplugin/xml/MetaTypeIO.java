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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.helper.ComponentContainer;
import org.apache.felix.scrplugin.helper.ComponentContainerUtil;
import org.apache.felix.scrplugin.helper.ComponentContainerUtil.ComponentContainerContainer;
import org.apache.felix.scrplugin.helper.DescriptionContainer;
import org.apache.felix.scrplugin.helper.MetatypeAttributeDefinition;
import org.apache.felix.scrplugin.helper.MetatypeContainer;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.osgi.service.metatype.MetaTypeService;
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

    private static final String NAMESPACE_URI_10 = "http://www.osgi.org/xmlns/metatype/v1.0.0";
    private static final String NAMESPACE_URI_12 = "http://www.osgi.org/xmlns/metatype/v1.2.0";

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

    public static List<String> generateDescriptors(final DescriptionContainer module,
            final Options options,
            final Log logger)
                    throws SCRDescriptorException {
        // create a list with relevant components
        final List<ComponentContainer> components = new ArrayList<ComponentContainer>();
        for(final ComponentContainer component : module.getComponents()) {
            if ( component.getMetatypeContainer() != null ) {
                components.add(component);
            }
        }
        // write meta type info if there is a file name
        if (!StringUtils.isEmpty(options.getMetaTypeName())) {
            final File mtDir = options.getMetaTypeDirectory();
            final File parentDir = mtDir.getParentFile();

            final File mtFile = new File(mtDir, options.getMetaTypeName());

            if (components.size() > 0) {
                mtDir.mkdirs();

                final List<String> fileNames = new ArrayList<String>();
                final List<ComponentContainerContainer> containers = ComponentContainerUtil.split(components, options.isGenerateSeparateDescriptors());
                for(final ComponentContainerContainer ccc : containers) {
                    final File useFile;
                    if ( ccc.className == null ) {
                        useFile = mtFile;
                    } else {
                        useFile = new File(mtDir, ccc.className + ".xml");
                    }
                    logger.info("Generating " + ccc.components.size() + " MetaType Descriptors in " + useFile);
                    MetaTypeIO.write(module, ccc.components, useFile);
                    fileNames.add(parentDir.getName() + '/' + mtDir.getName() + '/' + useFile.getName());

                }

                return fileNames;
            }
            if (mtFile.exists()) {
                mtFile.delete();
            }

        } else {
            if (components.size() > 0) {
                logger.info("Meta type file name is not set: meta type info is not written.");
            }
        }
        return null;
    }

    /**
     * Detect correct version to use.
     * If password type is used, we have to use v1.2
     */
    private static String detectMetatypeVersion(final DescriptionContainer container) {
        for(final ComponentContainer comp : container.getComponents()) {
            if ( comp.getMetatypeContainer() != null ) {
                final Iterator<MetatypeAttributeDefinition> i = comp.getMetatypeContainer().getProperties().iterator();
                while ( i.hasNext() ) {
                    final MetatypeAttributeDefinition ad = i.next();
                    if ( ad.getType().equalsIgnoreCase("password") ) {
                        return NAMESPACE_URI_12;
                    }
                }
            }
        }
        return NAMESPACE_URI_10;
    }

    /**
     * Generate the xml top level element and start streaming
     * the meta data.
     * @param metaData
     * @param contentHandler
     * @throws SAXException
     */
    private static void write(final DescriptionContainer metaData, final List<ComponentContainer> components, final File file)
    throws SCRDescriptorException {
        final String namespace = detectMetatypeVersion(metaData);

        try {
            final ContentHandler contentHandler = IOUtils.getSerializer(file);

            contentHandler.startDocument();
            contentHandler.startPrefixMapping(PREFIX, namespace);

            final AttributesImpl ai = new AttributesImpl();
            IOUtils.addAttribute(ai, "localization", MetaTypeService.METATYPE_DOCUMENTS_LOCATION + "/metatype");

            contentHandler.startElement(namespace, METADATA_ELEMENT, METADATA_ELEMENT_QNAME, ai);
            IOUtils.newline(contentHandler);

            for(final ComponentContainer comp : components) {
                if ( comp.getMetatypeContainer() != null ) {
                    generateOCDXML(comp.getMetatypeContainer(), contentHandler);
                    generateDesignateXML(comp.getMetatypeContainer(), contentHandler);
                }
            }

            // end wrapper element
            contentHandler.endElement(namespace, METADATA_ELEMENT, METADATA_ELEMENT_QNAME);
            IOUtils.newline(contentHandler);
            contentHandler.endPrefixMapping(PREFIX);
            contentHandler.endDocument();
        } catch (final IOException e) {
            throw new SCRDescriptorException("Unable to generate xml", file.toString(), e);
        } catch (final TransformerException e) {
            throw new SCRDescriptorException("Unable to generate xml", file.toString(), e);
        } catch (final SAXException e) {
            throw new SCRDescriptorException("Unable to generate xml", file.toString(), e);
        }
    }

    private static void generateOCDXML(final MetatypeContainer ocd, final ContentHandler contentHandler)
            throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "id", ocd.getId());
        IOUtils.addAttribute(ai, "name", ocd.getName());
        IOUtils.addAttribute(ai, "description", ocd.getDescription());
        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(INNER_NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME, ai);

        if ( ocd.getProperties().size() > 0 ) {
            IOUtils.newline(contentHandler);
            final Iterator<MetatypeAttributeDefinition> i = ocd.getProperties().iterator();
            while ( i.hasNext() ) {
                final MetatypeAttributeDefinition ad = i.next();
                generateAttributeXML(ad, contentHandler);
            }
            IOUtils.indent(contentHandler, 1);
        }

        contentHandler.endElement(INNER_NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    private static void generateAttributeXML(final MetatypeAttributeDefinition ad, final ContentHandler contentHandler)
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

    private static void generateDesignateXML(final MetatypeContainer designate, final ContentHandler contentHandler)
            throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "pid", designate.getId());
        IOUtils.addAttribute(ai, "factoryPid", designate.getFactoryPid());
        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(INNER_NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME, ai);
        IOUtils.newline(contentHandler);

        generateObjectXML(designate, contentHandler);

        IOUtils.indent(contentHandler, 1);
        contentHandler.endElement(INNER_NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    private static void generateObjectXML(final MetatypeContainer obj, final ContentHandler contentHandler)
            throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "ocdref", obj.getId());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME);
        IOUtils.newline(contentHandler);
    }
}
