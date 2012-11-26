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
package org.apache.felix.scrplugin.mojo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.Project;
import org.apache.felix.scrplugin.Result;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.SCRDescriptorGenerator;
import org.apache.felix.scrplugin.Source;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * The <code>SCRDescriptorMojo</code> generates a service descriptor file based
 * on annotations found in the sources.
 *
 * @goal scr
 * @phase process-classes
 * @threadSafe
 * @description Build Service Descriptors from Java Source
 * @requiresDependencyResolution compile
 */
public class SCRDescriptorMojo extends AbstractMojo {

    /**
     * The groupID of the SCR Annotation library
     *
     * @see #SCR_ANN_MIN_VERSION
     * @see #checkAnnotationArtifact(Artifact)
     */
    private static final String SCR_ANN_GROUPID = "org.apache.felix";

    /**
     * The artifactID of the SCR Annotation library.
     *
     * @see #SCR_ANN_MIN_VERSION
     * @see #checkAnnotationArtifact(Artifact)
     */
    private static final String SCR_ANN_ARTIFACTID = "org.apache.felix.scr.annotations";

    /**
     * The minimum SCR Annotation library version supported by this plugin. See
     * FELIX-2680 for full details.
     *
     * @see #checkAnnotationArtifact(Artifact)
     */
    private static final ArtifactVersion SCR_ANN_MIN_VERSION = new DefaultArtifactVersion(
            "1.6.9");

    /**
     * @parameter expression="${project.build.directory}/scr-plugin-generated"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Name of the generated descriptor.
     *
     * @parameter expression="${scr.descriptor.name}"
     *            default-value="serviceComponents.xml"
     */
    private String finalName;

    /**
     * Name of the generated meta type file.
     *
     * @parameter default-value="metatype.xml"
     */
    private String metaTypeName;

    /**
     * This flag controls the generation of the bind/unbind methods.
     *
     * @parameter default-value="true"
     */
    private boolean generateAccessors;

    /**
     * In strict mode the plugin even fails on warnings.
     *
     * @parameter default-value="false"
     */
    private boolean strictMode;

    /**
     * If set to false, a single descriptor will be created.
     *
     * @parameter default-value="true"
     */
    private boolean generateSeparateDescriptors;

    /**
     * The comma separated list of tokens to include when processing sources.
     *
     * @parameter alias="includes"
     */
    private String sourceIncludes;

    /**
     * The comma separated list of tokens to exclude when processing sources.
     *
     * @parameter alias="excludes"
     */
    private String sourceExcludes;

    /**
     * Predefined properties.
     *
     * @parameter
     */
    private Map<String, String> properties = new LinkedHashMap<String, String>();

    /**
     * The version of the DS spec this plugin generates a descriptor for. By
     * default the version is detected by the used tags.
     *
     * @parameter
     */
    private String specVersion;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    private List<String> supportedProjectTypes = Arrays.asList( new String[]
            { "jar", "bundle" } );

    /**
     * By default the plugin scans the java source tree, if this is set to true,
     * the generated classes directory is scanned instead.
     *
     * @parameter default-value="false"
     */
    private boolean scanClasses;

    /**
     * @component
     */
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final String projectType = project.getArtifact().getType();

        // ignore unsupported project types, useful when bundleplugin is configured in parent pom
        if ( !supportedProjectTypes.contains( projectType ) ) {
            getLog().debug(
                    "Ignoring project type " + projectType + " - supportedProjectTypes = " + supportedProjectTypes );
            return;
        }

        // create the log for the generator
        final org.apache.felix.scrplugin.Log scrLog = new MavenLog(getLog(), buildContext);

        // create project
        final MavenProjectScanner scanner = new MavenProjectScanner(
                this.buildContext,
                this.project, this.sourceIncludes, this.sourceExcludes, this.scanClasses, scrLog);

        final Project project = new Project();
        // create the class loader
        project.setClassLoader(new URLClassLoader(getClassPath(), this
                .getClass().getClassLoader()));
        project.setDependencies(scanner.getDependencies());
        project.setSources(scanner.getSources());
        project.setClassesDirectory(this.project.getBuild().getOutputDirectory());

