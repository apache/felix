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
package org.apache.felix.scrplugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;


/**
 * Options for the {@link SCRDescriptorGenerator}
 */
public class Options {

    private boolean generateAccessors = true;

    private boolean strictMode = false;

    private Map<String, String> properties = Collections.emptyMap();

    private SpecVersion specVersion;

    private File outputDirectory;

    private String scrName = "serviceComponents.xml";

    private String metaTypeName = "metatype.xml";

    public boolean isGenerateAccessors() {
        return generateAccessors;
    }

    /**
     * Defines whether bind and unbind methods are automatically created by
     * the SCR descriptor generator.
     * <p>
     * The generator uses the ASM library to create the method byte codes
     * directly inside the class files. If bind and unbind methods are not
     * to be created, the generator fails if such methods are missing.
     * <p>
     * The default value of this property is <code>true</code>.
     */
    public void setGenerateAccessors(boolean generateAccessors) {
        this.generateAccessors = generateAccessors;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Defines whether warnings should be considered as errors and thus cause
     * the generation process to fail.
     * <p>
     * The default value of this property is <code>false</code>.
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Sets global properties to be set for each descriptor. If a descriptor
     * provides properties of the same name, the descriptor properties are preferred
     * over the properties provided here.
     * <p>
     * The are no default global properties.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public SpecVersion getSpecVersion() {
        return specVersion;
    }

    /**
     * Sets the Declarative Services specification version number to be forced
     * on the declarations.
     * <p>
     * Supported values for this property are <code>null</code> to autodetect
     * the specification version, or one of the enum values from
     * {@link SpecVersion}.
     * <p>
     * The default is to generate the descriptor version according to the
     * capabilities used by the descriptors. If no 1.1 capabilities, such as
     * <code>configuration-policy</code>, are used, version 1.0 is used,
     * otherwise a 1.1 descriptor is generated.
     */
    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    /**
     * Sets the directory where the descriptor files will be created.
     * <p>
     * This field has no default value and this setter <b>must</b> called prior to calling {@link #execute()}.
     */
    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Sets the name of the SCR declaration descriptor file. This file will be
     * created in the <i>OSGI-INF</i> directory below the {@link #setOutputDirectory(File) output directory}.
     * <p>
     * This file will be overwritten if already existing. If no descriptors are created the file is actually removed.
     * <p>
     * The default value of this property is <code>serviceComponents.xml</code>.
     */
    public void setSCRName(final String finalName) {
        this.scrName = finalName;
    }

    /**
     * Sets the name of the file taking the Metatype Service descriptors. This
     * file will be created in the <i>OSGI-INF/metatype</i> directory below the {@link #setOutputDirectory(File) output directory}
     * .
     * <p>
     * This file will be overwritten if already existing. If no descriptors are created the file is actually removed.
     * <p>
     * The default value of this property is <code>metatype.xml</code>.
     */
    public void setMetaTypeName(final String metaTypeName) {
        this.metaTypeName = metaTypeName;
    }

    public File getOutputDirectory() {
        return this.outputDirectory;
    }

    public String getMetaTypeName() {
        return this.metaTypeName;
    }

    public String getSCRName() {
        return this.scrName;
    }
}
