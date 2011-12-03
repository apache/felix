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
package org.apache.felix.ipojo.manipulator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.felix.ipojo.manipulator.manifest.FileManifestProvider;
import org.apache.felix.ipojo.manipulator.metadata.AnnotationMetadataProvider;
import org.apache.felix.ipojo.manipulator.metadata.CompositeMetadataProvider;
import org.apache.felix.ipojo.manipulator.metadata.EmptyMetadataProvider;
import org.apache.felix.ipojo.manipulator.metadata.FileMetadataProvider;
import org.apache.felix.ipojo.manipulator.metadata.StreamMetadataProvider;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.reporter.SystemReporter;
import org.apache.felix.ipojo.manipulator.store.DirectoryResourceStore;
import org.apache.felix.ipojo.manipulator.store.JarFileResourceStore;
import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;
import org.apache.felix.ipojo.manipulator.store.mapper.WABResourceMapper;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.manipulator.visitor.check.CheckFieldConsistencyVisitor;
import org.apache.felix.ipojo.manipulator.visitor.writer.ManipulatedResourcesWriter;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.SchemaResolver;

/**
 * Pojoization allows creating an iPOJO bundle from a "normal" bundle.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Pojoization {

    /**
     * iPOJO Imported Package Version.
     */
    public static final String IPOJO_PACKAGE_VERSION = " 1.8.0";

    /**
     * Flag describing if we need or not compute annotations.
     * By default, compute the annotations.
     */
    private boolean m_ignoreAnnotations;

    /**
     * Flag describing if we need or not use local XSD files
     * (i.e. use the {@link SchemaResolver} or not).
     * If <code>true</code> the local XSD are used (default to {@literal false}).
     */
    private boolean m_useLocalXSD = false;

    /**
     * Reporter for error reporting.
     */
    private Reporter m_reporter;

    public Pojoization() {
        this(new SystemReporter());
    }

    public Pojoization(Reporter reporter) {
        m_reporter = reporter;
    }

    /**
     * Activates annotation processing.
     */
    public void disableAnnotationProcessing() {
        m_ignoreAnnotations = true;
    }

    /**
     * Activates the entity resolver loading
     * XSD files from the classloader.
     */
    public void setUseLocalXSD() {
        m_useLocalXSD = true;
    }

    /**
     * @return all the errors (fatal) reported by the manipulation process.
     */
    public List<String> getErrors() {
        // Simple delegation for backward compatibility
        return m_reporter.getErrors();
    }

    /**
     * @return all the warnings (non fatal) reported by the manipulation process.
     */
    public List<String> getWarnings() {
        // Simple delegation for backward compatibility
        return m_reporter.getWarnings();
    }


    /**
     * Manipulates an input bundle.
     * This method creates an iPOJO bundle based on the given metadata file.
     * The original and final bundles must be different.
     * @param in the original bundle.
     * @param out the final bundle.
     * @param metadata the iPOJO metadata input stream.
     */
    public void pojoization(File in, File out, InputStream metadata) {

        StreamMetadataProvider provider = new StreamMetadataProvider(metadata, m_reporter);
        provider.setValidateUsingLocalSchemas(m_useLocalXSD);

        ResourceStore store;
        try {
            JarFile origin = new JarFile(in);
            JarFileResourceStore jfrs = new JarFileResourceStore(origin, out);
            if (in.getName().endsWith(".war")) {
                // this is a war file, use the right mapper
                jfrs.setResourceMapper(new WABResourceMapper());
            }
            jfrs.setManifest(origin.getManifest());
            DefaultManifestBuilder dmb = new DefaultManifestBuilder();
            dmb.setMetadataRenderer(new MetadataRenderer());
            jfrs.setManifestBuilder(dmb);
            store = jfrs;
        } catch (IOException e) {
            m_reporter.error("The input file " + in.getAbsolutePath() + " is not a Jar file");
            return;
        }

        ManipulationVisitor visitor = createDefaultVisitorChain(store);

        pojoization(store, provider, visitor);
    }

    private ManipulationVisitor createDefaultVisitorChain(ResourceStore store) {
        ManipulatedResourcesWriter writer = new ManipulatedResourcesWriter();
        writer.setReporter(m_reporter);
        writer.setResourceStore(store);

        CheckFieldConsistencyVisitor checkFieldConsistencyVisitor = new CheckFieldConsistencyVisitor(writer);
        checkFieldConsistencyVisitor.setReporter(m_reporter);
        return checkFieldConsistencyVisitor;
    }

    /**
     * Manipulates an input bundle.
     * This method creates an iPOJO bundle based on the given metadata file.
     * The original and final bundles must be different.
     * @param in the original bundle.
     * @param out the final bundle.
     * @param metadataFile the iPOJO metadata file (XML).
     */
    public void pojoization(File in, File out, File metadataFile) {

        MetadataProvider provider = new EmptyMetadataProvider();
        if (metadataFile != null) {
            FileMetadataProvider fileMetadataProvider = new FileMetadataProvider(metadataFile, m_reporter);
            fileMetadataProvider.setValidateUsingLocalSchemas(m_useLocalXSD);
            provider = fileMetadataProvider;
        }

        ResourceStore store;
        try {
            JarFile origin = new JarFile(in);
            JarFileResourceStore jfrs = new JarFileResourceStore(origin, out);
            if (in.getName().endsWith(".war")) {
                // this is a war file, use the right mapper
                jfrs.setResourceMapper(new WABResourceMapper());
            }
            jfrs.setManifest(origin.getManifest());
            DefaultManifestBuilder dmb = new DefaultManifestBuilder();
            dmb.setMetadataRenderer(new MetadataRenderer());
            jfrs.setManifestBuilder(dmb);
            store = jfrs;
        } catch (IOException e) {
            m_reporter.error("The input file " + in.getAbsolutePath() + " is not a Jar file");
            return;
        }

        ManipulationVisitor visitor = createDefaultVisitorChain(store);

        pojoization(store, provider, visitor);
    }

    /**
     * Manipulates an expanded bundles.
     * Classes are in the specified directory.
     * this method allows to update a customized manifest.
     * @param directory the directory containing classes
     * @param metadataFile the metadata file
     * @param manifestFile the manifest file. <code>null</code> to use directory/META-INF/MANIFEST.mf
     */
    public void directoryPojoization(File directory, File metadataFile, File manifestFile) {
        // Get the metadata.xml location if not null
        MetadataProvider provider = new EmptyMetadataProvider();
        if (metadataFile != null) {
            FileMetadataProvider fileMetadataProvider = new FileMetadataProvider(metadataFile, m_reporter);
            fileMetadataProvider.setValidateUsingLocalSchemas(m_useLocalXSD);
            provider = fileMetadataProvider;
        }

        ManifestProvider manifestProvider;
        if (manifestFile != null) {
            if (manifestFile.isFile()) {
                try {
                    manifestProvider = new FileManifestProvider(manifestFile);
                } catch (IOException e) {
                    m_reporter.error("Cannot read Manifest from '" + manifestFile.getAbsolutePath() + "'");
                    return;
                }
            } else {
                m_reporter.error("The manifest file " + manifestFile.getAbsolutePath() + " does not exist");
                return;
            }
        } else {
            // If the manifest is not specified, the m_dir/META-INF/MANIFEST.MF is used.
            File metaInf = new File(directory, "META-INF");
            File original = new File(metaInf, "MANIFEST.MF");
            if (original.isFile()) {
                try {
                    manifestProvider = new FileManifestProvider(original);
                } catch (IOException e) {
                    m_reporter.error("Cannot read Manifest from '" + original.getAbsolutePath() + "'");
                    return;
                }
            } else {
                m_reporter.error("The manifest file " + original.getAbsolutePath() + " does not exist");
                return;
            }
        }

        DirectoryResourceStore store;
        if (directory.exists() && directory.isDirectory()) {
            store = new DirectoryResourceStore(directory);
            File webinf = new File(directory, "WEB-INF");
            if (directory.getName().endsWith(".war") || webinf.isDirectory()) {
                // this is a war file, use the right mapper
                store.setResourceMapper(new WABResourceMapper());
            }
            store.setManifest(manifestProvider.getManifest());
            DefaultManifestBuilder dmb = new DefaultManifestBuilder();
            dmb.setMetadataRenderer(new MetadataRenderer());
            store.setManifestBuilder(dmb);
        } else {
            m_reporter.error("The directory " + directory.getAbsolutePath() + " does not exist or is not a directory.");
            return;
        }

        ManipulationVisitor visitor = createDefaultVisitorChain(store);

        pojoization(store, provider, visitor);

    }

    public void pojoization(final ResourceStore store,
                            final MetadataProvider metadata,
                            final ManipulationVisitor visitor) {

        ManipulationEngine engine = new ManipulationEngine();
        engine.setResourceStore(store);
        engine.setReporter(m_reporter);
        engine.setManipulationVisitor(visitor);

        try {

            // Creates a composite to store multiple metadata providers
            CompositeMetadataProvider composite = new CompositeMetadataProvider(m_reporter);
            composite.addMetadataProvider(metadata);

            if (!m_ignoreAnnotations) {
                composite.addMetadataProvider(new AnnotationMetadataProvider(store, m_reporter));
            }

            // Get metadata
            List<Element> metadatas = composite.getMetadatas();

            // Construct ManipulationUnits
            // And collect non-component metadata
            for (Element meta : metadatas) {
                String name = Metadatas.getComponentType(meta);
                if (name != null) {
                    // Only handler and component have a classname attribute
                    String path = Strings.asResourcePath(name);
                    engine.addManipulationUnit(new ManipulationUnit(path, meta));
                } else {
                    visitor.visitMetadata(meta);
                }
            }

        } catch (IOException e) {
            m_reporter.error("Cannot load metadata " + e.getMessage());
            return;
        }

        // Start the manipulation
        engine.generate();

        // Tell the visitor that we have finished
        visitor.visitEnd();

    }
}

