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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.bundleplugin.pom.PomWriter;
import org.apache.maven.archiver.ManifestSection;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.osgi.DefaultMaven2OsgiConverter;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.lib.collections.ExtList;
import aQute.lib.spring.SpringXMLType;
import aQute.libg.generics.Create;


/**
 * Create an OSGi bundle from Maven project
 *
 */
@Mojo( name = "bundle", requiresDependencyResolution = ResolutionScope.TEST,
       threadSafe = true,
       defaultPhase = LifecyclePhase.PACKAGE )
public class BundlePlugin extends AbstractMojo
{
    /**
     * Directory where the manifest will be written
     */
    @Parameter( property = "manifestLocation", defaultValue = "${project.build.outputDirectory}/META-INF" )
    protected File manifestLocation;

    /**
     * Output a nicely formatted manifest that still respects the 72 character line limit.
     */
    @Parameter( property = "niceManifest", defaultValue = "false" )
    protected boolean niceManifest;

    /**
     * File where the BND instructions will be dumped
     */
    @Parameter( property = "dumpInstructions" )
    protected File dumpInstructions;

    /**
     * File where the BND class-path will be dumped
     */
    @Parameter( property = "dumpClasspath" )
    protected File dumpClasspath;

    /**
     * When true, unpack the bundle contents to the outputDirectory
     */
    @Parameter( property = "unpackBundle" )
    protected boolean unpackBundle;

    /**
     * Comma separated list of artifactIds to exclude from the dependency classpath passed to BND (use "true" to exclude everything)
     */
    @Parameter( property = "excludeDependencies" )
    protected String excludeDependencies;

    /**
     * Final name of the bundle (without classifier or extension)
     */
    @Parameter( defaultValue = "${project.build.finalName}")
    private String finalName;

    /**
     * Classifier type of the bundle to be installed.  For example, "jdk14".
     * Defaults to none which means this is the project's main bundle.
     */
    @Parameter
    protected String classifier;

    /**
     * Packaging type of the bundle to be installed.  For example, "jar".
     * Defaults to none which means use the same packaging as the project.
     */
    @Parameter
    protected String packaging;

    /**
     * If true, remove any inlined or embedded dependencies from the resulting pom.
     */
    @Parameter
    protected boolean createDependencyReducedPom;

    /**
     * Where to put the dependency reduced pom. Note: setting a value for this parameter with a directory other than
     * ${basedir} will change the value of ${basedir} for all executions that come after the shade execution. This is
     * often not what you want. This is considered an open issue with this plugin.
     */
    @Parameter( defaultValue = "${basedir}/dependency-reduced-pom.xml" )
    protected File dependencyReducedPomLocation;

    /**
     * Directory where the SCR files will be written
     */
    @Parameter(defaultValue="${project.build.outputDirectory}")
    protected File scrLocation;

    /**
     * When true, dump the generated SCR files
     */
    @Parameter
    protected boolean exportScr;
    
    @Component
    private MavenProjectHelper m_projectHelper;

    @Component
    private ArchiverManager m_archiverManager;

    @Component
    private ArtifactHandlerManager m_artifactHandlerManager;

    @Component
    protected DependencyGraphBuilder m_dependencyGraphBuilder;

    /* The current Maven session.  */
    @Parameter( defaultValue = "${session}", readonly = true )
    protected MavenSession session;


    /**
     * ProjectBuilder, needed to create projects from the artifacts.
     */
    @Component
    protected MavenProjectBuilder mavenProjectBuilder;

    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The dependency graph builder to use.
     */
    @Component
    protected DependencyGraphBuilder dependencyGraphBuilder;

    @Component
    private ArtifactMetadataSource artifactMetadataSource;

    @Component
    private ArtifactCollector artifactCollector;

    @Component
    protected ArtifactFactory artifactFactory;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;


    /**
     * Local maven repository.
     */
    @Parameter( readonly = true, required = true, defaultValue = "${localRepository}" )
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories which will be searched for source attachments.
     */
    @Parameter( readonly = true, required = true, defaultValue = "${project.remoteArtifactRepositories}" )
    protected List<ArtifactRepository> remoteArtifactRepositories;



    /**
     * Project types which this plugin supports.
     */
    @Parameter
    protected List<String> supportedProjectTypes = Arrays.asList("jar", "bundle");

    /**
     * The directory for the generated bundles.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}" )
    private File outputDirectory;

    /**
     * The directory for the generated JAR.
     */
    @Parameter( defaultValue = "${project.build.directory}" )
    private String buildDirectory;

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * The BND instructions for the bundle.
     * Maven will expand property macros in these values. If you want to use a BND macro, you must double the dollar sign
     * for the plugin to pass it to BND correctly. For example: <br>
     * {@code <_consumer-policy>$${range;[===,+)}<code>}<code>{@code </_consumer-policy> }
     */
    @Parameter
    private Map<String, String> instructions = new LinkedHashMap<String, String>();

    /**
     * Use locally patched version for now.
     */
    private final Maven2OsgiConverter m_maven2OsgiConverter = new DefaultMaven2OsgiConverter();

    /**
     * The archive configuration to use.
     */
    @Parameter
    private MavenArchiveConfiguration archive; // accessed indirectly in JarPluginConfiguration

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession m_mavenSession;

    @Component
    protected BuildContext buildContext;
    
    private static final String MAVEN_SYMBOLICNAME = "maven-symbolicname";
    private static final String MAVEN_RESOURCES = "{maven-resources}";
    private static final String MAVEN_TEST_RESOURCES = "{maven-test-resources}";
    private static final String LOCAL_PACKAGES = "{local-packages}";
    private static final String MAVEN_SOURCES = "{maven-sources}";
    private static final String MAVEN_TEST_SOURCES = "{maven-test-sources}";
    private static final String BUNDLE_PLUGIN_EXTENSION = "BNDExtension-";
    private static final String BUNDLE_PLUGIN_PREPEND_EXTENSION = "BNDPrependExtension-";

    private static final String[] EMPTY_STRING_ARRAY =
        {};
    private static final String[] DEFAULT_INCLUDES =
        { "**/**" };

    private static final String NL = System.getProperty( "line.separator" );


    protected Maven2OsgiConverter getMaven2OsgiConverter()
    {
        return m_maven2OsgiConverter;
    }


    protected MavenProject getProject()
    {
        return project;
    }

