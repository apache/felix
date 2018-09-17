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
package org.apache.felix.maven.osgicheck.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.felix.maven.osgicheck.impl.checks.ConsumerProviderTypeCheck;
import org.apache.felix.maven.osgicheck.impl.checks.ImportExportCheck;
import org.apache.felix.maven.osgicheck.impl.checks.SCRCheck;
import org.apache.felix.maven.osgicheck.impl.featureutil.ManifestUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.shared.utils.xml.Xpp3DomBuilder;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.osgi.framework.Constants;


/**
 * This plugin checks various aspects of your OSGi project like proper use
 * of versioning, best practices use of annotations for Declarative Services
 * and others.
 */
@Mojo(
        name = "check",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true
    )
public class CheckMojo extends AbstractMojo {

    public enum Mode {
        OFF,
        DEFAULT,
        STRICT,
        ERRORS_ONLY
    }

    /**
     * The mode to be used by each check. The value can be {@code OFF} to
     * disable this plugin, {@code DEFAULT} to print warnings and fail on
     * errors, {@code STRICT} to print warnings and fail on both errors and
     * warnings, or {@code ERRORS_ONLY} to only print errors and fail on errors.
     */
    @Parameter(defaultValue = "DEFAULT")
    protected Mode mode;

    /**
     * The configuration for the checks. This can be used to specify the mode
     * per plugin.
     * The configurations can be specified as a CDATA section with an XML
     * tree for each check. The root name of the tree is the name of the
     * check.
     */
    @Parameter
    protected String config;

    /**
     * The Maven project
     */
    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The archiver manager for unarchiving the jar
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * Check whether this is an OSGi project
     * @return {@code true} if it is a jar or a bundle
     */
    private boolean isOSGiProject() {
        if ( "bundle".equals(project.getPackaging()) || "jar".equals(project.getPackaging()) ) {
            return true;
        }
        return false;
    }

    /**
     * Return the file with the jar
     * @return The file
     */
    private File getBundle() {
        return this.project.getArtifact().getFile();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if ( isOSGiProject() ) {
            if ( mode != Mode.OFF ) {
                getLog().debug("Checking OSGi project...");
                doExecute();
            } else {
                getLog().debug("Skipping check...disabled by configuration");
            }
        } else {
            getLog().debug("Skipping check...not an OSGi project");
        }
    }

    private void doExecute() throws MojoExecutionException, MojoFailureException {
        final File bundle = this.getBundle();
        getLog().debug("Checking " + bundle);

        // configuration
        Xpp3Dom pomCfg = null;
        if ( config != null && !config.trim().isEmpty() ) {
            pomCfg = Xpp3DomBuilder.build(new StringReader("<c>" + config + "</c>"));
        }
        final Xpp3Dom configuration = pomCfg;

        final List<CheckResult> results = new ArrayList<>();

        try {
            final Manifest mf = ManifestUtil.getManifest(bundle);
            final String manifestVersion = mf.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION);
            if ( !"2".equals(manifestVersion) ) {
                throw new MojoExecutionException("Bundle manifestversion 2 not found in manifest (" +manifestVersion + ")");
            }
            File rootDir = null;
            try {
                rootDir = extract(bundle);

                final File dir = rootDir;

                final Class<?>[] checks = new Class<?>[] {ImportExportCheck.class, SCRCheck.class, ConsumerProviderTypeCheck.class};
                for(final Class<?> c : checks) {
                    // instantiate
                    final Check check = (Check)c.newInstance();

                    // extract configuration
                    final Map<String, String> config;
                    if ( configuration != null ) {
                        final Xpp3Dom cfg = configuration.getChild(check.getName());
                        if ( cfg != null ) {
                            final Map<String, String> map = new HashMap<>();
                            for(final Xpp3Dom child : cfg.getChildren()) {
                                map.put(child.getName(), child.getValue());
                            }
                            config = map;
                        } else {
                            config = Collections.emptyMap();
                        }
                    } else {
                        config = Collections.emptyMap();
                    }
                    getLog().debug("Configuration for " + check.getName() + " : " + config);

                    final CheckResult result = new CheckResult();
                    result.mode = this.mode;
                    if ( config.get("mode") != null ) {
                        result.mode = Mode.valueOf(config.get("mode"));
                    }

                    if ( result.mode != Mode.OFF ) {
                        getLog().debug("Executing " + check.getName() + "...");

                        check.check(new CheckContext() {

                            private final Map<String, String> conf = config;

                            @Override
                            public File getRootDir() {
                                return dir;
                            }

                            @Override
                            public Manifest getManifest() {
                                return mf;
                            }

                            @Override
                            public Map<String, String> getConfiguration() {
                                return conf;
                            }

                            @Override
                            public Log getLog() {
                                return CheckMojo.this.getLog();
                            }

                            @Override
                            public void reportWarning(String message) {
                                result.warnings.add(message);
                            }

                            @Override
                            public void reportError(String message) {
                                result.errors.add(message);
                            }

                        });
                        results.add(result);

                        getLog().debug("Finished " + check.getName() + "...");
                    } else {
                        getLog().debug("Skipping executing " + check.getName());
                    }
                }
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } finally {
                if ( rootDir != null ) {
                    FileUtils.deleteDirectory(rootDir);
                }
            }

        } catch ( final IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage(), ioe);
        }

        // print warnings from all checks were enabled
        for(final CheckResult result : results) {
            if ( result.mode != Mode.ERRORS_ONLY ) {
                for(final String msg : result.warnings) {
                    getLog().warn(msg);
                }
            }
        }

        // print all errors
        boolean hasErrors = false;
        for(final CheckResult result : results) {
            for(final String msg : result.errors) {
                hasErrors = true;
                getLog().error(msg);
            }
        }

        if ( hasErrors ) {
            throw new MojoExecutionException("Check detected errors. See log output for error messages.");
        }
        for(final CheckResult result : results) {
            if ( result.mode == Mode.STRICT && !result.warnings.isEmpty() ) {
                throw new MojoExecutionException("Check detected warnings and strict mode is enabled. See log output for warning messages.");
            }
        }
    }

    private File extract(final File file) throws IOException, MojoExecutionException {
        final File rootDir = Files.createTempDirectory("osgicheck").toFile();
        UnArchiver zipUnarchiver = null;
        try {
            zipUnarchiver = archiverManager.getUnArchiver(file);
        } catch (NoSuchArchiverException e) {
            // should not happen
            throw new MojoExecutionException("Impossible to unarchive the '"
                                             + file
                                             + "' file: "
                                             + e.getMessage());
        }
        zipUnarchiver.setDestDirectory(rootDir);
        zipUnarchiver.setSourceFile(file);
        zipUnarchiver.extract();

        return rootDir;
    }

    public static class CheckResult {

        public Mode mode;

        public final List<String> warnings = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
    }
}
