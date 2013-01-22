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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Resource;
import aQute.libg.reporter.Reporter;

/**
 * This class is a BND plugin. It scans the target bundle and look for DependencyManager annotations.
 * It can be directly used when using ant and can be referenced inside the ".bnd" descriptor, using
 * the "-plugin" parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AnnotationPlugin implements AnalyzerPlugin, Plugin
{
    private static final String IMPORT_SERVICE = "Import-Service";
    private static final String EXPORT_SERVICE = "Export-Service";
    private BndLogger m_logger;
    private Reporter m_reporter;
    private boolean m_buildImportExportService;

    /**
     * This plugin is called after analysis of the JAR but before manifest
     * generation. When some DM annotations are found, the plugin will add the corresponding 
     * DM component descriptors under META-INF/ directory. It will also set the  
     * "DependencyManager-Component" manifest header (which references the descriptor paths).
     * 
     * @param analyzer the object that is used to retrieve classes containing DM annotations.
     * @return true if the classpath has been modified so that the bundle classpath must be reanalyzed
     * @throws Exception on any errors.
     */
    public boolean analyzeJar(Analyzer analyzer) throws Exception
    {
        analyzer.setExceptions(true);
        try {
            // We'll do the actual parsing using a DescriptorGenerator object.
            DescriptorGenerator generator = new DescriptorGenerator(analyzer, m_logger);

            if (generator.execute())
            {
                // We have parsed some annotations: set the OSGi "DependencyManager-Component" header in the target bundle.
                analyzer.setProperty("DependencyManager-Component", generator.getDescriptorPaths());
                
                // Possibly set the Import-Service/Export-Service header
                if (m_buildImportExportService)
                {
                    // Don't override Import-Service header, if it is found from the bnd directives.
                    if (analyzer.getProperty(IMPORT_SERVICE) == null)
                    {
                        buildImportExportService(analyzer, IMPORT_SERVICE, generator.getImportService());
                    }
                    
                    // Don't override Export-Service header, if already defined
                    if (analyzer.getProperty(EXPORT_SERVICE) == null)
                    {
                        buildImportExportService(analyzer, EXPORT_SERVICE, generator.getExportService());
                    }
                }

                // And insert the generated descriptors into the target bundle.
                Map<String, Resource> resources = generator.getDescriptors();
                for (Map.Entry<String, Resource> entry : resources.entrySet())
                {
                    analyzer.getJar().putResource(entry.getKey(), entry.getValue());
                }

                // Insert the metatype resource, if any.
                Resource metaType = generator.getMetaTypeResource();
                if (metaType != null)
                {
                    analyzer.getJar().putResource("OSGI-INF/metatype/metatype.xml", metaType);
                }
            }
        } 
        
        catch (Throwable t)
        {
            m_logger.error(parse(t));
        }

        // Collect all logs and write it into the analyzer.
        m_logger.getLogs(analyzer);
        
        // When some errors are present in the analyzer logs: it seems that the Bnd ANT task
        // does not report it. So, to work around, we just log the errors ourself, and
        // we throw an Error in case some errors are detected, in order to prevent the bnd ANT
        // task from generating the target bundle.
        if (analyzer.getWarnings().size() > 0)
        {
            for (Iterator<String> e = analyzer.getWarnings().iterator(); e.hasNext();)
            {
                System.out.println(e.next());
            }   
            analyzer.getWarnings().clear();
        }
        
        if (analyzer.getErrors().size() > 0)
        {
            for (Iterator<String> e = analyzer.getErrors().iterator(); e.hasNext();)
            {
                System.err.println(e.next());
            }
            analyzer.getErrors().clear();
            throw new Error("DM Annotation plugin failure");
        }
        return false;
    }

    private void buildImportExportService(Analyzer analyzer, String header, Set<String> services)
    {
        m_logger.info("building %s header with the following services: %s", header, services);
        if (services.size() > 0)
        {
            StringBuilder sb = new StringBuilder();
            for (String service : services)
            {
                sb.append(service);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1); // skip last comma
            analyzer.setProperty(header, sb.toString());
        } 
    }

    public void setProperties(Map<String, String> map)
    {
        String logLevel = map.get("log");
        m_logger = new BndLogger(logLevel == null ? "error" : logLevel);
        String generateImportExportService = map.get("build-import-export-service");
        if (generateImportExportService == null)
        {
            generateImportExportService = "true";
        }
        m_buildImportExportService = Boolean.parseBoolean(generateImportExportService);
    }

    public void setReporter(Reporter reporter)
    {
        m_reporter = reporter;
    }
    
    /**
     * Parse an exception into a string.
     * @param e The exception to parse
     * @return the parsed exception
     */
    private static String parse(Throwable e) {
      StringWriter buffer = new StringWriter();
      PrintWriter  pw = new PrintWriter(buffer);
      e.printStackTrace(pw);
      return (buffer.toString());
    } 
}