    protected DependencyNode buildDependencyGraph( MavenProject mavenProject ) throws MojoExecutionException
    {
        DependencyNode dependencyGraph;
        try
        {
            dependencyGraph = m_dependencyGraphBuilder.buildDependencyGraph( mavenProject, null );
        }
        catch ( DependencyGraphBuilderException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        return dependencyGraph;
    }

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException
    {
        Properties properties = new Properties();
        String projectType = getProject().getArtifact().getType();

        // ignore unsupported project types, useful when bundleplugin is configured in parent pom
        if ( !supportedProjectTypes.contains( projectType ) )
        {
            getLog().warn(
                "Ignoring project type " + projectType + " - supportedProjectTypes = " + supportedProjectTypes );
            return;
        }

        execute( getProject(), buildDependencyGraph(getProject()), instructions, properties );
    }


    protected void execute( MavenProject currentProject, DependencyNode dependencyGraph, Map<String, String> originalInstructions, Properties properties )
        throws MojoExecutionException
    {
        try
        {
            execute( currentProject, dependencyGraph, originalInstructions, properties, getClasspath( currentProject, dependencyGraph ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error calculating classpath for project " + currentProject, e );
        }
    }


    /* transform directives from their XML form to the expected BND syntax (eg. _include becomes -include) */
    protected static Map<String, String> transformDirectives( Map<String, String> originalInstructions )
    {
        Map<String, String> transformedInstructions = new LinkedHashMap<String, String>();
        for ( Iterator<Map.Entry<String, String>> i = originalInstructions.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<String, String> e = i.next();

            String key = e.getKey();
            if ( key.startsWith( "_" ) )
            {
                key = "-" + key.substring( 1 );
            }

            String value = e.getValue();
            if ( null == value )
            {
                value = "";
            }
            else
            {
                value = value.replaceAll( "\\p{Blank}*[\r\n]\\p{Blank}*", "" );
            }

            if ( Analyzer.WAB.equals( key ) && value.length() == 0 )
            {
                // provide useful default
                value = "src/main/webapp/";
            }

            transformedInstructions.put( key, value );
        }
        return transformedInstructions;
    }


    protected boolean reportErrors( String prefix, Analyzer analyzer )
    {
        List<String> errors = analyzer.getErrors();
        List<String> warnings = analyzer.getWarnings();

        for ( Iterator<String> w = warnings.iterator(); w.hasNext(); )
        {
            String msg = w.next();
            getLog().warn( prefix + " : " + msg );
        }

        boolean hasErrors = false;
        String fileNotFound = "Input file does not exist: ";
        for ( Iterator<String> e = errors.iterator(); e.hasNext(); )
        {
            String msg = e.next();
            if ( msg.startsWith(fileNotFound) && msg.endsWith( "~" ) )
            {
                // treat as warning; this error happens when you have duplicate entries in Include-Resource
                String duplicate = Processor.removeDuplicateMarker( msg.substring( fileNotFound.length() ) );
                getLog().warn( prefix + " : Duplicate path '" + duplicate + "' in Include-Resource" );
            }
            else
            {
                getLog().error( prefix + " : " + msg );
                hasErrors = true;
            }
        }
        return hasErrors;
    }


    protected void execute( MavenProject currentProject, DependencyNode dependencyGraph, Map<String, String> originalInstructions, Properties properties,
        Jar[] classpath ) throws MojoExecutionException
    {
        try
        {
            File jarFile = new File( getBuildDirectory(), getBundleName( currentProject ) );
            Builder builder = buildOSGiBundle( currentProject, dependencyGraph, originalInstructions, properties, classpath );
            boolean hasErrors = reportErrors( "Bundle " + currentProject.getArtifact(), builder );
            if ( hasErrors )
            {
                String failok = builder.getProperty( "-failok" );
                if ( null == failok || "false".equalsIgnoreCase( failok ) )
                {
                    jarFile.delete();

                    throw new MojoFailureException( "Error(s) found in bundle configuration" );
                }
            }

            // attach bundle to maven project
            jarFile.getParentFile().mkdirs();
            builder.getJar().write( jarFile );

            Artifact mainArtifact = currentProject.getArtifact();

            if ( "bundle".equals( mainArtifact.getType() ) )
            {
                // workaround for MNG-1682: force maven to install artifact using the "jar" handler
                mainArtifact.setArtifactHandler( m_artifactHandlerManager.getArtifactHandler( "jar" ) );
            }

            boolean customClassifier = null != classifier && classifier.trim().length() > 0;
            boolean customPackaging = null != packaging && packaging.trim().length() > 0;

            if ( customClassifier && customPackaging )
            {
                m_projectHelper.attachArtifact( currentProject, packaging, classifier, jarFile );
            }
            else if ( customClassifier )
            {
                m_projectHelper.attachArtifact( currentProject, jarFile, classifier );
            }
            else if ( customPackaging )
            {
                m_projectHelper.attachArtifact( currentProject, packaging, jarFile );
            }
            else
            {
                mainArtifact.setFile( jarFile );
            }

            if ( unpackBundle )
            {
                unpackBundle( jarFile );
            }

            if ( manifestLocation != null )
            {
                File outputFile = new File( manifestLocation, "MANIFEST.MF" );

                try
                {
                    ManifestPlugin.writeManifest( builder, outputFile, niceManifest, exportScr, scrLocation, buildContext, getLog() );
                }
                catch ( IOException e )
                {
                    getLog().error( "Error trying to write Manifest to file " + outputFile, e );
                }
            }

            // cleanup...
            builder.close();
        }
        catch ( MojoFailureException e )
        {
            getLog().error( e.getLocalizedMessage() );
            throw new MojoExecutionException( "Error(s) found in bundle configuration", e );
        }
        catch ( Exception e )
        {
            getLog().error( "An internal error occurred", e );
            throw new MojoExecutionException( "Internal error in maven-bundle-plugin", e );
        }
    }


    protected Builder getOSGiBuilder( MavenProject currentProject, Map<String, String> originalInstructions, Properties properties,
        Jar[] classpath ) throws Exception
    {
        properties.putAll( getDefaultProperties( currentProject ) );
        properties.putAll( transformDirectives( originalInstructions ) );

        // process overrides from project
        final Map<String, String> addProps = new HashMap<String, String>();
        final Iterator<Map.Entry<Object, Object>> iter = currentProject.getProperties().entrySet().iterator();
        while ( iter.hasNext() )
        {
            final Map.Entry<Object, Object> entry = iter.next();
            final String key = entry.getKey().toString();
            if ( key.startsWith(BUNDLE_PLUGIN_EXTENSION) )
            {
                final String oKey = key.substring(BUNDLE_PLUGIN_EXTENSION.length());
                final String currentValue = properties.getProperty(oKey);
                if ( currentValue == null )
                {
                    addProps.put(oKey, entry.getValue().toString());
                }
                else
                {
                    addProps.put(oKey, currentValue + ',' + entry.getValue());
                }
            }
            if ( key.startsWith(BUNDLE_PLUGIN_PREPEND_EXTENSION) )
            {
                final String oKey = key.substring(BUNDLE_PLUGIN_PREPEND_EXTENSION.length());
                final String currentValue = properties.getProperty(oKey);
                if ( currentValue == null )
                {
                    addProps.put(oKey, entry.getValue().toString());
                }
                else
                {
                    addProps.put(oKey, entry.getValue() + "," + currentValue);
                }
            }
        }
        properties.putAll( addProps );
        final Iterator<String> keyIter = addProps.keySet().iterator();
        while ( keyIter.hasNext() )
        {
            Object key = keyIter.next();
            properties.remove(BUNDLE_PLUGIN_EXTENSION + key);
            properties.remove(BUNDLE_PLUGIN_PREPEND_EXTENSION + key);
        }

        if (properties.getProperty("Bundle-Activator") != null
                && properties.getProperty("Bundle-Activator").isEmpty())
        {
            properties.remove("Bundle-Activator");
        }
        if (properties.containsKey("-disable-plugin"))
        {
            String[] disabled = properties.remove("-disable-plugin").toString().replaceAll(" ", "").split(",");
            String[] enabled = properties.getProperty(Analyzer.PLUGIN, "").replaceAll(" ", "").split(",");
            Set<String> plugin = new LinkedHashSet<String>();
            plugin.addAll(Arrays.asList(enabled));
            plugin.removeAll(Arrays.asList(disabled));
            StringBuilder sb = new StringBuilder();
            for (String s : plugin)
            {
                if (sb.length() > 0)
                {
                    sb.append(",");
                }
                sb.append(s);
            }
            properties.setProperty(Analyzer.PLUGIN, sb.toString());
        }

        Builder builder = new Builder();
        synchronized ( BundlePlugin.class ) // protect setBase...getBndLastModified which uses static DateFormat
        {
            builder.setBase( getBase( currentProject ) );
        }
        builder.setProperties( sanitize( properties ) );
        if ( classpath != null )
        {
            builder.setClasspath( classpath );
        }

        return builder;
    }


    protected static Properties sanitize( Properties properties )
    {
        // convert any non-String keys/values to Strings
        Properties sanitizedEntries = new Properties();
        for ( Iterator<Map.Entry<Object,Object>> itr = properties.entrySet().iterator(); itr.hasNext(); )
        {
            Map.Entry<Object,Object> entry = itr.next();
            if ( entry.getKey() instanceof String == false )
            {
                String key = sanitize(entry.getKey());
                if ( !properties.containsKey( key ) )
                {
                    sanitizedEntries.setProperty( key, sanitize( entry.getValue() ) );
                }
                itr.remove();
            }
            else if ( entry.getValue() instanceof String == false )
            {
                entry.setValue( sanitize( entry.getValue() ) );
            }
        }
        properties.putAll( sanitizedEntries );
        return properties;
    }


    protected static String sanitize( Object value )
    {
        if ( value instanceof String )
        {
            return ( String ) value;
        }
        else if ( value instanceof Iterable )
        {
            String delim = "";
            StringBuilder buf = new StringBuilder();
            for ( Object i : ( Iterable<?> ) value )
            {
                buf.append( delim ).append( i );
                delim = ", ";
            }
            return buf.toString();
        }
        else if ( value.getClass().isArray() )
        {
            String delim = "";
            StringBuilder buf = new StringBuilder();
            for ( int i = 0, len = Array.getLength( value ); i < len; i++ )
            {
                buf.append( delim ).append( Array.get( value, i ) );
                delim = ", ";
            }
            return buf.toString();
        }
        else
        {
            return String.valueOf( value );
        }
    }


    protected void addMavenInstructions( MavenProject currentProject, DependencyNode dependencyGraph, Builder builder ) throws Exception
    {
        if ( currentProject.getBasedir() != null )
        {
            // update BND instructions to add included Maven resources
            includeMavenResources(currentProject, builder, getLog());

            // calculate default export/private settings based on sources
            addLocalPackages(outputDirectory, builder);

            // tell BND where the current project source resides
            addMavenSourcePath(currentProject, builder, getLog());
        }

        // update BND instructions to embed selected Maven dependencies
        Collection<Artifact> embeddableArtifacts = getEmbeddableArtifacts( currentProject, dependencyGraph, builder );
        DependencyEmbedder dependencyEmbedder = new DependencyEmbedder(getLog(), dependencyGraph, embeddableArtifacts);
        dependencyEmbedder.processHeaders(builder);

        Collection<Artifact> embeddedArtifacts = dependencyEmbedder.getEmbeddedArtifacts();
        if ( !embeddedArtifacts.isEmpty() && createDependencyReducedPom )
        {
            Set<String> embeddedIds = new HashSet<String>();
            for ( Artifact artifact : embeddedArtifacts )
            {
                embeddedIds.add( getId( artifact ) );
            }
            createDependencyReducedPom( embeddedIds );

        }

        if ( dumpInstructions != null || getLog().isDebugEnabled() )
        {
            StringBuilder buf = new StringBuilder();
            getLog().debug( "BND Instructions:" + NL + dumpInstructions( builder.getProperties(), buf ) );
            if ( dumpInstructions != null )
            {
                getLog().info( "Writing BND instructions to " + dumpInstructions );
                dumpInstructions.getParentFile().mkdirs();
                FileUtils.fileWrite( dumpInstructions, "# BND instructions" + NL + buf );
            }
        }



        if ( dumpClasspath != null || getLog().isDebugEnabled() )
        {
            StringBuilder buf = new StringBuilder();
            getLog().debug("BND Classpath:" + NL + dumpClasspath(builder.getClasspath(), buf));
            if ( dumpClasspath != null )
            {
                getLog().info( "Writing BND classpath to " + dumpClasspath );
                dumpClasspath.getParentFile().mkdirs();
                FileUtils.fileWrite( dumpClasspath, "# BND classpath" + NL + buf );
            }
        }
    }


    // We need to find the direct dependencies that have been included in the uber JAR so that we can modify the
    // POM accordingly.
    private void createDependencyReducedPom( Set<String> artifactsToRemove )
            throws IOException, DependencyTreeBuilderException, ProjectBuildingException
    {
        Model model = project.getOriginalModel();
        List<Dependency> dependencies = new ArrayList<Dependency>();

        boolean modified = false;

        List<Dependency> transitiveDeps = new ArrayList<Dependency>();

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( "pom".equals( artifact.getType() ) )
            {
                // don't include pom type dependencies in dependency reduced pom
                continue;
            }

            //promote
            Dependency dep = new Dependency();
            dep.setArtifactId( artifact.getArtifactId() );
            if ( artifact.hasClassifier() )
            {
                dep.setClassifier( artifact.getClassifier() );
            }
            dep.setGroupId( artifact.getGroupId() );
            dep.setOptional( artifact.isOptional() );
            dep.setScope( artifact.getScope() );
            dep.setType( artifact.getType() );
            dep.setVersion( artifact.getVersion() );

            //we'll figure out the exclusions in a bit.

            transitiveDeps.add( dep );
        }
        List<Dependency> origDeps = project.getDependencies();

        for ( Iterator<Dependency> i = origDeps.iterator(); i.hasNext(); )
        {
            Dependency d = i.next();

            dependencies.add( d );

            String id = getId( d );

            if ( artifactsToRemove.contains( id ) )
            {
                modified = true;

                dependencies.remove( d );
            }
        }

        // Check to see if we have a reduction and if so rewrite the POM.
        if ( modified )
        {
            while ( modified )
            {

                model.setDependencies( dependencies );

                if ( dependencyReducedPomLocation == null )
                {
                    // MSHADE-123: We can't default to 'target' because it messes up uses of ${project.basedir}
                    dependencyReducedPomLocation = new File ( project.getBasedir(), "dependency-reduced-pom.xml" );
                }

                File f = dependencyReducedPomLocation;
                if ( f.exists() )
                {
                    f.delete();
                }

                Writer w = WriterFactory.newXmlWriter( f );

                String origRelativePath = null;
                String replaceRelativePath = null;
                if ( model.getParent() != null)
                {
                    origRelativePath = model.getParent().getRelativePath();

                }
                replaceRelativePath = origRelativePath;

                if ( origRelativePath == null )
                {
                    origRelativePath = "../pom.xml";
                }

                if ( model.getParent() != null )
                {
                    File parentFile = new File( project.getBasedir(), model.getParent().getRelativePath() ).getCanonicalFile();
                    if ( !parentFile.isFile() )
                    {
                        parentFile = new File( parentFile, "pom.xml");
                    }

                    parentFile = parentFile.getCanonicalFile();

                    String relPath = RelativizePath.convertToRelativePath( parentFile, f );
                    model.getParent().setRelativePath( relPath );
                }

                try
                {
                    PomWriter.write( w, model, true );
                }
                finally
                {
                    if ( model.getParent() != null )
                    {
                        model.getParent().setRelativePath( replaceRelativePath );
                    }
                    w.close();
                }

                MavenProject p2 = mavenProjectBuilder.build( f, localRepository, null );
                modified = updateExcludesInDeps( p2, dependencies, transitiveDeps );

            }

            project.setFile( dependencyReducedPomLocation );
        }
    }

    private String getId( Artifact artifact )
    {
        return getId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getClassifier() );
    }

    private String getId( Dependency dependency )
    {
        return getId( dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(),
                dependency.getClassifier() );
    }

    private String getId( String groupId, String artifactId, String type, String classifier )
    {
        return groupId + ":" + artifactId + ":" + type + ":" + ( ( classifier != null ) ? classifier : "" );
    }

    public boolean updateExcludesInDeps( MavenProject project, List<Dependency> dependencies, List<Dependency> transitiveDeps )
            throws DependencyTreeBuilderException
    {
        org.apache.maven.shared.dependency.tree.DependencyNode node = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory,
                artifactMetadataSource, null,
                artifactCollector);
        boolean modified = false;
        Iterator it = node.getChildren().listIterator();
        while ( it.hasNext() )
        {
            org.apache.maven.shared.dependency.tree.DependencyNode n2 = (org.apache.maven.shared.dependency.tree.DependencyNode) it.next();
            Iterator it2 = n2.getChildren().listIterator();
            while ( it2.hasNext() )
            {
                org.apache.maven.shared.dependency.tree.DependencyNode n3 = (org.apache.maven.shared.dependency.tree.DependencyNode) it2.next();
                //anything two levels deep that is marked "included"
                //is stuff that was excluded by the original poms, make sure it
                //remains excluded IF promoting transitives.
                if ( n3.getState() == org.apache.maven.shared.dependency.tree.DependencyNode.INCLUDED )
                {
                    //check if it really isn't in the list of original dependencies.  Maven
                    //prior to 2.0.8 may grab versions from transients instead of
                    //from the direct deps in which case they would be marked included
                    //instead of OMITTED_FOR_DUPLICATE

                    //also, if not promoting the transitives, level 2's would be included
                    boolean found = false;
                    for ( int x = 0; x < transitiveDeps.size(); x++ )
                    {
                        Dependency dep = transitiveDeps.get( x );
                        if ( dep.getArtifactId().equals( n3.getArtifact().getArtifactId() ) && dep.getGroupId().equals(
                                n3.getArtifact().getGroupId() ) )
                        {
                            found = true;
                        }

                    }

                    if ( !found )
                    {
                        for ( int x = 0; x < dependencies.size(); x++ )
                        {
                            Dependency dep = dependencies.get( x );
                            if ( dep.getArtifactId().equals( n2.getArtifact().getArtifactId() )
                                    && dep.getGroupId().equals( n2.getArtifact().getGroupId() ) )
                            {
                                Exclusion exclusion = new Exclusion();
                                exclusion.setArtifactId( n3.getArtifact().getArtifactId() );
                                exclusion.setGroupId( n3.getArtifact().getGroupId() );
                                dep.addExclusion( exclusion );
                                modified = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return modified;
    }


    protected Builder buildOSGiBundle( MavenProject currentProject, DependencyNode dependencyGraph, Map<String, String> originalInstructions, Properties properties,
        Jar[] classpath ) throws Exception
    {
        Builder builder = getOSGiBuilder( currentProject, originalInstructions, properties, classpath );

        addMavenInstructions( currentProject, dependencyGraph, builder );

        builder.build();

        mergeMavenManifest(currentProject, dependencyGraph, builder);

        return builder;
    }


    protected static StringBuilder dumpInstructions( Properties properties, StringBuilder buf )
    {
        try
        {
            buf.append( "#-----------------------------------------------------------------------" + NL );
            Properties stringProperties = new Properties();
            for ( Enumeration<String> e = (Enumeration<String>) properties.propertyNames(); e.hasMoreElements(); )
            {
                // we can only store String properties
                String key = e.nextElement();
                String value = properties.getProperty( key );
                if ( value != null )
                {
                    stringProperties.setProperty( key, value );
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            stringProperties.store( out, null ); // properties encoding is 8859_1
            buf.append( out.toString( "8859_1" ) );
            buf.append( "#-----------------------------------------------------------------------" + NL );
        }
        catch ( Throwable e )
        {
            // ignore...
        }
        return buf;
    }


    protected static StringBuilder dumpClasspath( List<Jar> classpath, StringBuilder buf )
    {
        try
        {
            buf.append("#-----------------------------------------------------------------------" + NL);
            buf.append( "-classpath:\\" + NL );
            for ( Iterator<Jar> i = classpath.iterator(); i.hasNext(); )
            {
                File path = i.next().getSource();
                if ( path != null )
                {
                    buf.append( ' ' + path.toString() + ( i.hasNext() ? ",\\" : "" ) + NL );
                }
            }
            buf.append( "#-----------------------------------------------------------------------" + NL );
        }
        catch ( Throwable e )
        {
            // ignore...
        }
        return buf;
    }


    protected static StringBuilder dumpManifest( Manifest manifest, StringBuilder buf )
    {
        try
        {
            buf.append( "#-----------------------------------------------------------------------" + NL );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ManifestWriter.outputManifest(manifest, out, true); // manifest encoding is UTF8
            buf.append( out.toString( "UTF8" ) );
            buf.append( "#-----------------------------------------------------------------------" + NL );
        }
        catch ( Throwable e )
        {
            // ignore...
        }
        return buf;
    }


    protected static void includeMavenResources( MavenProject currentProject, Analyzer analyzer, Log log )
    {
        // pass maven resource paths onto BND analyzer
        final String mavenResourcePaths = getMavenResourcePaths( currentProject, false );
        final String mavenTestResourcePaths = getMavenResourcePaths( currentProject, true );
        final String includeResource = analyzer.getProperty( Analyzer.INCLUDE_RESOURCE );
        if ( includeResource != null )
        {
            if ( includeResource.contains( MAVEN_RESOURCES ) || includeResource.contains( MAVEN_TEST_RESOURCES ) )
            {
                String combinedResource = StringUtils.replace( includeResource, MAVEN_RESOURCES, mavenResourcePaths );
                combinedResource = StringUtils.replace( combinedResource, MAVEN_TEST_RESOURCES, mavenTestResourcePaths );
                if ( combinedResource.length() > 0 )
                {
                    analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, combinedResource );
                }
                else
                {
                    analyzer.unsetProperty( Analyzer.INCLUDE_RESOURCE );
                }
            }
            else if ( mavenResourcePaths.length() > 0 )
            {
                log.warn( Analyzer.INCLUDE_RESOURCE + ": overriding " + mavenResourcePaths + " with " + includeResource
                        + " (add " + MAVEN_RESOURCES + " if you want to include the maven resources)" );
            }
        }
        else if ( mavenResourcePaths.length() > 0 )
        {
            analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, mavenResourcePaths );
        }
    }


    protected void mergeMavenManifest( MavenProject currentProject, DependencyNode dependencyGraph, Builder builder ) throws Exception
    {
        Jar jar = builder.getJar();

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "BND Manifest:" + NL + dumpManifest( jar.getManifest(), new StringBuilder() ) );
        }

        boolean addMavenDescriptor = currentProject.getBasedir() != null;

        try
        {
            /*
             * Grab customized manifest entries from the maven-jar-plugin configuration
             */
            MavenArchiveConfiguration archiveConfig = JarPluginConfiguration.getArchiveConfiguration( currentProject );
            String mavenManifestText = new MavenArchiver().getManifest( currentProject, archiveConfig ).toString();
            addMavenDescriptor = addMavenDescriptor && archiveConfig.isAddMavenDescriptor();

            Manifest mavenManifest = new Manifest();

            // First grab the external manifest file (if specified and different to target location)
            File externalManifestFile = archiveConfig.getManifestFile();
            if ( null != externalManifestFile )
            {
                if ( !externalManifestFile.isAbsolute() )
                {
                    externalManifestFile = new File( currentProject.getBasedir(), externalManifestFile.getPath() );
                }
                if ( externalManifestFile.exists() && !externalManifestFile.equals( new File( manifestLocation, "MANIFEST.MF" ) ) )
                {
                    InputStream mis = new FileInputStream( externalManifestFile );
                    mavenManifest.read( mis );
                    mis.close();
                }
            }

            // Then apply customized entries from the jar plugin; note: manifest encoding is UTF8
            mavenManifest.read( new ByteArrayInputStream( mavenManifestText.getBytes( "UTF8" ) ) );

            if ( !archiveConfig.isManifestSectionsEmpty() )
            {
                /*
                 * Add customized manifest sections (for some reason MavenArchiver doesn't do this for us)
                 */
                List<ManifestSection> sections = archiveConfig.getManifestSections();
                for ( Iterator<ManifestSection> i = sections.iterator(); i.hasNext(); )
                {
                    ManifestSection section = i.next();
                    Attributes attributes = new Attributes();

                    if ( !section.isManifestEntriesEmpty() )
                    {
                        Map<String, String> entries = section.getManifestEntries();
                        for ( Iterator<Map.Entry<String, String>> j = entries.entrySet().iterator(); j.hasNext(); )
                        {
                            Map.Entry<String, String> entry = j.next();
                            attributes.putValue( entry.getKey(), entry.getValue() );
                        }
                    }

                    mavenManifest.getEntries().put( section.getName(), attributes );
                }
            }

            Attributes mainMavenAttributes = mavenManifest.getMainAttributes();
            mainMavenAttributes.putValue( "Created-By", "Apache Maven Bundle Plugin" );

            String[] removeHeaders = builder.getProperty( Constants.REMOVEHEADERS, "" ).split( "," );

            // apply -removeheaders to the custom manifest
            for ( int i = 0; i < removeHeaders.length; i++ )
            {
                for ( Iterator<Object> j = mainMavenAttributes.keySet().iterator(); j.hasNext(); )
                {
                    if ( j.next().toString().matches( removeHeaders[i].trim() ) )
                    {
                        j.remove();
                    }
                }
            }

            /*
             * Overlay generated bundle manifest with customized entries
             */
            Properties properties = builder.getProperties();
            Manifest bundleManifest = jar.getManifest();
            if ( properties.containsKey( "Merge-Headers" ) )
            {
                Instructions instructions = new Instructions( ExtList.from(builder.getProperty("Merge-Headers")) );
                mergeManifest( instructions, bundleManifest, mavenManifest );
            }
            else
            {
                bundleManifest.getMainAttributes().putAll( mainMavenAttributes );
                bundleManifest.getEntries().putAll( mavenManifest.getEntries() );
            }

            // adjust the import package attributes so that optional dependencies use
            // optional resolution.
            String importPackages = bundleManifest.getMainAttributes().getValue( "Import-Package" );
            if ( importPackages != null )
            {
                Set optionalPackages = getOptionalPackages( currentProject, dependencyGraph );

                Map<String, ? extends Map<String, String>> values = new Analyzer().parseHeader( importPackages );
                for ( Map.Entry<String, ? extends Map<String, String>> entry : values.entrySet() )
                {
                    String pkg = entry.getKey();
                    Map<String, String> options = entry.getValue();
                    if ( !options.containsKey( "resolution:" ) && optionalPackages.contains( pkg ) )
                    {
                        options.put( "resolution:", "optional" );
                    }
                }
                String result = Processor.printClauses( values );
                bundleManifest.getMainAttributes().putValue( "Import-Package", result );
            }

            jar.setManifest( bundleManifest );
        }
        catch ( Exception e )
        {
            getLog().warn( "Unable to merge Maven manifest: " + e.getLocalizedMessage() );
        }

        if ( addMavenDescriptor )
        {
            doMavenMetadata( currentProject, jar );
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Final Manifest:" + NL + dumpManifest( jar.getManifest(), new StringBuilder() ) );
        }

        builder.setJar( jar );
    }


    protected static void mergeManifest( Instructions instructions, Manifest... manifests ) throws IOException
    {
        for ( int i = manifests.length - 2; i >= 0; i-- )
        {
            Manifest mergedManifest = manifests[i];
            Manifest manifest = manifests[i + 1];
            Attributes mergedMainAttributes = mergedManifest.getMainAttributes();
            Attributes mainAttributes = manifest.getMainAttributes();
            Attributes filteredMainAttributes = filterAttributes( instructions, mainAttributes, null );
            if ( !filteredMainAttributes.isEmpty() )
            {
                mergeAttributes( mergedMainAttributes, filteredMainAttributes );
            }
            Map<String, Attributes> mergedEntries = mergedManifest.getEntries();
            Map<String, Attributes> entries = manifest.getEntries();
            for ( Map.Entry<String, Attributes> entry : entries.entrySet() )
            {
                String name = entry.getKey();
                Attributes attributes = entry.getValue();
                Attributes filteredAttributes = filterAttributes( instructions, attributes, null );
                if ( !filteredAttributes.isEmpty() )
                {
                    Attributes mergedAttributes = mergedManifest.getAttributes( name );
                    if ( mergedAttributes != null)
                    {
                        mergeAttributes(mergedAttributes, filteredAttributes);
                    }
                    else
                    {
                        mergedEntries.put(name, filteredAttributes);
                    }
                }
            }
        }
    }


    /**
     * @see Analyzer#filter
     */
    private static Attributes filterAttributes(Instructions instructions, Attributes source, Set<Instruction> nomatch) {
        Attributes result = new Attributes();
        Map<String, Object> keys = new TreeMap<String, Object>();
        for ( Object key : source.keySet() )
        {
            keys.put( key.toString(), key );
        }

        List<Instruction> filters = new ArrayList<Instruction>( instructions.keySet() );
        if (nomatch == null)
        {
            nomatch = Create.set();
        }
        for ( Instruction instruction : filters ) {
            boolean match = false;
            for (Iterator<Map.Entry<String, Object>> i = keys.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry<String, Object> entry = i.next();
                String key = entry.getKey();
                if ( instruction.matches( key ) )
                {
                    match = true;
                    if (!instruction.isNegated()) {
                        Object name = entry.getValue();
                        Object value = source.get( name );
                        result.put( name, value );
                    }
                    i.remove(); // Can never match again for another pattern
                }
            }
            if (!match && !instruction.isAny())
                nomatch.add(instruction);
        }

        /*
         * Tricky. If we have umatched instructions they might indicate that we
         * want to have multiple decorators for the same package. So we check
         * the unmatched against the result list. If then then match and have
         * actually interesting properties then we merge them
         */

        for (Iterator<Instruction> i = nomatch.iterator(); i.hasNext();) {
            Instruction instruction = i.next();

            // We assume the user knows what he is
            // doing and inserted a literal. So
            // we ignore any not matched literals
            // #252, we should not be negated to make it a constant
            if (instruction.isLiteral() && !instruction.isNegated()) {
                Object key = keys.get( instruction.getLiteral() );
                if ( key != null )
                {
                    Object value = source.get( key );
                    result.put( key, value );
                }
                i.remove();
                continue;
            }

            // Not matching a negated instruction looks
            // like an error ... Though so, but
            // in the second phase of Export-Package
            // the !package will never match anymore.
            if (instruction.isNegated()) {
                i.remove();
                continue;
            }

            // An optional instruction should not generate
            // an error
            if (instruction.isOptional()) {
                i.remove();
                continue;
            }
        }
        return result;
    }


    private static void mergeAttributes( Attributes... attributesArray ) throws IOException
    {
        for ( int i = attributesArray.length - 2; i >= 0; i-- )
        {
            Attributes mergedAttributes = attributesArray[i];
            Attributes attributes = attributesArray[i + 1];
            for ( Map.Entry<Object, Object> entry : attributes.entrySet() )
            {
                Object name = entry.getKey();
                String value = (String) entry.getValue();
                String oldValue = (String) mergedAttributes.put( name, value );
                if ( oldValue != null )
                {
                    Parameters mergedClauses = OSGiHeader.parseHeader(oldValue);
                    Parameters clauses = OSGiHeader.parseHeader( value );
                    if ( !mergedClauses.isEqual( clauses) )
                    {
                        for ( Map.Entry<String, Attrs> clauseEntry : clauses.entrySet() )
                        {
                            String clause = clauseEntry.getKey();
                            Attrs attrs = clauseEntry.getValue();
                            Attrs mergedAttrs = mergedClauses.get( clause );
                            if ( mergedAttrs == null)
                            {
                                mergedClauses.put( clause, attrs );
                            }
                            else if ( !mergedAttrs.isEqual(attrs) )
                            {
                                for ( Map.Entry<String,String> adentry : attrs.entrySet() )
                                {
                                    String adname = adentry.getKey();
                                    String ad = adentry.getValue();
                                    if ( mergedAttrs.containsKey( adname ) )
                                    {
                                        Attrs.Type type = attrs.getType( adname );
                                        switch (type)
                                        {
                                            case VERSIONS:
                                            case STRINGS:
                                            case LONGS:
                                            case DOUBLES:
                                                ExtList<String> mergedAd = ExtList.from( mergedAttrs.get( adname ) );
                                                ExtList.from( ad ).addAll( ExtList.from( ad ) );
                                                mergedAttrs.put(adname, mergedAd.join() );
                                                break;
                                        }
                                    }
                                    else
                                    {
                                        mergedAttrs.put( adname, ad );
                                    }
                                }
                            }
                        }
                        mergedAttributes.put( name, Processor.printClauses( mergedClauses ) );
                    }
                }
            }
        }
    }


    protected Set<String> getOptionalPackages( MavenProject currentProject, DependencyNode dependencyGraph ) throws IOException, MojoExecutionException
    {
        ArrayList<Artifact> inscope = new ArrayList<Artifact>();
        final Collection<Artifact> artifacts = getSelectedDependencies( dependencyGraph, currentProject.getArtifacts() );
        for ( Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();
            if ( artifact.getArtifactHandler().isAddedToClasspath() )
            {
                inscope.add( artifact );
            }
        }

        HashSet<String> optionalArtifactIds = new HashSet<String>();
        for ( Iterator<Artifact> it = inscope.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();
            if ( artifact.isOptional() )
            {
                String id = artifact.toString();
                if ( artifact.getScope() != null )
                {
                    // strip the scope...
                    id = id.replaceFirst( ":[^:]*$", "" );
                }
                optionalArtifactIds.add( id );
            }

        }

        HashSet<String> required = new HashSet<String>();
        HashSet<String> optional = new HashSet<String>();
        for ( Iterator<Artifact> it = inscope.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();
            File file = getFile( artifact );
            if ( file == null )
            {
                continue;
            }

            Jar jar = new Jar( artifact.getArtifactId(), file );
            if ( isTransitivelyOptional( optionalArtifactIds, artifact ) )
            {
                optional.addAll( jar.getPackages() );
            }
            else
            {
                required.addAll( jar.getPackages() );
            }
            jar.close();
        }

        optional.removeAll( required );
        return optional;
    }


    /**
     * Check to see if any dependency along the dependency trail of
     * the artifact is optional.
     *
     * @param artifact
     */
    protected boolean isTransitivelyOptional( HashSet<String> optionalArtifactIds, Artifact artifact )
    {
        List<String> trail = artifact.getDependencyTrail();
        for ( Iterator<String> iterator = trail.iterator(); iterator.hasNext(); )
        {
            String next = iterator.next();
            if ( optionalArtifactIds.contains( next ) )
            {
                return true;
            }
        }
        return false;
    }


    private void unpackBundle( File jarFile )
    {
        File outputDir = getOutputDirectory();
        if ( null == outputDir )
        {
            outputDir = new File( getBuildDirectory(), "classes" );
        }

        try
        {
            /*
             * this directory must exist before unpacking, otherwise the plexus
             * unarchiver decides to use the current working directory instead!
             */
            if ( !outputDir.exists() )
            {
                outputDir.mkdirs();
            }

            UnArchiver unArchiver = m_archiverManager.getUnArchiver( "jar" );
            unArchiver.setDestDirectory( outputDir );
            unArchiver.setSourceFile( jarFile );
            unArchiver.extract();
        }
        catch ( Exception e )
        {
            getLog().error( "Problem unpacking " + jarFile + " to " + outputDir, e );
        }
    }


    protected static String removeTagFromInstruction( String instruction, String tag )
    {
        StringBuffer buf = new StringBuffer();

        String[] clauses = instruction.split( "," );
        for ( int i = 0; i < clauses.length; i++ )
        {
            String clause = clauses[i].trim();
            if ( !tag.equals( clause ) )
            {
                if ( buf.length() > 0 )
                {
                    buf.append( ',' );
                }
                buf.append( clause );
            }
        }

        return buf.toString();
    }


    private static Map<String, String> getProperties( Model projectModel, String prefix )
    {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        Method methods[] = Model.class.getDeclaredMethods();
        for ( int i = 0; i < methods.length; i++ )
        {
            String name = methods[i].getName();
            if ( name.startsWith( "get" ) )
            {
                try
                {
                    Object v = methods[i].invoke( projectModel, null );
                    if ( v != null )
                    {
                        name = prefix + Character.toLowerCase( name.charAt( 3 ) ) + name.substring( 4 );
                        if ( v.getClass().isArray() )
                            properties.put( name, Arrays.asList( ( Object[] ) v ).toString() );
                        else
                            properties.put( name, v.toString() );

                    }
                }
                catch ( Exception e )
                {
                    // too bad
                }
            }
        }
        return properties;
    }


    private static StringBuffer printLicenses( List<License> licenses )
    {
        if ( licenses == null || licenses.size() == 0 )
            return null;
        StringBuffer sb = new StringBuffer();
        String del = "";
        for ( Iterator<License> i = licenses.iterator(); i.hasNext(); )
        {
            License l = i.next();
            String url = l.getUrl();
            if ( url == null )
                continue;
            sb.append( del );
            sb.append( url );
            del = ", ";
        }
        if ( sb.length() == 0 )
            return null;
        return sb;
    }


    /**
     * @param jar
     * @throws IOException
     */
    private void doMavenMetadata( MavenProject currentProject, Jar jar ) throws IOException
    {
        String path = "META-INF/maven/" + currentProject.getGroupId() + "/" + currentProject.getArtifactId();

        File pomFile = currentProject.getFile();
        if ( pomFile == null || !pomFile.exists() )
        {
            pomFile = new File( currentProject.getBasedir(), "pom.xml" );
        }
        if ( pomFile.exists() )
        {
            jar.putResource( path + "/pom.xml", new FileResource( pomFile ) );
        }

        Properties p = new Properties();
        p.put( "version", currentProject.getVersion() );
        p.put( "groupId", currentProject.getGroupId() );
        p.put( "artifactId", currentProject.getArtifactId() );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store( out, "Generated by org.apache.felix.bundleplugin" );
        jar.putResource( path + "/pom.properties", new EmbeddedResource( out.toByteArray(), System.currentTimeMillis() ) );
    }


    protected Jar[] getClasspath( MavenProject currentProject, DependencyNode dependencyGraph ) throws IOException, MojoExecutionException
    {
        List<Jar> list = new ArrayList<Jar>();

        if ( getOutputDirectory() != null && getOutputDirectory().exists() )
        {
            list.add( new Jar( ".", getOutputDirectory() ) );
        }

        final Collection<Artifact> artifacts = getSelectedDependencies( dependencyGraph, currentProject.getArtifacts() );
        for ( Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();
            if ( artifact.getArtifactHandler().isAddedToClasspath() && !Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
            {
                File file = getFile( artifact );
                if ( file == null )
                {
                    getLog().warn(
                        "File is not available for artifact " + artifact + " in project "
                            + currentProject.getArtifact() );
                    continue;
                }
                Jar jar = new Jar( artifact.getArtifactId(), file );
                list.add( jar );
            }
        }
        Jar[] cp = new Jar[list.size()];
        list.toArray( cp );
        return cp;
    }


    private Collection<Artifact> getSelectedDependencies( DependencyNode dependencyGraph, Collection<Artifact> artifacts ) throws MojoExecutionException
    {
        if ( null == excludeDependencies || excludeDependencies.length() == 0 )
        {
            return artifacts;
        }
        else if ( "true".equalsIgnoreCase( excludeDependencies ) )
        {
            return Collections.emptyList();
        }

        Collection<Artifact> selectedDependencies = new LinkedHashSet<Artifact>( artifacts );
        DependencyExcluder excluder = new DependencyExcluder( dependencyGraph, artifacts );
        excluder.processHeaders( excludeDependencies );
        selectedDependencies.removeAll( excluder.getExcludedArtifacts() );

        return selectedDependencies;
    }


    /**
     * Get the file for an Artifact
     *
     * @param artifact
     */
    protected File getFile( Artifact artifact )
    {
        return artifact.getFile();
    }


    private static void header( Properties properties, String key, Object value )
    {
        if ( value == null )
            return;

        if ( value instanceof Collection && ( ( Collection ) value ).isEmpty() )
            return;

        properties.put( key, value.toString().replaceAll( "[\r\n]", "" ) );
    }


    /**
     * Convert a Maven version into an OSGi compliant version
     *
     * @param version Maven version
     * @return the OSGi version
     */
    protected String convertVersionToOsgi( String version )
    {
        return getMaven2OsgiConverter().getVersion( version );
    }


    /**
     * TODO this should return getMaven2Osgi().getBundleFileName( project.getArtifact() )
     */
    protected String getBundleName( MavenProject currentProject )
    {
        String extension;
        try
        {
            extension = currentProject.getArtifact().getArtifactHandler().getExtension();
        }
        catch ( Throwable e )
        {
            extension = currentProject.getArtifact().getType();
        }
        if ( StringUtils.isEmpty( extension ) || "bundle".equals( extension ) || "pom".equals( extension ) )
        {
            extension = "jar"; // just in case maven gets confused
        }
        if ( null != classifier && classifier.trim().length() > 0 )
        {
            return finalName + '-' + classifier + '.' + extension;
        }
        return finalName + '.' + extension;
    }


    protected String getBuildDirectory()
    {
        return buildDirectory;
    }


    protected void setBuildDirectory( String _buildirectory )
    {
        buildDirectory = _buildirectory;
    }


    protected Properties getDefaultProperties( MavenProject currentProject )
    {
        Properties properties = new Properties();

        String bsn;
        try
        {
            bsn = getMaven2OsgiConverter().getBundleSymbolicName( currentProject.getArtifact() );
        }
        catch ( Exception e )
        {
            bsn = currentProject.getGroupId() + "." + currentProject.getArtifactId();
        }

        // Setup defaults
        properties.put( MAVEN_SYMBOLICNAME, bsn );
        properties.put( Analyzer.BUNDLE_SYMBOLICNAME, bsn );
        properties.put( Analyzer.IMPORT_PACKAGE, "*" );
        properties.put( Analyzer.BUNDLE_VERSION, getMaven2OsgiConverter().getVersion( currentProject.getVersion() ) );

        // remove the extraneous Include-Resource and Private-Package entries from generated manifest
        properties.put( Constants.REMOVEHEADERS, Analyzer.INCLUDE_RESOURCE + ',' + Analyzer.PRIVATE_PACKAGE );

        header( properties, Analyzer.BUNDLE_DESCRIPTION, currentProject.getDescription() );
        StringBuffer licenseText = printLicenses( currentProject.getLicenses() );
        if ( licenseText != null )
        {
            header( properties, Analyzer.BUNDLE_LICENSE, licenseText );
        }
        header( properties, Analyzer.BUNDLE_NAME, currentProject.getName() );

        if ( currentProject.getOrganization() != null )
        {
            if ( currentProject.getOrganization().getName() != null )
            {
                String organizationName = currentProject.getOrganization().getName();
                header( properties, Analyzer.BUNDLE_VENDOR, organizationName );
                properties.put( "project.organization.name", organizationName );
                properties.put( "pom.organization.name", organizationName );
            }
            if ( currentProject.getOrganization().getUrl() != null )
            {
                String organizationUrl = currentProject.getOrganization().getUrl();
                header( properties, Analyzer.BUNDLE_DOCURL, organizationUrl );
                properties.put( "project.organization.url", organizationUrl );
                properties.put( "pom.organization.url", organizationUrl );
            }
        }

        properties.putAll( currentProject.getProperties() );
        properties.putAll( currentProject.getModel().getProperties() );

        for ( Iterator<String> i = currentProject.getFilters().iterator(); i.hasNext(); )
        {
            File filterFile = new File( i.next() );
            if ( filterFile.isFile() )
            {
                properties.putAll( PropertyUtils.loadProperties( filterFile ) );
            }
        }

        if ( m_mavenSession != null )
        {
            try
            {
                // don't pass upper-case session settings to bnd as they end up in the manifest
                Properties sessionProperties = m_mavenSession.getExecutionProperties();
                for ( Enumeration<String> e = (Enumeration<String>) sessionProperties.propertyNames(); e.hasMoreElements(); )
                {
                    String key = e.nextElement();
                    if ( key.length() > 0 && !Character.isUpperCase( key.charAt( 0 ) ) )
                    {
                        properties.put( key, sessionProperties.getProperty( key ) );
                    }
                }
            }
            catch ( Exception e )
            {
                getLog().warn( "Problem with Maven session properties: " + e.getLocalizedMessage() );
            }
        }

        properties.putAll( getProperties( currentProject.getModel(), "project.build." ) );
        properties.putAll( getProperties( currentProject.getModel(), "pom." ) );
        properties.putAll( getProperties( currentProject.getModel(), "project." ) );

        properties.put( "project.baseDir", getBase( currentProject ) );
        properties.put( "project.build.directory", getBuildDirectory() );
        properties.put( "project.build.outputdirectory", getOutputDirectory() );

        properties.put( "classifier", classifier == null ? "" : classifier );

        // Add default plugins
        header( properties, Analyzer.PLUGIN, BlueprintPlugin.class.getName() + ","
                                           + SpringXMLType.class.getName() + ","
                                           + JpaPlugin.class.getName() );

        return properties;
    }


    protected static File getBase( MavenProject currentProject )
    {
        return currentProject.getBasedir() != null ? currentProject.getBasedir() : new File( "" );
    }


    protected File getOutputDirectory()
    {
        return outputDirectory;
    }


    protected void setOutputDirectory( File _outputDirectory )
    {
        outputDirectory = _outputDirectory;
    }


    private static void addLocalPackages( File outputDirectory, Analyzer analyzer ) throws IOException
    {
        Packages packages = new Packages();

        if ( outputDirectory != null && outputDirectory.isDirectory() )
        {
            // scan classes directory for potential packages
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( outputDirectory );
            scanner.setIncludes( new String[]
                { "**/*.class" } );

            scanner.addDefaultExcludes();
            scanner.scan();

            String[] paths = scanner.getIncludedFiles();
            for ( int i = 0; i < paths.length; i++ )
            {
                packages.put( analyzer.getPackageRef( getPackageName( paths[i] ) ) );
            }
        }

        Packages exportedPkgs = new Packages();
        Packages privatePkgs = new Packages();

        boolean noprivatePackages = "!*".equals( analyzer.getProperty( Analyzer.PRIVATE_PACKAGE ) );

        for ( PackageRef pkg : packages.keySet() )
        {
            // mark all source packages as private by default (can be overridden by export list)
            privatePkgs.put( pkg );

            // we can't export the default package (".") and we shouldn't export internal packages
            String fqn = pkg.getFQN();
            if ( noprivatePackages || !( ".".equals( fqn ) || fqn.contains( ".internal" ) || fqn.contains( ".impl" ) ) )
            {
                exportedPkgs.put( pkg );
            }
        }

        Properties properties = analyzer.getProperties();
        String exported = properties.getProperty( Analyzer.EXPORT_PACKAGE );
        if ( exported == null )
        {
            if ( !properties.containsKey( Analyzer.EXPORT_CONTENTS ) )
            {
                // no -exportcontents overriding the exports, so use our computed list
                for ( Attrs attrs : exportedPkgs.values() )
                {
                    attrs.put( Constants.SPLIT_PACKAGE_DIRECTIVE, "merge-first" );
                }
                properties.setProperty( Analyzer.EXPORT_PACKAGE, Processor.printClauses( exportedPkgs ) );
            }
            else
            {
                // leave Export-Package empty (but non-null) as we have -exportcontents
                properties.setProperty( Analyzer.EXPORT_PACKAGE, "" );
            }
        }
        else if ( exported.indexOf( LOCAL_PACKAGES ) >= 0 )
        {
            String newExported = StringUtils.replace( exported, LOCAL_PACKAGES, Processor.printClauses( exportedPkgs ) );
            properties.setProperty( Analyzer.EXPORT_PACKAGE, newExported );
        }

        String internal = properties.getProperty( Analyzer.PRIVATE_PACKAGE );
        if ( internal == null )
        {
            if ( !privatePkgs.isEmpty() )
            {
                for ( Attrs attrs : privatePkgs.values() )
                {
                    attrs.put( Constants.SPLIT_PACKAGE_DIRECTIVE, "merge-first" );
                }
                properties.setProperty( Analyzer.PRIVATE_PACKAGE, Processor.printClauses( privatePkgs ) );
            }
            else
            {
                // if there are really no private packages then use "!*" as this will keep the Bnd Tool happy
                properties.setProperty( Analyzer.PRIVATE_PACKAGE, "!*" );
            }
        }
        else if ( internal.indexOf( LOCAL_PACKAGES ) >= 0 )
        {
            String newInternal = StringUtils.replace( internal, LOCAL_PACKAGES, Processor.printClauses( privatePkgs ) );
            properties.setProperty( Analyzer.PRIVATE_PACKAGE, newInternal );
        }
    }


    private static String getPackageName( String filename )
    {
        int n = filename.lastIndexOf( File.separatorChar );
        return n < 0 ? "." : filename.substring( 0, n ).replace( File.separatorChar, '.' );
    }


    private static List<Resource> getMavenResources( MavenProject currentProject, boolean test )
    {
        List<Resource> resources = new ArrayList<Resource>( test ? currentProject.getTestResources() : currentProject.getResources() );

        if ( currentProject.getCompileSourceRoots() != null )
        {
            // also scan for any "packageinfo" files lurking in the source folders
            final List<String> packageInfoIncludes = Collections.singletonList( "**/packageinfo" );
            for ( Iterator<String> i = currentProject.getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String sourceRoot = i.next();
                Resource packageInfoResource = new Resource();
                packageInfoResource.setDirectory( sourceRoot );
                packageInfoResource.setIncludes( packageInfoIncludes );
                resources.add( packageInfoResource );
            }
        }

        return resources;
    }


    protected static String getMavenResourcePaths( MavenProject currentProject, boolean test )
    {
        final String basePath = currentProject.getBasedir().getAbsolutePath();

        Set<String> pathSet = new LinkedHashSet<String>();
        for ( Iterator<Resource> i = getMavenResources( currentProject, test ).iterator(); i.hasNext(); )
        {
            Resource resource = i.next();

            final String sourcePath = resource.getDirectory();
            final String targetPath = resource.getTargetPath();

            // ignore empty or non-local resources
            if ( new File( sourcePath ).exists() && ( ( targetPath == null ) || ( targetPath.indexOf( ".." ) < 0 ) ) )
            {
                DirectoryScanner scanner = new DirectoryScanner();

                scanner.setBasedir( sourcePath );
                if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
                {
                    scanner.setIncludes( resource.getIncludes().toArray( EMPTY_STRING_ARRAY ) );
                }
                else
                {
                    scanner.setIncludes( DEFAULT_INCLUDES );
                }

                if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
                {
                    scanner.setExcludes( resource.getExcludes().toArray( EMPTY_STRING_ARRAY ) );
                }

                scanner.addDefaultExcludes();
                scanner.scan();

                List<String> includedFiles = Arrays.asList( scanner.getIncludedFiles() );

                for ( Iterator<String> j = includedFiles.iterator(); j.hasNext(); )
                {
                    String name = j.next();
                    String path = sourcePath + '/' + name;

                    // make relative to project
                    if ( path.startsWith( basePath ) )
                    {
                        if ( path.length() == basePath.length() )
                        {
                            path = ".";
                        }
                        else
                        {
                            path = path.substring( basePath.length() + 1 );
                        }
                    }

                    // replace windows backslash with a slash
                    // this is a workaround for a problem with bnd 0.0.189
                    if ( File.separatorChar != '/' )
                    {
                        name = name.replace( File.separatorChar, '/' );
                        path = path.replace( File.separatorChar, '/' );
                    }

                    // copy to correct place
                    path = name + '=' + path;
                    if ( targetPath != null )
                    {
                        path = targetPath + '/' + path;
                    }

                    // use Bnd filtering?
                    if ( resource.isFiltering() )
                    {
                        path = '{' + path + '}';
                    }

                    pathSet.add( path );
                }
            }
        }

        StringBuffer resourcePaths = new StringBuffer();
        for ( Iterator<String> i = pathSet.iterator(); i.hasNext(); )
        {
            resourcePaths.append( i.next() );
            if ( i.hasNext() )
            {
                resourcePaths.append( ',' );
            }
        }

        return resourcePaths.toString();
    }


    protected Collection<Artifact> getEmbeddableArtifacts( MavenProject currentProject, DependencyNode dependencyGraph, Analyzer analyzer )
        throws MojoExecutionException
    {
        final Collection<Artifact> artifacts;

        String embedTransitive = analyzer.getProperty( DependencyEmbedder.EMBED_TRANSITIVE );
        if ( Boolean.valueOf( embedTransitive ).booleanValue() )
        {
            // includes transitive dependencies
            artifacts = currentProject.getArtifacts();
        }
        else
        {
            // only includes direct dependencies
            artifacts = currentProject.getDependencyArtifacts();
        }

        return getSelectedDependencies( dependencyGraph, artifacts );
    }


    protected static void addMavenSourcePath( MavenProject currentProject, Analyzer analyzer, Log log )
    {
        // pass maven source paths onto BND analyzer
        StringBuilder mavenSourcePaths = new StringBuilder();
        StringBuilder mavenTestSourcePaths = new StringBuilder();
        Map<StringBuilder, List<String>> map = new HashMap<StringBuilder, List<String>>(2);
        map.put(mavenSourcePaths, currentProject.getCompileSourceRoots() );
        map.put(mavenTestSourcePaths, currentProject.getTestCompileSourceRoots() );
        for ( Map.Entry<StringBuilder, List<String>> entry : map.entrySet() )
        {
            List<String> compileSourceRoots = entry.getValue();
            if ( compileSourceRoots != null )
            {
                StringBuilder sourcePaths = entry.getKey();
                for ( Iterator<String> i = compileSourceRoots.iterator(); i.hasNext(); )
                {
                    if ( sourcePaths.length() > 0 )
                    {
                        sourcePaths.append( ',' );
                    }
                    sourcePaths.append( i.next() );
                }
            }
        }
        final String sourcePath = analyzer.getProperty( Analyzer.SOURCEPATH );
        if ( sourcePath != null )
        {
            if ( sourcePath.contains(MAVEN_SOURCES) || sourcePath.contains(MAVEN_TEST_RESOURCES) )
            {
                String combinedSource = StringUtils.replace( sourcePath, MAVEN_SOURCES, mavenSourcePaths.toString() );
                combinedSource = StringUtils.replace( combinedSource, MAVEN_TEST_SOURCES, mavenTestSourcePaths.toString() );
                if ( combinedSource.length() > 0 )
                {
                    analyzer.setProperty( Analyzer.SOURCEPATH, combinedSource );
                }
                else
                {
                    analyzer.unsetProperty( Analyzer.SOURCEPATH );
                }
            }
            else if ( mavenSourcePaths.length() > 0 )
            {
                log.warn( Analyzer.SOURCEPATH + ": overriding " + mavenSourcePaths + " with " + sourcePath + " (add "
                    + MAVEN_SOURCES + " if you want to include the maven sources)" );
            }
            else if ( mavenTestSourcePaths.length() > 0 )
            {
                log.warn( Analyzer.SOURCEPATH + ": overriding " + mavenTestSourcePaths + " with " + sourcePath + " (add "
                        + MAVEN_TEST_SOURCES + " if you want to include the maven test sources)" );
            }
        }
        else if ( mavenSourcePaths.length() > 0 )
        {
            analyzer.setProperty( Analyzer.SOURCEPATH, mavenSourcePaths.toString() );
        }
    }
}
