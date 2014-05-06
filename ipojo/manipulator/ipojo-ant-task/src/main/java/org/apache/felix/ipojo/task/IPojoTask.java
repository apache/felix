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
package org.apache.felix.ipojo.task;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.io.File;

/**
 * iPOJO Ant Task. This Ant task manipulates an input bundle.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class IPojoTask extends Task {

    /**
     * Metadata file.
     */
    private File m_metadata;

    /**
     * Input bundle.
     */
    private File m_input;

    /**
     * Output bundle.
     */
    private File m_output;

    /**
     * Input directory.
     */
    private File m_directory;

    /**
     * Input manifest.
     */
    private File m_manifest;

    /**
     * Flag describing if we need to ignore annotation of not.
     */
    private boolean m_ignoreAnnotations = false;


    /**
     * Flag describing if we need or not use local XSD files
     * (i.e. use the {@link org.apache.felix.ipojo.xml.parser.SchemaResolver} or not).
     * If <code>true</code> the local XSD are not used.
     */
    private boolean m_ignoreLocalXSD = false;

    /**
     * The classpath.
     */
    private Path m_classpath;

    /**
     * Set the metadata file.
     *
     * @param meta : the metadata file.
     */
    public void setMetadata(File meta) {
        m_metadata = meta;
    }

    /**
     * Set the manifest file.
     *
     * @param manifest : the manifest file.
     */
    public void setManifest(File manifest) {
        m_manifest = manifest;
    }

    /**
     * Set the input bundle.
     *
     * @param in : the input bundle
     */
    public void setInput(File in) {
        m_input = in;
    }

    /**
     * Set the input directory.
     *
     * @param dir : the input directory
     */
    public void setDir(File dir) {
        m_directory = dir;
    }

    /**
     * Set the output bundle.
     *
     * @param out : the output bundle
     */
    public void setOutput(File out) {
        m_output = out;
    }

    /**
     * Set if we need to ignore annotations or not.
     *
     * @param flag : true if we need to ignore annotations.
     */
    public void setIgnoreAnnotations(boolean flag) {
        m_ignoreAnnotations = flag;
    }

    /**
     * Set if we need to use embedded XSD files or not.
     *
     * @param flag : true if we need to ignore embedded XSD files.
     */
    public void setIgnoreEmbeddedSchemas(boolean flag) {
        m_ignoreLocalXSD = flag;
    }

    /**
     * Execute the Ant Task.
     *
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() {

        if (m_input == null && m_directory == null) {
            throw new BuildException("Neither input bundle nor directory specified");
        }

        if (m_input != null && !m_input.exists()) {
            throw new BuildException("The input bundle " + m_input.getAbsolutePath() + " does not exist");
        }

        if (m_directory != null && !m_directory.exists()) {
            throw new BuildException("The input directory " + m_directory.getAbsolutePath() + " does not exist");
        }
        if (m_directory != null && !m_directory.isDirectory()) {
            throw new BuildException("The input directory " + m_directory.getAbsolutePath() + " is not a directory");
        }


        if (m_input != null) {
            log("Input bundle file : " + m_input.getAbsolutePath());
        } else {
            log("Input directory : " + m_directory.getAbsolutePath());
        }

        if (m_manifest != null) {
            if (m_input != null) {
                throw new BuildException("The manifest location cannot be used when manipulating an existing bundle");
            }
            if (!m_manifest.exists()) {
                throw new BuildException("The manifest file " + m_manifest.getAbsolutePath() + " does not exist");
            }
        }

        // Get metadata file
        if (m_metadata == null) {
            m_metadata = new File("./metadata.xml");
            if (!m_metadata.exists()) {
                // Verify if annotations are ignored
                if (m_ignoreAnnotations) {
                    log("No metadata file found & annotations ignored : nothing to do");
                    return;
                } else {
                    log("No metadata file found - trying to use only annotations");
                    m_metadata = null;
                }
            } else {
                log("Metadata file : " + m_metadata.getAbsolutePath());
            }
        } else {
            // Metadata file is specified, check existence
            if (!m_metadata.exists()) {
                throw new BuildException("No metadata file found - the file " + m_metadata.getAbsolutePath() + " does not exist");
            } else {
                if (m_metadata.isDirectory()) {
                    log("Metadata directory : " + m_metadata.getAbsolutePath());
                } else {
                    log("Metadata file : " + m_metadata.getAbsolutePath());
                }
            }
        }

        initializeSaxDriver();


        log("Start manipulation");

        if (m_input != null) { // Prepare output file
            if (m_output == null) {
                m_output = new File("./_out.jar");
            }
            if (m_output.exists()) {
                boolean r = m_output.delete();
                if (!r) {
                    throw new BuildException("The file " + m_output.getAbsolutePath() + " cannot be deleted");
                }
            }
        }

        AntReporter reporter = new AntReporter(getProject());
        Pojoization pojo = new Pojoization(reporter);
        if (m_ignoreAnnotations) {
            pojo.disableAnnotationProcessing();
        }
        if (!m_ignoreLocalXSD) {
            pojo.setUseLocalXSD();
        }

        Path classpath = getClasspath();
        classpath.addJavaRuntime();

        // Adding the input jar or directory
        if (m_classpath == null) {
            m_classpath = createClasspath();
        }
        Path element = m_classpath.createPath();
        if (m_input != null) {
            element.setLocation(m_input.getAbsoluteFile());
        } else if (m_directory != null) {
            element.setLocation(m_directory.getAbsoluteFile());
        }
        m_classpath.add(element);

        ClassLoader loader = getProject().createClassLoader(getClasspath());
        if (m_input != null) {
            pojo.pojoization(m_input, m_output, m_metadata, loader);
        } else {
            pojo.directoryPojoization(m_directory, m_metadata, m_manifest, loader);
        }
        for (int i = 0; i < reporter.getWarnings().size(); i++) {
            log((String) reporter.getWarnings().get(i), Project.MSG_WARN);
        }
        if (reporter.getErrors().size() > 0) {
            throw new BuildException((String) reporter.getErrors().get(0));
        }

        if (m_input != null) {
            String out;
            if (m_output.getName().equals("_out.jar")) {
                if (m_input.delete()) {
                    if (!m_output.renameTo(m_input)) {
                        log("Cannot rename the output jar to " + m_input.getAbsolutePath(), Project.MSG_WARN);
                    }
                } else {
                    log("Cannot delete the input file : " + m_input.getAbsolutePath(), Project.MSG_WARN);
                }
                out = m_input.getAbsolutePath();
            } else {
                out = m_output.getAbsolutePath();
            }

            log("Bundle manipulation - SUCCESS");
            log("Output file : " + out);
        } else {
            log("Manipulation - SUCCESS");
            log("Output files : " + m_directory.getAbsolutePath());
            if (m_manifest != null) {
                log("Manifest : " + m_manifest.getAbsolutePath());
            }

        }

    }

    /**
     * If Ant runs with Java 1.4, we should use the embedded Xerces.
     * To achieve that, we set the org.xml.sax.driver property.
     * Otherwise, the JVM sets the org.xml.sax.driver property.
     */
    private void initializeSaxDriver() {
        String version = (String) System.getProperty("java.vm.version");
        if (version.startsWith("1.4")) {
            System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        }
    }

    /**
     * Set the classpath to be used for this packaging.
     *
     * @param classpath the classpath used for this packaging
     */
    public synchronized void setClasspath(Path classpath) {
        if (m_classpath == null) {
            m_classpath = createClasspath();
        }
        m_classpath.append(classpath);
    }

    /**
     * Creates a nested classpath element.
     *
     * @return classpath
     */
    public synchronized Path createClasspath() {
        if (m_classpath == null) {
            m_classpath = new Path(getProject());
        }
        return m_classpath.createPath();
    }

    /**
     * Adds to the classpath a reference to
     * a &lt;path&gt; defined elsewhere.
     *
     * @param pathRef the reference to add to the classpath
     */
    public void setClasspathRef(Reference pathRef) {
        createClasspath().setRefid(pathRef);
    }

    /**
     * Gets the classpath.
     *
     * @return the classpath
     */
    public Path getClasspath() {
        return m_classpath;
    }


}

