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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
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

    private static final String BUNDLE_PLUGIN_GROUP_ID = "org.apache.felix";
    private static final String BUNDLE_PLUGIN_ARTIFACT_ID = "maven-bundle-plugin";
    private static final String BUNDLE_PLUGIN_INSTRUCTIONS = "instructions";
    private static final String BUNDLE_PLUGIN_EXTENSION = "BNDExtension-";
    private static final String OSGI_CFG_RESOURCES = "Include-Resource";
    private static final String OSGI_CFG_COMPONENTS = "Service-Component";

    /**
     * The minimum SCR Annotation library version supported by this plugin. See
     * FELIX-2680 for full details.
     *
     * @see #checkAnnotationArtifact(Artifact)
     */
    private static final ArtifactVersion SCR_ANN_MIN_VERSION = new DefaultArtifactVersion(
            "1.9.0");

    /**
     * @parameter expression="${project.build.outputDirectory}"
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
     * Skip volatile check for fields.
     *
     * @parameter default-value="false"
     */
    private boolean skipVolatileCheck;

    /**
     * @component
     */
    private BuildContext buildContext;

    @Override
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
        options.setGenerateAccessors(generateAccessors);
        options.setStrictMode(strictMode);
        options.setProperties(properties);
        options.setSpecVersion(SpecVersion.fromName(specVersion));
        options.setIncremental(this.buildContext.isIncremental());
        options.setSkipVolatileCheck(this.skipVolatileCheck);

        if ( specVersion != null && options.getSpecVersion() == null ) {
            throw new MojoExecutionException("Unknown spec version specified: " + specVersion);
        }

        try {

            final SCRDescriptorGenerator generator = new SCRDescriptorGenerator(
                    scrLog);

            // setup from plugin configuration
            generator.setOptions(options);
            generator.setProject(project);

            this.removePossiblyStaleFiles(scanner.getSources(), options);

            final Result result = generator.execute();
            this.setServiceComponentHeader(options);

            if ( !this.updateProjectResources() ) {
                this.setIncludeResourceHeader(options);
            }
            this.cleanUpDeletedSources(scanner.getDeletedSources(), options);

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

            final File metaTypePropsFile = new File(mtDir, source.getClassName() + ".properties");
            getLog().debug("Deleting " + metaTypePropsFile + " ");
            deleted = metaTypePropsFile.delete();
            if ( deleted ) {
                buildContext.refresh(metaTypePropsFile);
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
     * We need the bundle plugin with a version higher than 2.5.0!
     */
    private Plugin getBundlePlugin() {
        Plugin bundlePlugin  = null;
        final List<Plugin> plugins = this.project.getBuildPlugins();
        for(final Plugin p : plugins) {
            if ( p.getArtifactId().equals(BUNDLE_PLUGIN_ARTIFACT_ID)
                 && p.getGroupId().equals(BUNDLE_PLUGIN_GROUP_ID) ) {
                final ArtifactVersion pluginVersion = new DefaultArtifactVersion(p.getVersion());
                final ArtifactVersion requiredMinVersion = new DefaultArtifactVersion("2.5.0");
                if ( pluginVersion.compareTo(requiredMinVersion) > 0 ) {
                    bundlePlugin = p;
                    break;
                }
            }
        }
        return bundlePlugin;

    }

    private String getBundlePluginConfiguration(final String key) {
        String value = null;
        Plugin bundlePlugin = this.getBundlePlugin();
        if ( bundlePlugin != null ) {
            final Xpp3Dom config = (Xpp3Dom) bundlePlugin.getConfiguration();
            if ( config != null) {
                final Xpp3Dom instructionsConfig = config.getChild(BUNDLE_PLUGIN_INSTRUCTIONS);
                if ( instructionsConfig != null) {
                    final Xpp3Dom keyConfig = instructionsConfig.getChild(key);
                    if ( keyConfig != null ) {
                        return keyConfig.getValue();
                    }
                }
            }
        }
        return value;
    }

    /**
     * Set the service component header based on the files in the output directory
     */
    private void setServiceComponentHeader(final Options options) {
        final int outputDirLength = options.getOutputDirectory().getAbsolutePath().length() +1;
        final File componentDir = options.getComponentDescriptorDirectory();
        final String cmpPrefix = componentDir.getAbsolutePath().substring(outputDirLength).replace(File.separatorChar, '/');
        if ( componentDir.exists() ) {
            final Set<String> xmlFiles = new HashSet<String>();

            for(final File f : componentDir.listFiles()) {
                if ( f.isFile() && f.getName().endsWith(".xml") ) {
                    final String entry = cmpPrefix + '/' + f.getName();
                    xmlFiles.add(entry);
                }
            }

            final StringBuilder sb = new StringBuilder();
            for(final String entry : xmlFiles) {
                if ( sb.length() > 0 ) {
                    sb.append(", ");
                }
                sb.append(entry);
            }

            if ( sb.length() > 0 ) {
                if ( this.getBundlePlugin() != null ) {
                    project.getProperties().setProperty(BUNDLE_PLUGIN_EXTENSION + OSGI_CFG_COMPONENTS, sb.toString());
                } else {
                    project.getProperties().setProperty(OSGI_CFG_COMPONENTS, sb.toString());
                }
            }
        }
    }

    /**
     * Set the include resource header for bnd or as project properties
     * @param options
     */
    private void setIncludeResourceHeader(final Options options) {
        final int outputDirLength = options.getOutputDirectory().getAbsolutePath().length() +1;

        // make sure to either include the current settings or the default
        final StringBuilder resourcesEntry = new StringBuilder();
        final String includeResources = this.getBundlePluginConfiguration(OSGI_CFG_RESOURCES);
        if ( includeResources == null ) {
            resourcesEntry.append("{maven-resources}");
        }

        // process components
        final File componentDir = options.getComponentDescriptorDirectory();
        final String cmpPrefix = componentDir.getAbsolutePath().substring(outputDirLength).replace(File.separatorChar, '/');
        if ( componentDir.exists() ) {
            for(final File f : componentDir.listFiles()) {
                if ( f.isFile() && f.getName().endsWith(".xml") ) {
                    final String entry = cmpPrefix + '/' + f.getName();

                    if ( resourcesEntry.length() > 0 ) {
                        resourcesEntry.append(",");
                    }
                    resourcesEntry.append(entry);
                    resourcesEntry.append("=");
                    resourcesEntry.append(this.outputDirectory);
                    resourcesEntry.append(File.separatorChar);
                    resourcesEntry.append(entry);
                }
            }
        }
        // process metatype
        final File mtDir = options.getMetaTypeDirectory();
        final String mtPrefix = mtDir.getAbsolutePath().substring(outputDirLength).replace(File.separatorChar, '/');
        if ( mtDir.exists() ) {
            for(final File f : mtDir.listFiles()) {
                if ( f.isFile() && (f.getName().endsWith(".xml") || f.getName().endsWith(".properties")) ) {
                    final String entry = mtPrefix + '/' + f.getName();

                    if ( resourcesEntry.length() > 0 ) {
                        resourcesEntry.append(",");
                    }
                    resourcesEntry.append(entry);
                    resourcesEntry.append("=");
                    resourcesEntry.append(this.outputDirectory);
                    resourcesEntry.append(File.separatorChar);
                    resourcesEntry.append(entry);
                }
            }
        }

        if ( resourcesEntry.length() > 0 ) {
            if ( this.getBundlePlugin() != null ) {
                project.getProperties().setProperty(BUNDLE_PLUGIN_EXTENSION + OSGI_CFG_RESOURCES, resourcesEntry.toString());
            } else {
                project.getProperties().setProperty(OSGI_CFG_RESOURCES, resourcesEntry.toString());
            }
        }
    }

    /**
     * Update the Maven project resources if not target/classes (or the
     * configured build output directory) is used for output
     */
    private boolean updateProjectResources() {
        final String classesDir = new File(this.project.getBuild().getOutputDirectory()).getAbsolutePath().replace(File.separatorChar, '/');
        final String ourRsrcPath = this.outputDirectory.getAbsolutePath().replace(File.separatorChar, '/');
        if ( !classesDir.equals(ourRsrcPath) ) {
            // now add the descriptor directory to the maven resources
            boolean found = false;
            @SuppressWarnings("unchecked")
            final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
            while (!found && rsrcIterator.hasNext()) {
                final Resource rsrc = rsrcIterator.next();
                found = new File(rsrc.getDirectory()).getAbsolutePath().replace(File.separatorChar, '/').equals(ourRsrcPath);
            }
            if (!found) {
                final Resource resource = new Resource();
                resource.setDirectory(this.outputDirectory.getAbsolutePath());
                this.project.addResource(resource);
            }
            return true;
        }
        return false;
    }
}
