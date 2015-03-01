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
import java.util.Map;
import java.util.Set;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * This class is a BND plugin. It scans the target bundle and look for DependencyManager annotations.
 * It can be directly used when using ant and can be referenced inside the ".bnd" descriptor, using
 * the "-plugin" parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AnnotationPlugin implements AnalyzerPlugin, Plugin {
    private static final String IMPORT_SERVICE = "Import-Service";
    private static final String EXPORT_SERVICE = "Export-Service";
    private static final String REQUIRE_CAPABILITY = "Require-Capability";

    private static final String LOGLEVEL = "log";
    private static final String BUILD_IMPEXT = "build-import-export-service";
    private static final String ADD_REQUIRE_CAPABILITY = "add-require-capability";
    private static final String DM_RUNTIME_CAPABILITY = "osgi.extender; filter:=\"(&(osgi.extender=org.apache.felix.dependencymanager.runtime)(version>=4.0.0))\"";
    private BndLogger m_logger;
    private Reporter m_reporter;
    private boolean m_buildImportExportService;
    private boolean m_addRequireCapability;
    private Map<String, String> m_properties;

    public void setReporter(Reporter reporter) {
        m_reporter = reporter;
    }

    public void setProperties(Map<String, String> map) {
        m_properties = map;
    }

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
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        m_logger = new BndLogger(m_reporter, analyzer.getBsn());

        try {
            init(analyzer);

            // We'll do the actual parsing using a DescriptorGenerator object.
            DescriptorGenerator generator = new DescriptorGenerator(analyzer, m_logger);

            if (generator.execute()) {
                // We have parsed some annotations: set the OSGi "DependencyManager-Component" header in the target bundle.
                analyzer.setProperty("DependencyManager-Component", generator.getDescriptorPaths());

                if (m_addRequireCapability) {
                    // Add our Require-Capability header
                    buildRequireCapability(analyzer);
                }
                
                // Possibly set the Import-Service/Export-Service header
                if (m_buildImportExportService) {
                    // Don't override Import-Service header, if it is found from the bnd directives.
                    if (analyzer.getProperty(IMPORT_SERVICE) == null) {
                        buildImportExportService(analyzer, IMPORT_SERVICE, generator.getImportService());
                    }

                    // Don't override Export-Service header, if already defined
                    if (analyzer.getProperty(EXPORT_SERVICE) == null) {
                        buildImportExportService(analyzer, EXPORT_SERVICE, generator.getExportService());
                    }
                }

                // And insert the generated descriptors into the target bundle.
                Map<String, Resource> resources = generator.getDescriptors();
                for (Map.Entry<String, Resource> entry : resources.entrySet()) {
                    analyzer.getJar().putResource(entry.getKey(), entry.getValue());
                }

                // Insert the metatype resource, if any.
                Resource metaType = generator.getMetaTypeResource();
                if (metaType != null) {
                    analyzer.getJar().putResource("OSGI-INF/metatype/metatype.xml", metaType);
                }
            }
        }

        catch (Throwable t) {
            m_logger.error(parse(t));
        }

        finally {
            m_logger.close();
        }

        return false; // do not reanalyze bundle classpath because our plugin has not changed it.
    }

    private void init(Analyzer analyzer) {
        m_logger.setLevel(parseOption(m_properties, LOGLEVEL, BndLogger.Level.Warn.toString()));
        m_buildImportExportService = parseOption(m_properties, BUILD_IMPEXT, false);
        m_addRequireCapability = parseOption(m_properties, ADD_REQUIRE_CAPABILITY, false);
        analyzer.setExceptions(true);
        m_logger.info("Initialized Bnd DependencyManager plugin: buildImportExport=%b", m_buildImportExportService);
    }

    private String parseOption(Map<String, String> opts, String name, String def) {
        String value = opts.get(name);
        return value == null ? def : value;
    }

    private boolean parseOption(Map<String, String> opts, String name, boolean def) {
        String value = opts.get(name);
        return value == null ? def : Boolean.valueOf(value);
    }

    private void buildImportExportService(Analyzer analyzer, String header, Set<String> services) {
        m_logger.info("building %s header with the following services: %s", header, services);
        if (services.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String service : services) {
                sb.append(service);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1); // skip last comma
            analyzer.setProperty(header, sb.toString());
        }
    }

    private void buildRequireCapability(Analyzer analyzer) {
        String requireCapability = analyzer.getProperty(REQUIRE_CAPABILITY);
        if (requireCapability == null) {
            analyzer.setProperty(REQUIRE_CAPABILITY, DM_RUNTIME_CAPABILITY);
        } else {
            StringBuilder sb = new StringBuilder(requireCapability).append(",").append(DM_RUNTIME_CAPABILITY);
            analyzer.setProperty(REQUIRE_CAPABILITY, sb.toString());
        }
    }

    /**
     * Parse an exception into a string.
     * @param e The exception to parse
     * @return the parsed exception
     */
    private static String parse(Throwable e) {
        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.printStackTrace(pw);
        return (buffer.toString());
    }
}
