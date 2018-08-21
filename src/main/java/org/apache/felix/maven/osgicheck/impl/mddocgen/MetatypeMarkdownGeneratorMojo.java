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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.metatype.AD;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

@Mojo(
    name = "generate-metatype-doc",
    defaultPhase = LifecyclePhase.PACKAGE,
    threadSafe = false
)
public final class MetatypeMarkdownGeneratorMojo extends AbstractMarkdownMojo {

    // class members

    private static final String[] TYPES  = new String[] { null,
                                                          "String",
                                                          "Long",
                                                          "Integer",
                                                          "Short",
                                                          "Character",
                                                          "Byte",
                                                          "Double",
                                                          "Float",
                                                          "BigInteger",
                                                          "BigDecimal",
                                                          "Boolean",
                                                          "Password" };

    // instance members

    private final MetaDataReader reader = new MetaDataReader();

    // plugin parameters

    @Parameter(defaultValue = "")
    private String locale;

    @Parameter(defaultValue="${project.build.outputDirectory}")
    private File metatypeDirectory;

    @Parameter(defaultValue="${project.build.directory}/mddoc/metatype/${project.artifactId}/${project.version}")
    private File metatypeMarkdownDirectory;

    @Parameter(defaultValue="${project.name} ${project.version} Metatypes", readonly = true)
    private String readmeTitle;

    // methods

    @Override
    protected String getReadmeTitle() {
        return readmeTitle;
    }

    @Override
    protected File getSourceDir() {
        return metatypeDirectory;
    }

    @Override
    protected File getTargetDir() {
        return metatypeMarkdownDirectory;
    }

    @Override
    protected String getIncludes() {
        return "**/OSGI-INF/metatype/*.xml";
    }

    @Override
    protected void handle(Collection<File> metatypes) {
        for (File metatypeFile : metatypes) {
            MetaData metaData = readMetaData(metatypeFile);

            if (metaData != null) {
                Properties localizationProperties = readLocalizationProperties(metatypeFile, metaData);

                // produce the output(s)

                @SuppressWarnings("unchecked") // verified in the source code
                Map<String, OCD> ocds = metaData.getObjectClassDefinitions();
                for (OCD ocd : ocds.values()) {
                    String id = ocd.getID();

                    if (isExcluded(id)) {
                        continue;
                    }

                    ClassName className = ClassName.get(id);

                    // write in the index

                    doIndex(className.getPackageName(), className);

                    // write in the related metatype file

                    File targetDir = new File(metatypeMarkdownDirectory, className.getPackagePath());
                    if (!targetDir.exists()) {
                        targetDir.mkdirs();
                    }
                    File targetFile = new File(targetDir, className.getSimpleName() + ".md");

                    PrintWriter writer = null;
                    try {
                        writer = newPrintWriter(targetFile);

                        // generic properties

                        writer.format("# %s%n%n", getlocalizedLabel(ocd.getName(), localizationProperties));
                        writer.format("## `%s`%n%n", id);
                        writer.println(getlocalizedLabel(ocd.getDescription(), localizationProperties));
                        writer.println();
                        writer.println("| ID  | Name | Required | Type | Default value | Description |");
                        writer.println("| --- | ---- | -------- | ---- | ------------- | ----------- |");

                        // main attributes

                        @SuppressWarnings("unchecked") // verified in the source code
                        Map<String, AD> attributes = ocd.getAttributeDefinitions();
                        printAttributes(attributes, localizationProperties, writer);

                        // optional attributes

                        @SuppressWarnings("unchecked") // verified in the source code
                        Map<String, AD> optionalAttributes = ocd.getOptionalAttributes();
                        printAttributes(optionalAttributes, localizationProperties, writer);
                    } catch (IOException e) {
                        getLog().error("An error occurred while rendering documentation in " + targetFile, e);
                    } finally {
                        IOUtil.close(writer);
                    }
                }
            }
        }
    }

    private MetaData readMetaData(File metatypeFile) {
        getLog().debug("Analyzing '" + metatypeFile + "' meta type file...");

        // read the original XML file
        FileInputStream inputStream = null;
        MetaData metadata = null;
        try {
            inputStream = new FileInputStream(metatypeFile);
            metadata = reader.parse(inputStream);

            getLog().debug("Metaype file '" + metatypeFile + "' successfully load");
        } catch (Exception e) {
            getLog().error("Metatype file '"
                           + metatypeFile
                           + "' could not be read", e);
        } finally {
            IOUtil.close(inputStream);
        }

        return metadata;
    }

    private Properties readLocalizationProperties(File metatypeFile, MetaData metaData) {
        StringBuilder propertiesFileName = new StringBuilder(metaData.getLocalePrefix());
        if (locale != null && !locale.isEmpty()) {
            propertiesFileName.append('_')
                              .append(locale);
        }
        propertiesFileName.append(".properties");

        // **/OSGI-INF/metatype/*.xml
        File mainDir = metatypeFile.getParentFile().getParentFile().getParentFile();
        File propertiesFile = FileUtils.resolveFile(mainDir, propertiesFileName.toString());

        Properties properties = new Properties();
        FileInputStream inputStream = null;
        if (propertiesFile.exists()) {
            getLog().debug("Loading properties file '" + propertiesFile + "'...");

            try {
                inputStream = new FileInputStream(propertiesFile);
                properties.load(inputStream);

                getLog().debug("Properties file '" + propertiesFile + "' successfully load");
            } catch (IOException e) {
                getLog().error("Properties file '"
                               + metatypeFile
                               + "' can not be read, labels could not be human-readable", e);
            } finally {
                IOUtil.close(inputStream);
            }
        } else {
            getLog().warn("File '"
                        + propertiesFile
                        + "' for '"
                        + metatypeFile 
                        + "' does not exist, labels could not be human-readable");
        }

        return properties;
    }

    private static String getlocalizedLabel(String originalKey, Properties properties) {
        if (originalKey != null && originalKey.startsWith("%")) {
            String key = originalKey.substring(1);

            if (properties.containsKey(key)) {
                return properties.getProperty(key);
            }
        }

        // just return the string unmodified
        return originalKey;
    }

    private static void printAttributes(Map<String, AD> attributes,
                                 Properties properties,
                                 PrintWriter writer) {
        if (attributes != null && !attributes.isEmpty()) {
            for (AD attribute : attributes.values()) {
                writer.format("| %s | %s | `%s` | `%s` | `%s` | %s |%n",
                              attribute.getID(),
                              getlocalizedLabel(attribute.getName(), properties),
                              attribute.isRequired(),
                              TYPES[attribute.getType()],
                              Arrays.toString(attribute.getDefaultValue()),
                              getlocalizedLabel(attribute.getDescription(), properties));
            }
        }
    }

}