        // create options
        final Options options = new Options();
        options.setOutputDirectory(outputDirectory);
        options.setSCRName(finalName);
        options.setMetaTypeName(metaTypeName);
        options.setGenerateAccessors(generateAccessors);
        options.setStrictMode(strictMode);
        options.setProperties(properties);
        options.setSpecVersion(SpecVersion.fromName(specVersion));
        options.setGenerateSeparateDescriptors(this.generateSeparateDescriptors);

        if ( specVersion != null && options.getSpecVersion() == null ) {
            throw new MojoExecutionException("Unknown spec version specified: " + specVersion);
        }

        try {

            final SCRDescriptorGenerator generator = new SCRDescriptorGenerator(
                    scrLog);

            // setup from plugin configuration
            generator.setOptions(options);
            generator.setProject(project);

            // don't try to delete per-class descriptors if only one descriptor is generated
            if ( options.isGenerateSeparateDescriptors() ) {
                this.removePossiblyStaleFiles(scanner.getSources(), options);
            }

            final Result result = generator.execute();
            this.setServiceComponentHeader(result.getScrFiles());

            this.updateProjectResources();
            // don't try to delete per-class descriptors if only one descriptor is generated
            if ( options.isGenerateSeparateDescriptors() ) {
                this.cleanUpDeletedSources(scanner.getDeletedSources(), options);
            }

            this.refreshMessages(result.getProcessedSourceFiles());
            this.updateBuildContext(result);

        } catch (final SCRDescriptorException sde) {
            throw new MojoExecutionException(sde.getSourceLocation() + " : " + sde.getMessage(), sde);
        } catch (final SCRDescriptorFailureException sdfe) {
            throw (MojoFailureException) new MojoFailureException(
                    sdfe.getMessage()).initCause(sdfe);
        }
    }

    /**
     * Remove existing files for the sources which have recently changed
     *
     * <p>This method ensures that files which were generated in a previous run are not
     * leftover if the source file has changed by:
     * <ol>
     *  <li>No longer having a <tt>@Component</tt>annotation</li>
     *  <li>No longer having the <tt>metatype</tt> property set to true</li>
     * </ol>
     * </p>
     *
     * @param sources the changed source files
     */
    private void removePossiblyStaleFiles(final Collection<Source> sources, final Options options) {
        deleteOutputFilesForSources(sources, options);
    }

    /**
     * @param scrFiles
     */
    private void refreshMessages(List<String> scrFiles) {
        for ( final String scrFile : scrFiles ) {
            buildContext.removeMessages(new File(scrFile));
        }
    }

    private void cleanUpDeletedSources(final Collection<Source> deletedSources, final Options options) {
        deleteOutputFilesForSources(deletedSources, options);
    }

    private void deleteOutputFilesForSources(final Collection<Source> sources, final Options options) {
        final File componentDir = options.getComponentDescriptorDirectory();
        final File mtDir = options.getMetaTypeDirectory();

        for ( final Source source : sources ) {

            final File metaTypeFile = new File(mtDir, source.getClassName() + ".xml");
            getLog().debug("Deleting " + metaTypeFile + " ");
            boolean deleted = metaTypeFile.delete();
            if ( deleted ) {
                buildContext.refresh(metaTypeFile);
            }

            final File componentDescriptorFile = new File(componentDir, source.getClassName() + ".xml");
            getLog().debug("Deleting " + componentDescriptorFile);
            deleted = componentDescriptorFile.delete();
            if ( deleted ) {
                buildContext.refresh(componentDescriptorFile);
            }
        }
    }

    private void updateBuildContext(final Result result) {

        if ( result.getMetatypeFiles() != null ) {
            for(final String name : result.getMetatypeFiles() ) {
                final File metaTypeFile = new File(this.outputDirectory, name.replace('/', File.separatorChar));
                getLog().debug("Refreshing " + metaTypeFile);
                this.buildContext.refresh(metaTypeFile);
            }
        }
        if ( result.getScrFiles() != null ) {
            for(final String name : result.getScrFiles() ) {
                final File scrFile = new File(this.outputDirectory, name.replace('/', File.separatorChar));
                getLog().debug("Refreshing " + scrFile);
                this.buildContext.refresh(scrFile);
            }
        }
    }

    private URL[] getClassPath() throws MojoFailureException {
        @SuppressWarnings("unchecked")
        final List<Artifact> artifacts = this.project.getCompileArtifacts();
        final ArrayList<URL> path = new ArrayList<URL>();

        try {
            path.add(new File(this.project.getBuild().getOutputDirectory())
            .toURI().toURL());
        } catch (final IOException ioe) {
            throw new MojoFailureException(
                    "Unable to add target directory to classloader.");
        }

        for (final Iterator<Artifact> ai = artifacts.iterator(); ai.hasNext();) {
            final Artifact a = ai.next();
            assertMinScrAnnotationArtifactVersion(a);
            try {
                path.add(a.getFile().toURI().toURL());
            } catch (IOException ioe) {
                throw new MojoFailureException(
                        "Unable to get compile class loader.");
            }
        }

        return path.toArray(new URL[path.size()]);
    }

    /**
     * Asserts that the artifact is at least version
     * {@link #SCR_ANN_MIN_VERSION} if it is
     * org.apache.felix:org.apache.felix.scr.annotations. If the version is
     * lower then the build fails because as of Maven SCR Plugin 1.6.0 the old
     * SCR Annotation libraries do not produce descriptors any more. If the
     * artifact is not this method silently returns.
     *
     * @param a
     *            The artifact to check and assert
     * @see #SCR_ANN_ARTIFACTID
     * @see #SCR_ANN_GROUPID
     * @see #SCR_ANN_MIN_VERSION
     * @throws MojoFailureException
     *             If the artifact refers to the SCR Annotation library with a
     *             version less than {@link #SCR_ANN_MIN_VERSION}
     */
    @SuppressWarnings("unchecked")
    private void assertMinScrAnnotationArtifactVersion(final Artifact a)
            throws MojoFailureException {
        if (SCR_ANN_ARTIFACTID.equals(a.getArtifactId())
                && SCR_ANN_GROUPID.equals(a.getGroupId())) {
            // assert minimal version number
            final ArtifactVersion aVersion = new DefaultArtifactVersion(a.getBaseVersion());
            if (SCR_ANN_MIN_VERSION.compareTo(aVersion) > 0) {
                getLog().error("Project depends on " + a);
                getLog().error(
                        "Minimum required version is " + SCR_ANN_MIN_VERSION);
                throw new MojoFailureException(
                        "Please use org.apache.felix:org.apache.felix.scr.annotations version "
                                + SCR_ANN_MIN_VERSION + " or newer.");
            }
        }
    }

    /**
     * Set the service component header based on the scr files.
     */
    private void setServiceComponentHeader(final List<String> files) {
        if ( files != null && files.size() > 0 ) {
            final String svcHeader = project.getProperties().getProperty("Service-Component");
            final Set<String> xmlFiles = new HashSet<String>();
            if ( svcHeader != null ) {
                final StringTokenizer st = new StringTokenizer(svcHeader, ",");
                while ( st.hasMoreTokens() ) {
                    final String token = st.nextToken();
                    xmlFiles.add(token.trim());
                }
            }

            for(final String path : files) {
                xmlFiles.add(path);
            }
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(final String entry : xmlFiles) {
                if ( !first ) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(entry);
            }
            project.getProperties().setProperty("Service-Component", sb.toString());
        }
    }

    /**
     * Update the Maven project resources.
     */
    private void updateProjectResources() {
        // now add the descriptor directory to the maven resources
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath();
        boolean found = false;
        @SuppressWarnings("unchecked")
        final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
        while (!found && rsrcIterator.hasNext()) {
            final Resource rsrc = rsrcIterator.next();
            found = rsrc.getDirectory().equals(ourRsrcPath);
        }
        if (!found) {
            final Resource resource = new Resource();
            resource.setDirectory(this.outputDirectory.getAbsolutePath());
            this.project.addResource(resource);
        }
    }
}
