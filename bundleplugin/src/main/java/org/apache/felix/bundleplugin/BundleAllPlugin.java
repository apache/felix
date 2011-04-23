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
package org.apache.felix.bundleplugin;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.codehaus.plexus.util.FileUtils;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;


/**
 * Create OSGi bundles from all dependencies in the Maven project
 * 
 * @goal bundleall
 * @phase package
 * @requiresDependencyResolution test
 * @description build an OSGi bundle jar for all transitive dependencies
 */
public class BundleAllPlugin extends ManifestPlugin
{
    @SuppressWarnings("serial")
    static class MojoExecutionRuntimeException extends RuntimeException {
        public MojoExecutionRuntimeException(MojoExecutionException e) {
            super(e);
        }
        
        @Override
        public MojoExecutionException getCause() {
            return (MojoExecutionException) super.getCause();
        }
    }

    private static final String LS = System.getProperty( "line.separator" );

    private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile( "[0-9]{8}_[0-9]{6}_[0-9]+" );

    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from
     * all scopes. Note that this feature does not currently work due to MNG-3236.
     *
     * @since 2.0-alpha-5
     * @see <a href="http://jira.codehaus.org/browse/MNG-3236">MNG-3236</a>
     *
     * @parameter expression="${scope}"
     */
    private String scope;

    /**
     * Local repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Remote repositories.
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteRepositories;

    /**
     * Import-Package to be used when wrapping dependencies.
     *
     * @parameter expression="${wrapImportPackage}" default-value="*"
     */
    private String wrapImportPackage;

    /**
     * @component
     */
    private ArtifactFactory m_factory;

    /**
     * @component
     */
    private ArtifactMetadataSource m_artifactMetadataSource;

    /**
     * @component
     */
    private ArtifactCollector m_collector;

    /**
     * Artifact resolver, needed to download jars.
     * 
     * @component
     */
    private ArtifactResolver m_artifactResolver;

    /**
     * @component
     */
    private DependencyTreeBuilder m_dependencyTreeBuilder;

    /**
     * @component
     */
    private MavenProjectBuilder m_mavenProjectBuilder;

    /**
     * Ignore missing artifacts that are not required by current project but are required by the
     * transitive dependencies.
     * 
     * @parameter
     */
    private boolean ignoreMissingArtifacts;

    private Set m_artifactsBeingProcessed = new HashSet();

    /**
     * Process up to some depth 
     * 
     * @parameter
     */
    private int depth = Integer.MAX_VALUE;
    
    
    /**
     * Supported project types 
     * 
     * @parameter
     */
    private List supportedProjectTypes = Arrays.asList(new String[]{ "jar", "bundle" });

    public void execute() throws MojoExecutionException
    {
        BundleInfo bundleInfo = bundleAll( getProject() );
        if( bundleInfo != null )
            logDuplicatedPackages( bundleInfo );
    }


    /**
     * Bundle a project and all its dependencies
     * 
     * @param project
     * @throws MojoExecutionException
     */
    private BundleInfo bundleAll( MavenProject project ) throws MojoExecutionException
    {
        return bundleAll( project, depth );
    }


    /**
     * Bundle a project and its transitive dependencies up to some depth level
     * 
     * @param project
     * @param maxDepth how deep to process the dependency tree
     * @throws MojoExecutionException
     */
    protected BundleInfo bundleAll( final MavenProject project, int maxDepth ) throws MojoExecutionException
    {
        if( !supportedProjectTypes.contains(project.getPackaging()) ){
            getLog().warn(
                "Ignoring project type " + project.getPackaging() + " - supportedProjectTypes = " + supportedProjectTypes );
            return null;
        }
        
        DependencyNode dependencyTree;

        try
        {
            dependencyTree = m_dependencyTreeBuilder.buildDependencyTree( project, localRepository, m_factory,
                m_artifactMetadataSource, createResolvingArtifactFilter(), m_collector );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new MojoExecutionException( "Unable to build dependency tree", e );
        }

        final BundleInfo bundleInfo = new BundleInfo();

        getLog().debug( "Will bundle the following dependency tree" + LS + dependencyTree );

        try {
            dependencyTree.accept(new Bundler(bundleInfo, maxDepth));
        } catch (MojoExecutionRuntimeException e) {
            throw e.getCause();
        }

        return bundleInfo;
    }


