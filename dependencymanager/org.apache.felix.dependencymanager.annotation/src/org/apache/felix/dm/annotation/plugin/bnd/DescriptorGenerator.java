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
package org.apache.felix.dm.annotation.plugin.bnd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Clazz.QUERY;

/**
 * This helper parses all classes which contain DM annotations, and generates the corresponding component descriptors.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DescriptorGenerator
{
    /**
     * This is the bnd analyzer used to lookup classes containing DM annotations.
     */
    private Analyzer m_analyzer;

    /**
     * This is the generated Dependency Manager descriptors. The hashtable key is the path
     * to a descriptor. The value is a bnd Resource object which contains the content of a
     * descriptor. 
     */
    Map<String, Resource> m_resources = new HashMap<String, Resource>();

    /**
     * This is the generated MetaType XML descriptor, if any Properties/Property annotations have been found.
     */
    private Resource m_metaTypeResource;

    /**
     * Object used to collect logs.
     */
    private final Logger m_logger;

    /**
     * List of imported services found from every ServiceDependency annotations.
     */
    private Set<String> m_importService = new HashSet<String>();

    /**
     * List of exported services found from every service providing components.
     */
    private Set<String> m_exportService = new HashSet<String>();

    /**
     * Creates a new descriptor generator.
     * @param analyzer The bnd analyzer used to lookup classes containing DM annotations.
     * @param debug 
     */
    public DescriptorGenerator(Analyzer analyzer, Logger logger)
    {
        m_analyzer = analyzer;
        m_logger = logger;
    }

    /**
     * Starts the scanning.
     * @return true if some annotations were successfully parsed, false if not. corresponding generated 
     * descriptors can then be retrieved by invoking the getDescriptors/getDescriptorPaths methods.
     */
    public boolean execute() throws Exception
    {
        boolean annotationsFound = false;
        // Try to locate any classes in the wildcarded universe
        // that are annotated with the DependencyManager "Service" annotations.
        Collection<Clazz> expanded = m_analyzer.getClasses("",
                                                           // Parse everything
                                                           QUERY.NAMED.toString(), "*");

        // Create the object which will collect Config Admin MetaTypes.
        MetaType metaType = new MetaType();
            
        for (Clazz c : expanded)
        {
            // Let's parse all annotations from that class !
            AnnotationCollector reader = new AnnotationCollector(m_logger, metaType);
            c.parseClassFileWithCollector(reader);
            if (reader.finish())
            {
                // And store the generated component descriptors in our resource list.
                String name = c.getFQN();
                Resource resource = createComponentResource(reader);
                m_resources.put("META-INF/dependencymanager/" + name, resource);
                annotationsFound = true;
                
                m_importService.addAll(reader.getImportService());
                m_exportService.addAll(reader.getExportService());
            }
        }

        // If some Meta Types have been parsed, then creates the corresponding resource file.
        if (metaType.getSize() > 0)
        {
            m_metaTypeResource = createMetaTypeResource(metaType);
        }
        return annotationsFound;
    }

    /**
     * Returns the path of the descriptor.
     * @return the path of the generated descriptors.
     */
    public String getDescriptorPaths()
    {
        StringBuilder descriptorPaths = new StringBuilder();
        String del = "";
        for (Map.Entry<String, Resource> entry : m_resources.entrySet())
        {
            descriptorPaths.append(del);
            descriptorPaths.append(entry.getKey());
            del = ",";
        }
        return descriptorPaths.toString();
    }

    /**
     * Returns the list of the generated descriptors.
     * @return the list of the generated descriptors.
     */
    public Map<String, Resource> getDescriptors()
    {
        return m_resources;
    }

    /**
     * Returns the MetaType resource.
     */
    public Resource getMetaTypeResource() {
        return m_metaTypeResource;
    }
    
    /**
     * Returns set of all imported services. Imported services are deduced from every
     * @ServiceDependency annotations.
     * @return the list of imported services
     */
    public Set<String> getImportService()
    {
        return m_importService;
    }

    /**
     * Returns set of all exported services. Imported services are deduced from every
     * annotations which provides a service (@Component, etc ...)
     * @return the list of exported services
     */
    public Set<String> getExportService()
    {
        return m_exportService;
    }    

    /**
     * Creates a bnd resource that contains the generated dm descriptor.
     * @param collector 
     * @return
     * @throws IOException
     */
    private Resource createComponentResource(AnnotationCollector collector) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        collector.writeTo(pw);
        pw.close();
        byte[] data = out.toByteArray();
        out.close();
        return new EmbeddedResource(data, 0);
    }
    
    /**
     * Creates a bnd resource that contains the generated metatype descriptor.
     * @param metaType the Object that has collected all meta type informations.
     * @return the meta type resource
     * @throws IOException on any errors
     */
    private Resource createMetaTypeResource(MetaType metaType) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        metaType.writeTo(pw);
        pw.close();
        byte[] data = out.toByteArray();
        out.close();
        return new EmbeddedResource(data, 0);    
    }
}