/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.maven.osgicheck.impl.mddocgen;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.Faker;
import org.apache.felix.scr.impl.metadata.PropertyMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;

@Mojo(
    name = "generate-services-doc",
    defaultPhase = LifecyclePhase.PACKAGE,
    threadSafe = false
)
public final class ServicesMarkdownGeneratorMojo extends AbstractMarkdownMojo {

    // plugin parameters

    @Parameter(defaultValue="${project.build.outputDirectory}")
    private File servicesDirectory;

    @Parameter(defaultValue="${project.build.directory}/mddoc/services/${project.artifactId}/${project.version}")
    private File servicesMarkdownDirectory;

    @Parameter(defaultValue="${project.name} ${project.version} Services", readonly = true)
    private String readmeTitle;

    private final MavenScrLogger mavenScrLogger = new MavenScrLogger(getLog());

    @Override
    protected String getReadmeTitle() {
        return readmeTitle;
    }

    @Override
    protected File getSourceDir() {
        return servicesDirectory;
    }

    @Override
    protected File getTargetDir() {
        return servicesMarkdownDirectory;
    }

    @Override
    protected String getIncludes() {
        return "**/OSGI-INF/*.xml";
    }

    @Override
    protected void handle(Collection<File> services) {
        for (File serviceFile : services) {
            List<ComponentMetadata> metadata = readComponentMetadata(serviceFile);

            for (ComponentMetadata component : metadata) {
                if (isExcluded(component.getImplementationClassName())) {
                    continue;
                }

                ServiceMetadata serviceMetadata = component.getServiceMetadata();
                if (serviceMetadata != null) {
                    String[] providedServices = serviceMetadata.getProvides();
                    if (providedServices != null && providedServices.length > 0) {
                        ClassName className = ClassName.get(component.getImplementationClassName());

                        // index all services first

                        for (String providedService : providedServices) {
                            // index each service impl to the related provided

                            doIndex(providedService, className);
                        }

                        // write related impl metadata
                        File targetDir = new File(servicesMarkdownDirectory, className.getPackagePath());
                        if (!targetDir.exists()) {
                            targetDir.mkdirs();
                        }
                        File targetFile = new File(targetDir, className.getSimpleName() + ".md");

                        PrintWriter writer = null;
                        try {
                            writer = newPrintWriter(targetFile);

                            // generic properties

                            writer.format("# %s%n%n", component.getName());

                            // terrible hack, but there's no other way
                            List<PropertyMetadata> properties = Faker.getPropertyMetaData(component);
                            if (!properties.isEmpty()) {
                                writer.println("# Properties");
                                writer.println();
                                writer.println("| Name | Type | Value |");
                                writer.println("| ---- | ---- | ----- |");

                                for (PropertyMetadata property : properties) {
                                    writer.format("| %s | %s | %s |%n",
                                                  property.getName(),
                                                  property.getType() != null ? property.getType() : "String",
                                                  property.getValue().getClass().isArray() ? Arrays.toString((Object[]) property.getValue()) : property.getValue());
                                }
                            }
                        } catch (IOException e) {
                            getLog().error("An error occurred while rendering documentation in " + targetFile, e);
                        } finally {
                            IOUtil.close(writer);
                        }
                    }
                }
            }
        }
    }

    private List<ComponentMetadata> readComponentMetadata(File serviceFile) {
        getLog().debug("Analyzing '" + serviceFile + "' SCR file...");

        // read the original XML file
        FileReader reader = null;
        List<ComponentMetadata> metadata = null;

        try {
            reader = new FileReader(serviceFile);

            XmlHandler xmlHandler = new XmlHandler(new SyntheticBundle(servicesDirectory, serviceFile), mavenScrLogger, false, true);
            KXml2SAXParser parser = new KXml2SAXParser(reader);
            parser.parseXML(xmlHandler);

            getLog().debug("SCR file '" + serviceFile + "' successfully load");

            metadata = xmlHandler.getComponentMetadataList();
        } catch (Exception e) {
            getLog().error("SCR file '"
                           + serviceFile
                           + "' could not be read", e);
            metadata = Collections.emptyList();
        } finally {
            IOUtil.close(reader);
        }

        return metadata;
    }

}
