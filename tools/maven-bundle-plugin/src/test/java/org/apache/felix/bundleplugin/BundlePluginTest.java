package org.apache.felix.bundleplugin;


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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Manifest;

import org.apache.maven.model.Organization;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;


/**
 * Test for {@link BundlePlugin}.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundlePluginTest extends AbstractBundlePluginTest
{

    private BundlePlugin plugin;


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        plugin = new BundlePlugin();
        plugin.setBuildDirectory( "." );
        plugin.setOutputDirectory(new File(getBasedir(), "target" + File.separatorChar + "scratch"));
        setVariableValueToObject(plugin, "m_dependencyGraphBuilder", lookup(DependencyGraphBuilder.class.getName(), "default"));
    }


    public void testConvertVersionToOsgi()
    {
        String osgiVersion;

        osgiVersion = plugin.convertVersionToOsgi( "2.1.0-SNAPSHOT" );
        assertEquals( "2.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1-SNAPSHOT" );
        assertEquals( "2.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2-SNAPSHOT" );
        assertEquals( "2.0.0.SNAPSHOT", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2" );
        assertEquals( "2.0.0", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1" );
        assertEquals( "2.1.0", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1.3" );
        assertEquals( "2.1.3", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "2.1.3.4" );
        assertEquals( "2.1.3.4", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.1-alpha-2" );
        assertEquals( "1.1.0.alpha-2", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.0-alpha-16-20070122.203121-13" );
        assertEquals( "1.0.0.alpha-16-20070122_203121-13", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.0-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432-1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432-1", osgiVersion );

        osgiVersion = plugin.convertVersionToOsgi( "1.4.1-20070217.082013-7" );
        assertEquals( "1.4.1.20070217_082013-7", osgiVersion );
    }


    public void testReadExportedModules() throws Exception
    {
        File osgiBundleFile = getTestBundle();

        assertTrue( osgiBundleFile.exists() );

        MavenProject project = getMavenProjectStub();

        //        PackageVersionAnalyzer analyzer = new PackageVersionAnalyzer();
        Builder analyzer = new Builder();
        Jar jar = new Jar( "name", osgiBundleFile );
        analyzer.setJar( jar );
        analyzer.setClasspath( new Jar[]
            { jar } );

        analyzer.setProperty( Analyzer.EXPORT_PACKAGE, "*" );
        analyzer.getJar().setManifest( analyzer.calcManifest() );

        assertEquals( 3, analyzer.getExports().size() );

        analyzer.close();
    }


    public void testTransformDirectives() throws Exception
    {
        Map instructions = new TreeMap();

        instructions.put( "a", "1" );
        instructions.put( "-a", "2" );
        instructions.put( "_a", "3" );
        instructions.put( "A", "3" );
        instructions.put( "_A", "1" );
        instructions.put( "_b", "4" );
        instructions.put( "b", "6" );
        instructions.put( "_B", "6" );
        instructions.put( "-B", "5" );
        instructions.put( "B", "4" );

        instructions.put( "z", null );
        instructions.put( "_z", null );

        Map transformedInstructions = BundlePlugin.transformDirectives( instructions );

        assertEquals( "1", transformedInstructions.get( "a" ) );
        assertEquals( "3", transformedInstructions.get( "-a" ) );
        assertEquals( null, transformedInstructions.get( "_a" ) );
        assertEquals( "3", transformedInstructions.get( "A" ) );
        assertEquals( "1", transformedInstructions.get( "-A" ) );
        assertEquals( null, transformedInstructions.get( "_A" ) );
        assertEquals( null, transformedInstructions.get( "_b" ) );
        assertEquals( "4", transformedInstructions.get( "-b" ) );
        assertEquals( "6", transformedInstructions.get( "b" ) );
        assertEquals( null, transformedInstructions.get( "_B" ) );
        assertEquals( "6", transformedInstructions.get( "-B" ) );
        assertEquals( "4", transformedInstructions.get( "B" ) );

        assertEquals( "", transformedInstructions.get( "z" ) );
        assertEquals( "", transformedInstructions.get( "-z" ) );
    }


    public void testDefaultPropertiesIncludeOrganization()
    {
        final Organization organization = new Organization();
        organization.setName( "Example Organization" );
        organization.setUrl( "http://example.org" );

        // MavenProjectStub.setOrganization(Organization) doesn't do anything, so we have to make it work this way
        MavenProject project = new MavenProjectStub()
        {
            @Override
            public Organization getOrganization()
            {
                return organization;
            }
        };
        project.setGroupId( "group" );
        project.setArtifactId( "project" );
        project.setVersion( "1.2.3.4" );

        Properties properties = plugin.getDefaultProperties( project );
        assertEquals( organization.getName(), properties.getProperty( "project.organization.name" ) );
        assertEquals( organization.getName(), properties.getProperty( "pom.organization.name" ) );
        assertEquals( organization.getUrl(), properties.getProperty( "project.organization.url" ) );
        assertEquals( organization.getUrl(), properties.getProperty( "pom.organization.url" ) );
    }


    public void testVersion() throws Exception
    {
        String cleanupVersion = Builder.cleanupVersion( "0.0.0.4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", cleanupVersion );
    }


    public void testPackageInfoDetection() throws Exception
    {
        MavenProject project = getMavenProjectStub();
        project.addCompileSourceRoot( getBasedir() + "/src/test/java" );

        String resourcePaths = plugin.getMavenResourcePaths( project, false );

        assertEquals( "org/apache/felix/bundleplugin/packageinfo="
            + "src/test/java/org/apache/felix/bundleplugin/packageinfo", resourcePaths );
    }


    public void testEmbedDependencyPositiveClauses() throws Exception
    {
        ArtifactStubFactory artifactFactory = new ArtifactStubFactory( plugin.getOutputDirectory(), true );

        Set artifacts = new LinkedHashSet();

        artifacts.addAll( artifactFactory.getClassifiedArtifacts() );
        artifacts.addAll( artifactFactory.getScopedArtifacts() );
        artifacts.addAll( artifactFactory.getTypedArtifacts() );

        MavenProject project = getMavenProjectStub();
        project.setDependencyArtifacts( artifacts );

        Map instructions = new HashMap();
        instructions.put( DependencyEmbedder.EMBED_DEPENDENCY, "*;classifier=;type=jar;scope=compile,"
            + "*;classifier=;type=jar;scope=runtime" );
        Properties props = new Properties();

        DependencyNode dependencyGraph = plugin.buildDependencyGraph(project);
        Builder builder = plugin.buildOSGiBundle( project, dependencyGraph, instructions, props, plugin.getClasspath( project, dependencyGraph ) );
        Manifest manifest = builder.getJar().getManifest();

        String bcp = manifest.getMainAttributes().getValue( Constants.BUNDLE_CLASSPATH );
        assertEquals( ".," + "compile-1.0.jar,b-1.0.jar,runtime-1.0.jar", bcp );

        String eas = manifest.getMainAttributes().getValue( "Embedded-Artifacts" );
        assertEquals( "compile-1.0.jar;g=\"g\";a=\"compile\";v=\"1.0\"," + "b-1.0.jar;g=\"g\";a=\"b\";v=\"1.0\","
            + "runtime-1.0.jar;g=\"g\";a=\"runtime\";v=\"1.0\"", eas );
    }


    public void testEmbedDependencyNegativeClauses() throws Exception
    {
        ArtifactStubFactory artifactFactory = new ArtifactStubFactory( plugin.getOutputDirectory(), true );

        Set artifacts = new LinkedHashSet();

        artifacts.addAll( artifactFactory.getClassifiedArtifacts() );
        artifacts.addAll( artifactFactory.getScopedArtifacts() );
        artifacts.addAll( artifactFactory.getTypedArtifacts() );

        MavenProject project = getMavenProjectStub();
        project.setDependencyArtifacts( artifacts );

        Map instructions = new HashMap();
        instructions.put( DependencyEmbedder.EMBED_DEPENDENCY, "!type=jar, !artifactId=c" );
        Properties props = new Properties();

        DependencyNode dependencyGraph = plugin.buildDependencyGraph(project);
        Builder builder = plugin.buildOSGiBundle( project, dependencyGraph, instructions, props, plugin.getClasspath( project, dependencyGraph ) );
        Manifest manifest = builder.getJar().getManifest();

        String bcp = manifest.getMainAttributes().getValue( Constants.BUNDLE_CLASSPATH );
        assertEquals( ".," + "a-1.0.war," + "d-1.0.zip," + "e-1.0.rar", bcp );

        String eas = manifest.getMainAttributes().getValue( "Embedded-Artifacts" );
        assertEquals( "a-1.0.war;g=\"g\";a=\"a\";v=\"1.0\"," + "d-1.0.zip;g=\"g\";a=\"d\";v=\"1.0\","
            + "e-1.0.rar;g=\"g\";a=\"e\";v=\"1.0\"", eas );
    }


    public void testEmbedDependencyDuplicateKeys() throws Exception
    {
        ArtifactStubFactory artifactFactory = new ArtifactStubFactory( plugin.getOutputDirectory(), true );

        Set artifacts = new LinkedHashSet();

        artifacts.addAll( artifactFactory.getClassifiedArtifacts() );
        artifacts.addAll( artifactFactory.getScopedArtifacts() );
        artifacts.addAll( artifactFactory.getTypedArtifacts() );

        MavenProject project = getMavenProjectStub();
        project.setDependencyArtifacts( artifacts );

        Map instructions = new HashMap();
        instructions.put( DependencyEmbedder.EMBED_DEPENDENCY, "c;type=jar,c;type=sources" );
        Properties props = new Properties();

        DependencyNode dependencyGraph = plugin.buildDependencyGraph(project);
        Builder builder = plugin.buildOSGiBundle( project, dependencyGraph, instructions, props, plugin.getClasspath( project, dependencyGraph ) );
        Manifest manifest = builder.getJar().getManifest();

        String bcp = manifest.getMainAttributes().getValue( Constants.BUNDLE_CLASSPATH );
        assertEquals( ".," + "c-1.0-three.jar," + "c-1.0.sources", bcp );

        String eas = manifest.getMainAttributes().getValue( "Embedded-Artifacts" );
        assertEquals( "c-1.0-three.jar;g=\"g\";a=\"c\";v=\"1.0\";c=\"three\","
            + "c-1.0.sources;g=\"g\";a=\"c\";v=\"1.0\"", eas );
    }


    public void testEmbedDependencyMissingPositiveKey() throws Exception
    {
        ArtifactStubFactory artifactFactory = new ArtifactStubFactory( plugin.getOutputDirectory(), true );

        Set artifacts = new LinkedHashSet();

        artifacts.addAll( artifactFactory.getClassifiedArtifacts() );
        artifacts.addAll( artifactFactory.getScopedArtifacts() );
        artifacts.addAll( artifactFactory.getTypedArtifacts() );

        MavenProject project = getMavenProjectStub();
        project.setDependencyArtifacts( artifacts );

        Map instructions = new HashMap();
        instructions.put( DependencyEmbedder.EMBED_DEPENDENCY, "artifactId=a|b" );
        Properties props = new Properties();

        DependencyNode dependencyGraph = plugin.buildDependencyGraph(project);
        Builder builder = plugin.buildOSGiBundle( project, dependencyGraph, instructions, props, plugin.getClasspath( project, dependencyGraph ) );
        Manifest manifest = builder.getJar().getManifest();

        String bcp = manifest.getMainAttributes().getValue( Constants.BUNDLE_CLASSPATH );
        assertEquals( ".," + "a-1.0-one.jar," + "b-1.0-two.jar," + "a-1.0.war," + "b-1.0.jar", bcp );

        String eas = manifest.getMainAttributes().getValue( "Embedded-Artifacts" );
        assertEquals( "a-1.0-one.jar;g=\"g\";a=\"a\";v=\"1.0\";c=\"one\","
            + "b-1.0-two.jar;g=\"g\";a=\"b\";v=\"1.0\";c=\"two\"," + "a-1.0.war;g=\"g\";a=\"a\";v=\"1.0\","
            + "b-1.0.jar;g=\"g\";a=\"b\";v=\"1.0\"", eas );
    }


    public void testEmbedDependencyMissingNegativeKey() throws Exception
    {
        ArtifactStubFactory artifactFactory = new ArtifactStubFactory( plugin.getOutputDirectory(), true );

        Set artifacts = new LinkedHashSet();

        artifacts.addAll( artifactFactory.getClassifiedArtifacts() );
        artifacts.addAll( artifactFactory.getScopedArtifacts() );
        artifacts.addAll(artifactFactory.getTypedArtifacts());

        MavenProject project = getMavenProjectStub();
        project.setDependencyArtifacts(artifacts);
        Properties props = new Properties();
        DependencyNode dependencyGraph = plugin.buildDependencyGraph(project);
        Jar[] classpath = plugin.getClasspath(project, dependencyGraph);

        Map instructions1 = new HashMap();
        instructions1.put( DependencyEmbedder.EMBED_DEPENDENCY, "!scope=compile" );
        Builder builder1 = plugin.buildOSGiBundle( project, dependencyGraph, instructions1, props, classpath );
        Manifest manifest1 = builder1.getJar().getManifest();

        Map instructions2 = new HashMap();
        instructions2.put( DependencyEmbedder.EMBED_DEPENDENCY, "scope=!compile" );
        Builder builder2 = plugin.buildOSGiBundle( project, dependencyGraph, instructions2, props, classpath );
        Manifest manifest2 = builder2.getJar().getManifest();

        String bcp1 = manifest1.getMainAttributes().getValue( Constants.BUNDLE_CLASSPATH );
        assertEquals( ".," + "provided-1.0.jar," + "test-1.0.jar," + "runtime-1.0.jar," + "system-1.0.jar", bcp1 );

        String eas1 = manifest1.getMainAttributes().getValue( "Embedded-Artifacts" );
        assertEquals( "provided-1.0.jar;g=\"g\";a=\"provided\";v=\"1.0\","
            + "test-1.0.jar;g=\"g\";a=\"test\";v=\"1.0\"," + "runtime-1.0.jar;g=\"g\";a=\"runtime\";v=\"1.0\","
            + "system-1.0.jar;g=\"g\";a=\"system\";v=\"1.0\"", eas1 );

        String bcp2 = manifest2.getMainAttributes().getValue( Constants.BUNDLE_CLASSPATH );
        assertEquals( bcp1, bcp2 );

        String eas2 = manifest2.getMainAttributes().getValue( "Embedded-Artifacts" );
        assertEquals( eas1, eas2 );
    }


    public void testPropertySanitizing() throws Exception
    {
        MavenProject project = getMavenProjectStub();

        Properties props = new Properties();

        props.put( new File( "A" ), new File( "B" ) );
        props.put( new int[4], new HashMap( 2 ) );
        props.put( Arrays.asList( 1, "two", 3.0 ), new float[5] );

        props.put( "A", new File( "B" ) );
        props.put( "4", new HashMap( 2 ) );
        props.put( "1, two, 3.0", new char[5] );

        DependencyNode dependencyGraph = plugin.buildDependencyGraph(project);
        Builder builder = plugin.getOSGiBuilder( project, new HashMap(), props, plugin.getClasspath( project, dependencyGraph ) );

        File file = new File( getBasedir(), "target" + File.separatorChar + "test.props" );
        builder.getProperties().store( new FileOutputStream( file ), "TEST" );
    }
}