    /**
     * Bundle the root of a dependency tree after all its children have been bundled
     * 
     * @param project
     * @param bundleInfo
     * @return
     * @throws MojoExecutionException
     */
    private BundleInfo bundleRoot( MavenProject project, BundleInfo bundleInfo ) throws MojoExecutionException
    {
        /* do not bundle the project the mojo was called on */
        if ( getProject() != project )
        {
            getLog().debug( "Project artifact location: " + project.getArtifact().getFile() );

            BundleInfo subBundleInfo = bundle( project );
            if ( subBundleInfo != null )
            {
                bundleInfo.merge( subBundleInfo );
            }
        }
        return bundleInfo;
    }


    /**
     * Bundle one project only without building its childre
     * 
     * @param project
     * @throws MojoExecutionException
     */
    protected BundleInfo bundle( MavenProject project ) throws MojoExecutionException
    {
        Artifact artifact = project.getArtifact();
        getLog().info( "Bundling " + artifact );

        try
        {
            Map instructions = new LinkedHashMap();
            instructions.put( Analyzer.IMPORT_PACKAGE, wrapImportPackage );

            File outputFile = getOutputFile( artifact );

            Analyzer analyzer = getAnalyzer( project, instructions, new Properties(), getClasspath( project ) );

            Jar osgiJar = new Jar( project.getArtifactId(), project.getArtifact().getFile() );

            outputFile.getAbsoluteFile().getParentFile().mkdirs();

            Collection exportedPackages;
            if ( isOsgi( osgiJar ) )
            {
                /* if it is already an OSGi jar copy it as is */
                getLog().info(
                    " - Using existing OSGi bundle for " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                        + project.getVersion() );
                String exportHeader = osgiJar.getManifest().getMainAttributes().getValue( Analyzer.EXPORT_PACKAGE );
                exportedPackages = analyzer.parseHeader( exportHeader ).keySet();
                FileUtils.copyFile( project.getArtifact().getFile(), outputFile );
            }
            else
            {
                /* else generate the manifest from the packages */
                exportedPackages = analyzer.getExports().keySet();
                Manifest manifest = analyzer.getJar().getManifest();
                osgiJar.setManifest( manifest );
                osgiJar.write( outputFile );
            }

            BundleInfo bundleInfo = addExportedPackages( project, exportedPackages );

            // cleanup...
            analyzer.close();
            osgiJar.close();

            return bundleInfo;
        }
        /* too bad Jar.write throws Exception */
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error generating OSGi bundle for project "
                + getArtifactKey( project.getArtifact() ), e );
        }
    }


    private boolean isOsgi( Jar jar ) throws Exception
    {
        if ( jar.getManifest() != null )
        {
            return jar.getManifest().getMainAttributes().getValue( Analyzer.BUNDLE_NAME ) != null;
        }
        return false;
    }


    private BundleInfo addExportedPackages( MavenProject project, Collection packages )
    {
        BundleInfo bundleInfo = new BundleInfo();
        for ( Iterator it = packages.iterator(); it.hasNext(); )
        {
            String packageName = ( String ) it.next();
            bundleInfo.addExportedPackage( packageName, project.getArtifact() );
        }
        return bundleInfo;
    }


    private String getArtifactKey( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }


    private String getBundleName( Artifact artifact )
    {
        return getMaven2OsgiConverter().getBundleFileName( artifact );
    }


    private boolean alreadyBundled( Artifact artifact )
    {
        return getBuiltFile( artifact ) != null;
    }


    /**
     * Use previously built bundles when available.
     * 
     * @param artifact
     */
    protected File getFile( final Artifact artifact )
    {
        File bundle = getBuiltFile( artifact );

        if ( bundle != null )
        {
            getLog().debug( "Using previously built OSGi bundle for " + artifact + " in " + bundle );
            return bundle;
        }
        return super.getFile( artifact );
    }


    private File getBuiltFile( final Artifact artifact )
    {
        File bundle = null;

        /* if bundle was already built use it instead of jar from repo */
        File outputFile = getOutputFile( artifact );
        if ( outputFile.exists() )
        {
            bundle = outputFile;
        }

        /*
         * Find snapshots in output folder, eg. 2.1-SNAPSHOT will match 2.1.0.20070207_193904_2
         * TODO there has to be another way to do this using Maven libs 
         */
        if ( ( bundle == null ) && artifact.isSnapshot() )
        {
            final File buildDirectory = new File( getBuildDirectory() );
            if ( !buildDirectory.exists() )
            {
                buildDirectory.mkdirs();
            }
            File[] files = buildDirectory.listFiles( new FilenameFilter()
            {
                public boolean accept( File dir, String name )
                {
                    if ( dir.equals( buildDirectory ) && snapshotMatch( artifact, name ) )
                    {
                        return true;
                    }
                    return false;
                }
            } );
            if ( files.length > 1 )
            {
                throw new RuntimeException( "More than one previously built bundle matches for artifact " + artifact
                    + " : " + Arrays.asList( files ) );
            }
            if ( files.length == 1 )
            {
                bundle = files[0];
            }
        }

        return bundle;
    }


    /**
     * Check that the bundleName provided correspond to the artifact provided.
     * Used to determine when the bundle name is a timestamped snapshot and the artifact is a snapshot not timestamped.
     * 
     * @param artifact artifact with snapshot version
     * @param bundleName bundle file name 
     * @return if both represent the same artifact and version, forgetting about the snapshot timestamp
     */
    protected boolean snapshotMatch( Artifact artifact, String bundleName )
    {
        String artifactBundleName = getBundleName( artifact );
        int i = artifactBundleName.indexOf( "SNAPSHOT" );
        if ( i < 0 )
        {
            return false;
        }
        artifactBundleName = artifactBundleName.substring( 0, i );

        if ( bundleName.startsWith( artifactBundleName ) )
        {
            /* it's the same artifact groupId and artifactId */
            String timestamp = bundleName.substring( artifactBundleName.length(), bundleName.lastIndexOf( ".jar" ) );
            Matcher m = SNAPSHOT_VERSION_PATTERN.matcher( timestamp );
            return m.matches();
        }
        return false;
    }


    protected File getOutputFile( Artifact artifact )
    {
        File bundleFolder = new File(getBuildDirectory(), "bundles");
        if( !bundleFolder.exists() )
            bundleFolder.mkdir();
        return new File(bundleFolder, getBundleName( artifact ) );
    }


    private Artifact resolveArtifact( Artifact artifact ) throws MojoExecutionException, ArtifactNotFoundException
    {
        VersionRange versionRange;
        if ( artifact.getVersion() != null )
        {
            versionRange = VersionRange.createFromVersion( artifact.getVersion() );
        }
        else
        {
            versionRange = artifact.getVersionRange();
        }

        /*
         * there's a bug with ArtifactFactory#createDependencyArtifact(String, String, VersionRange,
         * String, String, String) that ignores the scope parameter, that's why we use the one with
         * the extra null parameter
         */
        Artifact resolvedArtifact = m_factory.createDependencyArtifact( artifact.getGroupId(),
            artifact.getArtifactId(), versionRange, artifact.getType(), artifact.getClassifier(), artifact.getScope(),
            null );

        try
        {
            m_artifactResolver.resolve( resolvedArtifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Error resolving artifact " + resolvedArtifact, e );
        }

        return resolvedArtifact;
    }


    /**
     * Log what packages are exported in more than one bundle
     */
    protected void logDuplicatedPackages( BundleInfo bundleInfo )
    {
        Map duplicatedExports = bundleInfo.getDuplicatedExports();

        for ( Iterator it = duplicatedExports.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = ( Map.Entry ) it.next();
            String packageName = ( String ) entry.getKey();
            Collection artifacts = ( Collection ) entry.getValue();

            getLog().warn( "Package " + packageName + " is exported in more than a bundle: " );
            for ( Iterator it2 = artifacts.iterator(); it2.hasNext(); )
            {
                Artifact artifact = ( Artifact ) it2.next();
                getLog().warn( "  " + artifact );
            }

        }
    }
    
    /**
     * Gets the artifact filter to use when resolving the dependency tree.
     *
     * @return the artifact filter
     */
    private ArtifactFilter createResolvingArtifactFilter()
    {
        ArtifactFilter filter;

        // filter scope
        if ( scope != null )
        {
            getLog().debug( "+ Resolving dependency tree for scope '" + scope + "'" );

            filter = new ScopeArtifactFilter( scope );
        }
        else
        {
            filter = null;
        }

        return filter;
    }
    
    
    /**
     * A dependency node visitor that serializes visited nodes to a writer.
     * 
     * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
     * @version $Id: SerializingDependencyNodeVisitor.java 661727 2008-05-30 14:21:49Z bentmann $
     * @since 1.1
     */
    class Bundler implements DependencyNodeVisitor
    {
        /**
         * The depth of the currently visited dependency node.
         */
        private Deque<BundleInfo> bundleInfos = new ArrayDeque<BundleInfo>();
        private int maxDepth = Integer.MAX_VALUE;
        
        /**
         * Creates a dependency node visitor that serializes visited nodes to the specified writer using the specified
         * tokens.
         * @param bundleInfo 
         * @param maxDepth 
         * 
         * @param writer
         *            the writer to serialize to
         * @param tokens
         *            the tokens to use when serializing the dependency tree
         */
        public Bundler(BundleInfo bundleInfo, int maxDepth)
        {
            bundleInfos.addLast(bundleInfo);
        }

        /**
         * 
         * @return
         */
        public int getDepth()
        {
            return bundleInfos.size();
        }

        // DependencyNodeVisitor methods ------------------------------------------
        /**
         * {@inheritDoc}
         */
        public boolean visit(DependencyNode node)
        {
            if( getDepth() > maxDepth ){
                /* node is deeper than we want */
                getLog().debug(
                    "Ignoring " + node.getArtifact() + ", depth is " + getDepth()
                        + ", bigger than " + maxDepth);
                return false;
            }
            
            bundleInfos.addLast(new BundleInfo());

            return true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean endVisit(DependencyNode node)
        {
            if( getDepth() > maxDepth )
                return false;

            BundleInfo subBundleInfo = bundleInfos.removeLast();

            try
            {
                BundleInfo bundleInfo = bundle(node, subBundleInfo);
                if (!bundleInfos.isEmpty())
                    bundleInfos.peekLast().merge(bundleInfo);
            }
            catch (MojoExecutionException e)
            {
                throw new MojoExecutionRuntimeException(e);
            }

            return true;
        }

        protected BundleInfo bundle(DependencyNode node, BundleInfo bundleInfo)
            throws MojoExecutionException
        {
            Artifact artifact = null;
            try
            {
                artifact = resolveArtifact(node.getArtifact());
            }
            catch (ArtifactNotFoundException e)
            {
                if (!ignoreMissingArtifacts)
                {
                    throw new MojoExecutionException(
                            "Artifact was not found in the repo"
                                    + node.getArtifact(), e);
                }
            }

            node.getArtifact().setFile(artifact.getFile());

            MavenProject project;
            try
            {
                project = m_mavenProjectBuilder.buildFromRepository(artifact,
                    remoteRepositories,
                    localRepository, true);
                if (project.getDependencyArtifacts() == null)
                {
                    project.setDependencyArtifacts(project.createArtifacts(m_factory,
                        null, null));
                }
            }
            catch (ProjectBuildingException e)
            {
                throw new MojoExecutionException(
                    "Unable to build project object for artifact " + artifact, e);
            }
            catch (InvalidDependencyVersionException e)
            {
                throw new MojoExecutionException(
                    "Invalid dependency version for artifact " + artifact);
            }

            project.setArtifact(artifact);

            return bundleRoot(project, bundleInfo);
        }

        /**
         * Gets whether the specified dependency node is the last of its siblings.
         * 
         * @param node
         *            the dependency node to check
         * @return <code>true</code> if the specified dependency node is the last of its last siblings
         */
        private boolean isLast(DependencyNode node)
        {
            DependencyNode parent = node.getParent();

            boolean last;

            if (parent == null)
            {
                last = true;
            }
            else
            {
                List<?> siblings = parent.getChildren();

                last = (siblings.indexOf(node) == siblings.size() - 1);
            }

            return last;
        }
    }
}
