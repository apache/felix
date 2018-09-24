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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * This is a list of maps. Each map is a configuration for a check.
     * The name of the check needs to be provided through the "name" property.
     * The mode can be changed with the "mode" property for just this check.
     */
    @Parameter
    protected Map<String, String>[] config;

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
        if ( bundle == null ) {
            throw new MojoExecutionException("This mojo requires the project artifact to be built to perform the check." +
                "Make sure to create your bundle before running this plugin.");
        }
        getLog().debug("Checking " + bundle);

        // configuration
        final Map<String, Map<String,String>> configurations = new HashMap<>();
        final Set<String> configNames = new HashSet<>();
        if ( config != null ) {
            for(final Map<String, String> c : config) {
                final String name = c.remove("name");
                if ( name == null ) {
                    throw new MojoExecutionException("Name needs to be specified for a check configuration.");
                }
                configNames.add(name);
                configurations.put(name, c);
            }
        }

        final Class<?>[] checkClasses = new Class<?>[] {ImportExportCheck.class, SCRCheck.class, ConsumerProviderTypeCheck.class};
        final Check[] checks = new Check[checkClasses.length];
        int i = 0;
        for(final Class<?> c : checkClasses) {
            try {
                checks[i] = (Check)c.newInstance();
                configNames.remove(checks[i].getName());
                i++;
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        if ( !configNames.isEmpty() ) {
            throw new MojoExecutionException("Configurations for unknown checks: " + configNames);
        }

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

                for(final Check check : checks) {

                    // extract configuration
                    Map<String, String> checkConfig = configurations.get(check.getName());
                    if ( checkConfig == null ) {
                        checkConfig = Collections.emptyMap();
                    }
                    final Map<String, String> cc = checkConfig;

                    getLog().debug("Configuration for " + check.getName() + " : " + cc);

                    final CheckResult result = new CheckResult();
                    result.mode = this.mode;
                    if ( cc.get("mode") != null ) {
                        result.mode = Mode.valueOf(cc.remove("mode"));
                    }

                    if ( result.mode != Mode.OFF ) {
                        getLog().debug("Executing " + check.getName() + "...");

                        check.check(new CheckContext() {

                            private final Map<String, String> conf = cc;

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
