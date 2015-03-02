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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

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
     *
     * @parameter expression="${baseline.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * Whether to fail on errors.
     *
     * @parameter expression="${baseline.failOnError}" default-value="true"
     */
    protected boolean failOnError;

    /**
     * Whether to fail on warnings.
     *
     * @parameter expression="${baseline.failOnWarning}" default-value="false"
     */
    protected boolean failOnWarning;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File buildDirectory;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     * @readonly
     */
    private String finalName;

    /**
     * @component
     */
    protected ArtifactResolver resolver;

    /**
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * Version to compare the current code against.
     *
     * @parameter expression="${comparisonVersion}" default-value="(,${project.version})"
     * @required
     * @readonly
     */
    protected String comparisonVersion;

    /**
     * Classifier for the artifact to compare the current code against.
     *
     * @parameter expression="${comparisonClassifier}"
     */
    protected String comparisonClassifier;

    /**
     * A list of packages filter, if empty the whole bundle will be traversed. Values are specified in OSGi package
     * instructions notation, e.g. <code>!org.apache.felix.bundleplugin</code>.
     *
     * @parameter
     */
    private String[] filters;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    protected List supportedProjectTypes = Arrays.asList( new String[] { "jar", "bundle" } );

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
        final Jar previousBundle = openJar(previousArtifact.getFile());
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

        // go!
        context = this.init(context);

        String generationDate = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm'Z'" ).format( new Date() );
        Reporter reporter = new Processor();

        try
        {
            Set<Info> infoSet = new Baseline( reporter, new DiffPluginImpl() )
                                .baseline( currentBundle, previousBundle, packageFilters );

            startBaseline( context, generationDate, project.getArtifactId(), project.getVersion(), previousArtifact.getVersion() );

            final Info[] infos = infoSet.toArray( new Info[infoSet.size()] );
            Arrays.sort( infos, new InfoComparator() );

            for ( Info info : infos )
            {
                DiffMessage diffMessage = null;
                Version newerVersion = info.newerVersion;
                Version suggestedVersion = info.suggestedVersion;

                if ( suggestedVersion != null )
                {
                    if ( newerVersion.compareTo( suggestedVersion ) > 0 )
                    {
                        diffMessage = new DiffMessage( "Excessive version increase", DiffMessage.Type.warning );
                        reporter.warning( "%s: %s; detected %s, suggested %s",
                                          info.packageName, diffMessage, info.newerVersion, info.suggestedVersion );
                    }
                    else if ( newerVersion.compareTo( suggestedVersion ) < 0 )
                    {
                        diffMessage = new DiffMessage( "Version increase required", DiffMessage.Type.error );
                        reporter.error( "%s: %s; detected %s, suggested %s",
                                        info.packageName, diffMessage, info.newerVersion, info.suggestedVersion );
                    }
                }

                Diff packageDiff = info.packageDiff;

                Delta delta = packageDiff.getDelta();

                switch ( delta )
                {
                    case UNCHANGED:
                        if ( ( suggestedVersion.getMajor() != newerVersion.getMajor() )
                            || ( suggestedVersion.getMicro() != newerVersion.getMicro() )
                            || ( suggestedVersion.getMinor() != newerVersion.getMinor() ) )
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

                boolean mismatch = info.mismatch;
                String packageName = info.packageName;
                String shortDelta = getShortDelta( info.packageDiff.getDelta() );
                String deltaString = StringUtils.lowerCase( String.valueOf( info.packageDiff.getDelta() ) );
                String newerVersionString = String.valueOf( info.newerVersion );
                String olderVersionString = String.valueOf( info.olderVersion );
                String suggestedVersionString = String.valueOf( ( info.suggestedVersion == null ) ? "-" : info.suggestedVersion );
                Map<String,String> attributes = info.attributes;

                startPackage( context, mismatch,
                              packageName,
                              shortDelta,
                              deltaString,
                              newerVersionString,
                              olderVersionString,
                              suggestedVersionString,
                              diffMessage,
                              attributes );

                if ( Delta.REMOVED != delta )
                {
                    doPackageDiff( context, packageDiff );
                }

                endPackage(context);
            }

            endBaseline(context);
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Impossible to calculate the baseline", e );
        }
        finally
        {
            closeJars( currentBundle, previousBundle );
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
                                          String newerVersion,
                                          String olderVersion,
                                          String suggestedVersion,
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
                factory.createDependencyArtifact( project.getGroupId(),
                                                  project.getArtifactId(),
                                                  range,
                                                  project.getPackaging(),
                                                  comparisonClassifier,
                                                  Artifact.SCOPE_COMPILE );

            if ( !previousArtifact.getVersionRange().isSelectedVersionKnown( previousArtifact ) )
            {
                getLog().debug( "Searching for versions in range: " + previousArtifact.getVersionRange() );
                @SuppressWarnings( "unchecked" )
                // type is konwn
                List<ArtifactVersion> availableVersions =
                    metadataSource.retrieveAvailableVersions( previousArtifact, localRepository,
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
            resolver.resolve( previousArtifact, project.getRemoteArtifactRepositories(), localRepository );
        }
        catch ( ArtifactResolutionException are )
        {
            throw new MojoExecutionException( "Artifact " + previousArtifact + " cannot be resolved", are );
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
            if ( "SNAPSHOT".equals( version.getQualifier() ) )
            {
                versionIterator.remove();
            }
        }
    }

    private static Jar openJar( File file )
        throws MojoExecutionException
    {
        try
        {
            return new Jar( file );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "An error occurred while opening JAR directory: " + file, e );
        }
    }

    private static void closeJars( Jar...jars )
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
