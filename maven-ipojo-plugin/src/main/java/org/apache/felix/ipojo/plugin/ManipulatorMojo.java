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
package org.apache.felix.ipojo.plugin;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.util.Classpath;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Packages an OSGi jar "bundle" as an "iPOJO bundle".
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @version $Rev$, $Date$
 * @goal ipojo-bundle
 * @phase package
 * @requiresDependencyResolution test
 * @description manipulate an OSGi bundle jar to build an iPOJO bundle
 * @threadSafe
 */
public class ManipulatorMojo extends AbstractMojo {

    /**
     * The directory for the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String m_buildDirectory;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File m_outputDirectory;

    /**
     * Location of the metadata file or iPOJO metadata configuration.
     *
     * @parameter alias="metadata"
     */
    private String m_metadata;

    /**
     * If set, the manipulated jar will be attached to the project as a separate artifact.
     *
     * @parameter alias="classifier" expression="${ipojo.classifier}"
     */
    private String m_classifier;

    /**
     * If set, select the manipulated artifact using this classifier.
     *
     * @parameter alias="input-classifier"
     */
    private String m_inputClassifier;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject m_project;

    /**
     * Used for attaching new artifacts.
     *
     * @component
     * @required
     */
    private MavenProjectHelper m_helper;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    private List<String> m_supportedProjectTypes = Arrays.asList(new String[]{"bundle", "jar", "war"});

    /**
     * Ignore annotations parameter.
     *
     * @parameter alias="ignoreAnnotations" default-value="false"
     */
    private boolean m_ignoreAnnotations;

    /**
     * Ignore embedded XSD parameter.
     *
     * @parameter alias="IgnoreEmbeddedSchemas" default-value="false"
     */
    private boolean m_ignoreEmbeddedXSD;

    private boolean isXML() {
        return m_metadata != null && (m_metadata.indexOf('<') > -1);
    }

