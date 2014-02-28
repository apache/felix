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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.TransformerException;

import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.Project;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.helper.ComponentContainer;
import org.apache.felix.scrplugin.helper.ComponentContainerUtil;
import org.apache.felix.scrplugin.helper.ComponentContainerUtil.ComponentContainerContainer;
import org.apache.felix.scrplugin.helper.DescriptionContainer;
import org.apache.felix.scrplugin.helper.MetatypeAttributeDefinition;
import org.apache.felix.scrplugin.helper.MetatypeContainer;
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
            final Project project,
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
        // write meta type info
        final File mtDir = options.getMetaTypeDirectory();
        final File parentDir = mtDir.getParentFile();

        if (components.size() > 0) {
            // check for metatype.properties
            final File mtProps = new File(project.getClassesDirectory(), "OSGI-INF" + File.separator + "metatype" + File.separator + "metatype.properties");
            final boolean oldStyle = mtProps.exists();
            mtDir.mkdirs();

            final List<String> fileNames = new ArrayList<String>();
            final List<ComponentContainerContainer> containers = ComponentContainerUtil.split(components);
            for(final ComponentContainerContainer ccc : containers) {
                final File useFile = new File(mtDir, ccc.className + ".xml");

                String metatypeLocation = MetaTypeService.METATYPE_DOCUMENTS_LOCATION + "/metatype";

                // check if all labels and descriptions are inlined
                boolean allInlined = true;
                for(final ComponentContainer cc : ccc.components) {
                    final MetatypeContainer mc = cc.getMetatypeContainer();

                    if ( mc.getName() == null ) {
                        if ( oldStyle ) {
                            mc.setName( "%" + cc.getComponentDescription().getName() + ".name");
                        } else {
                            mc.setName("Component " + cc.getComponentDescription().getName());
                        }
                    }
                    if ( mc.getName() != null && mc.getName().startsWith("%") ) {
                        allInlined = false;
                    }
                    if ( mc.getDescription() == null ) {
                        if ( oldStyle ) {
                            mc.setDescription("%" + cc.getComponentDescription().getName() + ".description");
                        } else {
                            mc.setDescription("Description for " + cc.getComponentDescription().getName());
                        }
                    }
                    if ( mc.getDescription() != null && mc.getDescription().startsWith("%") ) {
                        allInlined = false;
                    }
                    for(final MetatypeAttributeDefinition mad : mc.getProperties()) {
                        if ( mad.getName() == null ) {
                            if ( oldStyle ) {
                                mad.setName("%" + mad.getId() + ".name");
                            } else {
                                mad.setName("Property " + mad.getId());
                            }
                        }
                        if ( mad.getName() != null && mad.getName().startsWith("%") ) {
                            allInlined = false;
                        }
                        if ( mad.getDescription() == null ) {
                            if ( oldStyle ) {
                                mad.setDescription("%" + mad.getId() + ".description");
                            } else {
                                mad.setDescription("Description for " + mad.getId());
                            }
                        }
                        if ( mad.getDescription() != null && mad.getDescription().startsWith("%") ) {
                            allInlined = false;
                        }
                    }
                }
                if ( allInlined ) {
                    final Properties metatypeProps = new Properties();

                    // externalize all labels and descriptions
                    for(final ComponentContainer cc : ccc.components) {
                        final MetatypeContainer mc = cc.getMetatypeContainer();

                        final String baseKey = cc.getComponentDescription().getName().replace("$", ".");

                        if ( mc.getName() != null ) {
                            final String key = baseKey + ".name";
                            metatypeProps.put(key, mc.getName());
                            mc.setName("%" + key);
                        }
                        if ( mc.getDescription() != null ) {
                            final String key = baseKey + ".description";
                            metatypeProps.put(key, mc.getDescription());
                            mc.setDescription("%" + key);
                        }
                        for(final MetatypeAttributeDefinition mad : mc.getProperties()) {
                            if ( mad.getName() != null ) {
                                final String key = baseKey + "." + mad.getId() + ".name";
                                metatypeProps.put(key, mad.getName());
                                mad.setName("%" + key);
                            }
                            if ( mad.getDescription() != null ) {
                                final String key = baseKey + "." + mad.getId() + ".description";
                                metatypeProps.put(key, mad.getDescription());
                                mad.setDescription("%" + key);
                            }
                        }
                    }
                    if ( metatypeProps.size() > 0 ) {
                        final int lastDot = useFile.getName().lastIndexOf(".");
                        final String baseName = useFile.getName().substring(0, lastDot);
                        final File propsFile = new File(useFile.getParentFile(), baseName + ".properties");
                        try {
                            metatypeProps.store(new FileWriter(propsFile), null);
                        } catch (IOException e) {
                            throw new SCRDescriptorException("Unable to get metatype.properties", propsFile.getAbsolutePath());
                        }
                        fileNames.add(parentDir.getName() + '/' + mtDir.getName() + '/' + propsFile.getName());
                        metatypeLocation = MetaTypeService.METATYPE_DOCUMENTS_LOCATION + '/' + baseName;
                    }
                }
                logger.info("Generating " + ccc.components.size() + " MetaType Descriptors in " + useFile);
                MetaTypeIO.write(module, ccc.components, useFile, metatypeLocation);
                fileNames.add(parentDir.getName() + '/' + mtDir.getName() + '/' + useFile.getName());
            }

            return fileNames;
        }
        if (mtDir.exists() && !options.isIncremental()) {
            for(final File f : mtDir.listFiles()) {
                if ( f.isFile() ) {
                    logger.debug("Removing obsolete metatype file " + f);
                    f.delete();
                }
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
    private static void write(final DescriptionContainer metaData,
            final List<ComponentContainer> components,
            final File file,
            final String localization)
    throws SCRDescriptorException {
        final String namespace = detectMetatypeVersion(metaData);

        try {
            final ContentHandler contentHandler = IOUtils.getSerializer(file);

            contentHandler.startDocument();
            contentHandler.startPrefixMapping(PREFIX, namespace);

            final AttributesImpl ai = new AttributesImpl();
            IOUtils.addAttribute(ai, "localization", localization);

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

    /**
     * Escape the value according to 105.13.3.21 (validate method)
     */
    private static String escapeDefaultValue(final String value) {
        if ( value == null ) {
            return value;
        }

        String result = value.trim();
        result = result.replace("\\", "\\\\");
        result = result.replace(" ", "\\ ");
        result = result.replace(",", "\\,");
        return result;
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
                buf.append(escapeDefaultValue(ad.getDefaultMultiValue()[i]));
            }
            IOUtils.addAttribute(ai, "default", buf);
        } else {
            IOUtils.addAttribute(ai, "default", escapeDefaultValue(ad.getDefaultValue()));
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
