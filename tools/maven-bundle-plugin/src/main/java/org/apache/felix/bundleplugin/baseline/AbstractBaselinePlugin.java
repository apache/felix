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
package org.apache.felix.bundleplugin.baseline;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.version.Version;
import aQute.service.reporter.Reporter;

/**
 * Abstract BND Baseline check between two bundles.
 */
abstract class AbstractBaselinePlugin
    extends AbstractMojo
{

    /**
     * Flag to easily skip execution.
     */
    @Parameter( property = "baseline.skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * Whether to fail on errors.
     */
    @Parameter( property = "baseline.failOnError", defaultValue = "true" )
    protected boolean failOnError;

    /**
     * Whether to fail on warnings.
     */
    @Parameter( property = "baseline.failOnWarning", defaultValue = "false" )
    protected boolean failOnWarning;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    @Parameter( defaultValue = "${project.build.directory}", readonly = true, required = true )
    private File buildDirectory;

    @Parameter( defaultValue = "${project.build.finalName}", readonly = true, required = true )
    private String finalName;

    @Component
    protected ArtifactResolver resolver;

    @Component
    protected ArtifactFactory factory;

    @Component
    private ArtifactMetadataSource metadataSource;

    /**
     * Group id to compare the current code against.
     */
    @Parameter( defaultValue = "${project.groupId}", property="comparisonGroupId" )
    protected String comparisonGroupId;

    /**
     * Artifact to compare the current code against.
     */
    @Parameter( defaultValue = "${project.artifactId}", property="comparisonArtifactId" )
    protected String comparisonArtifactId;

    /**
     * Version to compare the current code against.
     */
    @Parameter( defaultValue = "(,${project.version})", property="comparisonVersion" )
    protected String comparisonVersion;

    /**
     * Artifact to compare the current code against.
     */
    @Parameter( defaultValue = "${project.packaging}", property="comparisonPackaging" )
    protected String comparisonPackaging;

    /**
     * Classifier for the artifact to compare the current code against.
     */
    @Parameter( property="comparisonClassifier" )
    protected String comparisonClassifier;

    /**
     * A list of packages filter, if empty the whole bundle will be traversed. Values are specified in OSGi package
     * instructions notation, e.g. <code>!org.apache.felix.bundleplugin</code>.
     */
    @Parameter
    private String[] filters;

    /**
     * Project types which this plugin supports.
     */
    @Parameter
    protected List<String> supportedProjectTypes = Arrays.asList( new String[] { "jar", "bundle" } );

    @Component
    protected BuildContext buildContext;
    
    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        this.execute(null);
    }

    protected void execute( Object context)
            throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Skipping Baseline execution" );
            return;
        }

        if ( !supportedProjectTypes.contains( project.getArtifact().getType() ) )
        {
            getLog().info("Skipping Baseline (project type " + project.getArtifact().getType() + " not supported)");
            return;
        }

        // get the bundles that have to be compared

        final Jar currentBundle = getCurrentBundle();
        if ( currentBundle == null )
        {
            getLog().info( "Not generating Baseline report as there is no bundle generated by the project" );
            return;
        }

        final Artifact previousArtifact = getPreviousArtifact();

        final Jar previousBundle;
        if (previousArtifact != null)
        {
            previousBundle = openJar(previousArtifact.getFile());
        }
        else
        {
            previousBundle = null;
        }

        if ( previousBundle == null )
        {
            getLog().info( "Not generating Baseline report as there is no previous version of the library to compare against" );
            return;
        }

        // preparing the filters

        final Instructions packageFilters;
        if ( filters == null || filters.length == 0 )
        {
            packageFilters = new Instructions();
        }
        else
        {
            packageFilters = new Instructions( Arrays.asList( filters ) );
        }


        String generationDate = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm'Z'" ).format( new Date() );
        final Reporter reporter = new Processor();

        final Info[] infos;
        try
        {
            final Set<Info> infoSet = new Baseline( reporter, new DiffPluginImpl() )
                                .baseline( currentBundle, previousBundle, packageFilters );
            infos = infoSet.toArray( new Info[infoSet.size()] );
            Arrays.sort( infos, new InfoComparator() );
        }
        catch ( final Exception e )
        {
            throw new MojoExecutionException( "Impossible to calculate the baseline", e );
        }
        finally
        {
            closeJars( currentBundle, previousBundle );
        }

        try
        {
            // go!
            context = this.init(context);
            startBaseline( context, generationDate, project.getArtifactId(), project.getVersion(), previousArtifact.getVersion() );

            for ( final Info info : infos )
            {
                DiffMessage diffMessage = null;

                if ( info.suggestedVersion != null )
                {
                    if ( info.newerVersion.compareTo( info.suggestedVersion ) > 0 )
                    {
                        diffMessage = new DiffMessage( "Excessive version increase", DiffMessage.Type.warning );
                        reporter.warning( "%s: %s; detected %s, suggested %s",
                                          info.packageName, diffMessage, info.newerVersion, info.suggestedVersion );
                    }
                    else if ( info.newerVersion.compareTo( info.suggestedVersion ) < 0 )
                    {
                        diffMessage = new DiffMessage( "Version increase required", DiffMessage.Type.error );
                        reporter.error( "%s: %s; detected %s, suggested %s",
                                        info.packageName, diffMessage, info.newerVersion, info.suggestedVersion );
                    }
                }

                switch ( info.packageDiff.getDelta() )
                {
                    case UNCHANGED:
                        if ( info.newerVersion.compareTo( info.suggestedVersion ) != 0 )
                        {
                            diffMessage = new DiffMessage( "Version has been increased but analysis detected no changes", DiffMessage.Type.warning );
                            reporter.warning( "%s: %s; detected %s, suggested %s",
                                              info.packageName, diffMessage, info.newerVersion, info.suggestedVersion );
                        }
                        break;

                    case REMOVED:
                        diffMessage = new DiffMessage( "Package removed", DiffMessage.Type.info );
                        reporter.trace( "%s: %s ", info.packageName, diffMessage );
                        break;

                    case CHANGED:
                    case MICRO:
                    case MINOR:
                    case MAJOR:
                    case ADDED:
                    default:
                        // ok
                        break;
                }

                startPackage( context,
                              info.mismatch,
                              info.packageName,
                              getShortDelta( info.packageDiff.getDelta() ),
                              StringUtils.lowerCase( String.valueOf( info.packageDiff.getDelta() ) ),
                              info.newerVersion,
                              info.olderVersion,
                              info.suggestedVersion,
                              diffMessage,
                              info.attributes );

                if ( Delta.REMOVED != info.packageDiff.getDelta() )
                {
                    doPackageDiff( context, info.packageDiff );
                }

                endPackage(context);
            }

            endBaseline(context);
        }
        finally
        {
            this.close(context);
        }

        // check if it has to fail if some error has been detected

        boolean fail = false;

        if ( !reporter.isOk() )
        {
            for ( String errorMessage : reporter.getErrors() )
            {
                getLog().error( errorMessage );
            }

            if ( failOnError )
            {
                fail = true;
            }
        }

        // check if it has to fail if some warning has been detected

        if ( !reporter.getWarnings().isEmpty() )
        {
            for ( String warningMessage : reporter.getWarnings() )
            {
                getLog().warn( warningMessage );
            }

            if ( failOnWarning )
            {
                fail = true;
            }
        }

        getLog().info( String.format( "Baseline analysis complete, %s error(s), %s warning(s)",
                                      reporter.getErrors().size(),
                                      reporter.getWarnings().size() ) );

        if ( fail )
        {
            throw new MojoFailureException( "Baseline failed, see generated report" );
        }
    }

    private void doPackageDiff( Object context, Diff diff )
    {
        int depth = 1;

        for ( Diff curDiff : diff.getChildren() )
        {
            if ( Delta.UNCHANGED != curDiff.getDelta() )
            {
                doDiff( context, curDiff, depth );
            }
        }
    }

    private void doDiff( Object context, Diff diff, int depth )
    {
        String type = StringUtils.lowerCase( String.valueOf( diff.getType() ) );
        String shortDelta = getShortDelta( diff.getDelta() );
        String delta = StringUtils.lowerCase( String.valueOf( diff.getDelta() ) );
        String name = diff.getName();

        startDiff( context, depth, type, name, delta, shortDelta );

        for ( Diff curDiff : diff.getChildren() )
        {
            if ( Delta.UNCHANGED != curDiff.getDelta() )
            {
                doDiff( context, curDiff, depth + 1 );
            }
        }

        endDiff( context, depth );
    }

    // extensions APIs

    protected abstract Object init(final Object initialContext);

    protected abstract void close(final Object context);

    protected abstract void startBaseline( final Object context, String generationDate, String bundleName, String currentVersion, String previousVersion );

    protected abstract void startPackage( final Object context,
            boolean mismatch,
                                          String name,
                                          String shortDelta,
                                          String delta,
                                          Version newerVersion,
                                          Version olderVersion,
                                          Version suggestedVersion,
                                          DiffMessage diffMessage,
                                          Map<String,String> attributes );

    protected abstract void startDiff( final Object context,
                                       int depth,
                                       String type,
                                       String name,
                                       String delta,
                                       String shortDelta );

    protected abstract void endDiff( final Object context, int depth );

    protected abstract void endPackage(final Object context);

    protected abstract void endBaseline(final Object context);

    // internals

    private Jar getCurrentBundle()
        throws MojoExecutionException
    {
        /*
         * Resolving the aQute.bnd.osgi.Jar via the produced artifact rather than what is produced in the target/classes
         * directory would make the Mojo working also in projects where the bundle-plugin is used just to generate the
         * manifest file and the final jar is assembled via the jar-plugin
         */
        File currentBundle = new File( buildDirectory, getBundleName() );
        if ( !currentBundle.exists() )
        {
            getLog().debug( "Produced bundle not found: " + currentBundle );
            return null;
        }

        return openJar( currentBundle );
    }

    private Artifact getPreviousArtifact()
        throws MojoFailureException, MojoExecutionException
    {
        // Find the previous version JAR and resolve it, and it's dependencies
        final VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec( comparisonVersion );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoFailureException( "Invalid comparison version: " + e.getMessage() );
        }

        final Artifact previousArtifact;
        try
        {
            previousArtifact =
                factory.createDependencyArtifact( comparisonGroupId,
                                                  comparisonArtifactId,
                                                  range,
                                                  comparisonPackaging,
                                                  comparisonClassifier,
                                                  Artifact.SCOPE_COMPILE );

            if ( !previousArtifact.getVersionRange().isSelectedVersionKnown( previousArtifact ) )
            {
                getLog().debug( "Searching for versions in range: " + previousArtifact.getVersionRange() );
                @SuppressWarnings( "unchecked" )
                // type is konwn
                List<ArtifactVersion> availableVersions =
                    metadataSource.retrieveAvailableVersions( previousArtifact, session.getLocalRepository(),
                                                              project.getRemoteArtifactRepositories() );
                filterSnapshots( availableVersions );
                ArtifactVersion version = range.matchVersion( availableVersions );
                if ( version != null )
                {
                    previousArtifact.selectVersion( version.toString() );
                }
            }
        }
        catch ( OverConstrainedVersionException ocve )
        {
            throw new MojoFailureException( "Invalid comparison version: " + ocve.getMessage() );
        }
        catch ( ArtifactMetadataRetrievalException amre )
        {
            throw new MojoExecutionException( "Error determining previous version: " + amre.getMessage(), amre );
        }

        if ( previousArtifact.getVersion() == null )
        {
            getLog().info( "Unable to find a previous version of the project in the repository" );
            return null;
        }

        try
        {
            resolver.resolve( previousArtifact, project.getRemoteArtifactRepositories(), session.getLocalRepository() );
        }
        catch ( ArtifactResolutionException are )
        {
            throw new MojoExecutionException( "Artifact " + previousArtifact + " cannot be resolved : " + are.getMessage(), are );
        }
        catch ( ArtifactNotFoundException anfe )
        {
            throw new MojoExecutionException( "Artifact " + previousArtifact
                + " does not exist on local/remote repositories", anfe );
        }

        return previousArtifact;
    }

    private void filterSnapshots( List<ArtifactVersion> versions )
    {
        for ( Iterator<ArtifactVersion> versionIterator = versions.iterator(); versionIterator.hasNext(); )
        {
            ArtifactVersion version = versionIterator.next();
            if ( version.getQualifier() != null && version.getQualifier().endsWith( "SNAPSHOT" ) )
            {
                versionIterator.remove();
            }
        }
    }

    private static Jar openJar( final File file )
        throws MojoExecutionException
    {
        try
        {
            return new Jar( file );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "An error occurred while opening JAR directory: " + file, e );
        }
    }

    private static void closeJars( final Jar...jars )
    {
        for ( Jar jar : jars )
        {
            jar.close();
        }
    }

    private String getBundleName()
    {
        String extension;
        try
        {
            extension = project.getArtifact().getArtifactHandler().getExtension();
        }
        catch ( Throwable e )
        {
            extension = project.getArtifact().getType();
        }

        if ( StringUtils.isEmpty( extension ) || "bundle".equals( extension ) || "pom".equals( extension ) )
        {
            extension = "jar"; // just in case maven gets confused
        }

        String classifier = this.comparisonClassifier != null ? this.comparisonClassifier : project.getArtifact().getClassifier();
        if ( null != classifier && classifier.trim().length() > 0 )
        {
            return finalName + '-' + classifier + '.' + extension;
        }

        return finalName + '.' + extension;
    }

    private static String getShortDelta( Delta delta )
    {
        switch ( delta )
        {
            case ADDED:
                return "+";

            case CHANGED:
                return "~";

            case MAJOR:
                return ">";

            case MICRO:
                return "0xB5";

            case MINOR:
                return "<";

            case REMOVED:
                return "-";

            case UNCHANGED:
                return " ";

            default:
                String deltaString = delta.toString();
                return String.valueOf( deltaString.charAt( 0 ) );
        }
    }
}
