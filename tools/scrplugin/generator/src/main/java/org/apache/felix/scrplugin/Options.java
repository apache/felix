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

    /** Flag for generating accessor methods. */
    private boolean generateAccessors = true;

    /** Flag for strict mode. */
    private boolean strictMode = false;

    /** Map of global properties. */
    private Map<String, String> properties = Collections.emptyMap();

    /** The requested spec version. */
    private SpecVersion specVersion;

    /** The output directory for the generated files. */
    private File outputDirectory;

    /** Is this an incremental build? */
    private boolean incremental = false;

    /** Skip volatile check. */
    private boolean skipVolatileCheck = false;

    /**
     * @see #setGenerateAccessors(boolean)
     * @return Whether accessor methods should be generated.
     */
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
    public void setGenerateAccessors(final boolean generateAccessors) {
        this.generateAccessors = generateAccessors;
    }

    /**
     * @see #setStrictMode(boolean)
     * @return Whether strict mode should be used or not.
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Defines whether warnings should be considered as errors and thus cause
     * the generation process to fail.
     * <p>
     * The default value of this property is <code>false</code>.
     */
    public void setStrictMode(final boolean strictMode) {
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

    /**
     * @see #setSpecVersion(SpecVersion)
     * @return Return the requested spec version
     */
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
    public void setSpecVersion(final SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    /**
     * @see #setOutputDirectory(File)
     * @return The output directory for the generated files.
     */
    public File getOutputDirectory() {
        return this.outputDirectory;
    }

    /**
     * Sets the directory where the descriptor files will be created.
     * <p>
     * This field has no default value and this setter <b>must</b> called
     * before passing this object to {@link SCRDescriptorGenerator#setOptions(Options)}.
     */
    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    private static final String PARENT_NAME = "OSGI-INF";

    /**
     * Convenience method to get the meta type directory.
     */
    public File getMetaTypeDirectory() {
        final File parentDir = new File(this.getOutputDirectory(), PARENT_NAME);
        return new File(parentDir, "metatype");
    }

    /**
     * Convenience method to get the component descriptor directory.
     */
    public File getComponentDescriptorDirectory() {
        return new File(this.getOutputDirectory(), PARENT_NAME);
    }

    /**
     * Is this an incremental build
     */
    public boolean isIncremental() {
        return incremental;
    }

    /**
     * Set whether this is an incremental build
     */
    public void setIncremental(final boolean incremental) {
        this.incremental = incremental;
    }

    /**
     * Should the check for volatile fields be skipped?
     */
    public boolean isSkipVolatileCheck() {
        return skipVolatileCheck;
    }

    /**
     * Set whether the check should be skipped
     */
    public void setSkipVolatileCheck(final boolean skipVolatileCheck) {
        this.skipVolatileCheck = skipVolatileCheck;
    }
}