    /**
     * Execute method : this method launches the pojoization.
     *
     * @throws MojoExecutionException : an exception occurs during the manipulation.
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException {
        // ignore project types not supported, useful when the plugin is configured in the parent pom
        if (!this.m_supportedProjectTypes.contains(m_project.getArtifact().getType())) {
            this.getLog().debug("Ignoring project "
                    + m_project.getArtifact() + " : type " + m_project.getArtifact().getType()
                    + " is not supported by iPOJO plugin, supported types are " + this.m_supportedProjectTypes);
            return;
        }

        initializeSaxDriver();

        getLog().info("Start bundle manipulation");

        // Get metadata
        // Check if metadata are contained in the configuration
        File metadata = null; // Metadata File or directory containing the metadata files.
        InputStream is = null; //Use if contained in the configuration

        // Create the ClassPath and classloader.
        Set<Artifact> artifacts = m_project.getArtifacts();
        Set<String> urls = new LinkedHashSet<String>();
        File classes = new File(m_project.getBasedir(), "target/classes");
        if (classes.isDirectory()) {
            urls.add(classes.getAbsolutePath());
        }
        for (Artifact artifact : artifacts) {
            File file = artifact.getFile();
            if (file != null && file.isFile()) {
                urls.add(file.getAbsolutePath());
            }
        }
        getLog().debug("Compute classpath: " + urls);
        Classpath classpath = new Classpath(urls);

        if (isXML()) {
            is = new ByteArrayInputStream(m_metadata.getBytes());
        } else {
            // If the metadata is not set,
            // first check if ./src/main/ipojo exists, if so look into it.
            if (m_metadata == null) {
                File m = new File(m_project.getBasedir(), "src/main/ipojo");
                if (m.isDirectory()) {
                    metadata = m;
                    getLog().info("Metadata directory : " + metadata.getAbsolutePath());
                } else {
                    // Else check target/classes/metadata.xml
                    File meta = new File(m_outputDirectory + File.separator + "metadata.xml");
                    if (!meta.exists()) {
                        // If it still does not exist, try ./metadata.xml
                        meta = new File(m_project.getBasedir() + File.separator + "metadata.xml");
                    }

                    if (meta.exists()) {
                        metadata = meta;
                        getLog().info("Metadata file : " + metadata.getAbsolutePath());
                    }

                    // No metadata.
                }
            } else {
                // metadata path set.
                File m = new File(m_project.getBasedir(), m_metadata);
                if (!m.exists()) {
                    throw new MojoExecutionException("The metadata file does not exist : " + m.getAbsolutePath());
                }
                metadata = m;
                if (m.isDirectory()) {
                    getLog().info("Metadata directory : " + metadata.getAbsolutePath());
                } else {
                    getLog().info("Metadata file : " + metadata.getAbsolutePath());
                }
            }

            if (metadata == null) {
                // Verify if annotations are ignored
                if (m_ignoreAnnotations) {
                    getLog().info("No metadata file found - ignoring annotations");
                    return;
                } else {
                    getLog().info("No metadata file found - trying to use only annotations");
                }
            }
        }

        // Get input bundle, we use the already create artifact.
        File in = null;
        if (m_inputClassifier == null) {
            in = m_project.getArtifact().getFile();
            getLog().info("Input Bundle File : " + in.getAbsolutePath());
            if (!in.exists()) {
                throw new MojoExecutionException("The specified bundle file does not exist : " + in.getAbsolutePath());
            }
        } else {
            // Look from attached artifacts.
            @SuppressWarnings("unchecked")
            List<Artifact> attached = m_project.getAttachedArtifacts();
            for (int i = 0; in == null && attached != null && i < attached.size(); i++) {
                Artifact artifact = attached.get(i);
                if (artifact.hasClassifier() && m_inputClassifier.equals(artifact.getClassifier())) {
                    in = artifact.getFile();
                }
            }

            if (in == null) {
                throw new MojoExecutionException("Cannot find the file to manipulate, " +
                        "no attached artifact with classifier " + m_inputClassifier);
            }

            getLog().info("Input Bundle File : " + in.getAbsolutePath());
            if (!in.exists()) {
                throw new MojoExecutionException("The specified bundle file does not exist : " + in.getAbsolutePath());
            }
        }

        File out = new File(m_buildDirectory + File.separator + "_out.jar");

        Reporter reporter = new MavenReporter(getLog());
        Pojoization pojo = new Pojoization(reporter);
        if (m_ignoreAnnotations) {
            pojo.disableAnnotationProcessing();
        }
        if (!m_ignoreEmbeddedXSD) {
            pojo.setUseLocalXSD();
        }

        // Executes the pojoization.
        if (is == null) {
            if (metadata == null) { // No metadata.
                pojo.pojoization(in, out, (File) null, classpath.createClassLoader()); // Only annotations
            } else {
                pojo.pojoization(in, out, metadata, classpath.createClassLoader()); // Metadata set
            }
        } else { // In-Pom metadata.
            pojo.pojoization(in, out, is, classpath.createClassLoader());
        }

        for (int i = 0; i < reporter.getWarnings().size(); i++) {
            getLog().warn((String) reporter.getWarnings().get(i));
        }
        if (reporter.getErrors().size() > 0) {
            throw new MojoExecutionException((String) reporter.getErrors().get(0));
        }

        if (m_classifier != null) {
            // The user want to attach the resulting jar
            // Do not delete in File
            m_helper.attachArtifact(m_project, "jar", m_classifier, out);
        } else {
            // Usual behavior
            if (in.delete()) {
                if (!out.renameTo(in)) {
                    getLog().warn("Cannot rename the manipulated jar file");
                }
            } else {
                getLog().warn("Cannot delete the input jar file");
            }
        }
        getLog().info("Bundle manipulation - SUCCESS");
    }

    /**
     * If Maven runs with Java 1.4, we should use the Maven Xerces.
     * To achieve that, we set the org.xml.sax.driver property.
     * Otherwise, the JVM sets the org.xml.sax.driver property.
     */
    private void initializeSaxDriver() {
        String version = (String) System.getProperty("java.vm.version");
        if (version.startsWith("1.4")) {
            getLog().info("Set the Sax driver to org.apache.xerces.parsers.SAXParser");
            System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        }
    }

}
