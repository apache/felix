package org.apache.maven.shared.osgi;

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

import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test for {@link DefaultMaven2OsgiConverter}
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class Maven2OsgiConverterTest
    extends PlexusTestCase
{

    private Maven2OsgiConverter maven2Osgi = new DefaultMaven2OsgiConverter();

    public void testGetBundleSymbolicName()
    {
        ArtifactStub artifact = getTestArtifact();
        String s;
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "org.apache.commons.logging", s );

        artifact.setGroupId( "org.apache.commons" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "org.apache.commons.logging", s );

        artifact.setGroupId( "org.apache.commons.commons-logging" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "org.apache.commons.commons-logging", s );

        artifact.setGroupId( "org.apache" );
        artifact.setArtifactId( "org.apache.commons-logging" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "org.apache.commons-logging", s );

        artifact.setFile( getTestFile( "junit-3.8.2.jar" ) );
        artifact.setGroupId( "junit" );
        artifact.setArtifactId( "junit" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "junit", s );

        artifact.setFile( getTestFile( "xml-apis-1.0.b2.jar" ) );
        artifact.setGroupId( "xml-apis" );
        artifact.setArtifactId( "a" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "xml-apis.a", s );

        artifact.setFile( getTestFile( "test-1.jar" ) );
        artifact.setGroupId( "test" );
        artifact.setArtifactId( "test" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "test", s );

        artifact.setFile( getTestFile( "xercesImpl-2.6.2.jar" ) );
        artifact.setGroupId( "xerces" );
        artifact.setArtifactId( "xercesImpl" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "xerces.Impl", s );

        artifact.setFile( getTestFile( "aopalliance-1.0.jar" ) );
        artifact.setGroupId( "org.aopalliance" );
        artifact.setArtifactId( "aopalliance" );
        s = maven2Osgi.getBundleSymbolicName( artifact );
        assertEquals( "org.aopalliance", s );
    }

    public void testGetBundleFileName()
    {
        ArtifactStub artifact = getTestArtifact();
        String s;
        s = maven2Osgi.getBundleFileName( artifact );
        assertEquals( "org.apache.commons.logging_1.1.0.jar", s );

        artifact.setGroupId( "org.aopalliance" );
        artifact.setArtifactId( "aopalliance" );
        s = maven2Osgi.getBundleFileName( artifact );
        assertEquals( "org.aopalliance_1.1.0.jar", s );
    }

    public void testGetVersion()
    {
        ArtifactStub artifact = getTestArtifact();
        String s = maven2Osgi.getVersion( artifact );
        assertEquals( "1.1.0", s );
    }

    public void testConvertVersionToOsgi()
    {
        String osgiVersion;

        osgiVersion = maven2Osgi.getVersion( "2.1.0-SNAPSHOT" );
        assertEquals( "2.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "2.1-SNAPSHOT" );
        assertEquals( "2.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "0.1-SNAPSHOT" );
        assertEquals( "0.1.0.SNAPSHOT", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "2-SNAPSHOT" );
        assertEquals( "2.0.0.SNAPSHOT", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "2" );
        assertEquals( "2.0.0", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "2.1" );
        assertEquals( "2.1.0", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "2.1.3" );
        assertEquals( "2.1.3", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "2.1.3.4" );
        assertEquals( "2.1.3.4", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "1.1-alpha-2" );
        assertEquals( "1.1.0.alpha-2", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "1.0-alpha-16-20070122.203121-13" );
        assertEquals( "1.0.0.alpha-16-20070122_203121-13", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "1.0-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432-1", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "1-20070119.021432-1" );
        assertEquals( "1.0.0.20070119_021432-1", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "1.4.1-20070217.082013-7" );
        assertEquals( "1.4.1.20070217_082013-7", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "0.0.0.4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", osgiVersion );

        osgiVersion = maven2Osgi.getVersion( "4aug2000r7-dev" );
        assertEquals( "0.0.0.4aug2000r7-dev", osgiVersion );
    }

    private ArtifactStub getTestArtifact()
    {
        ArtifactStub a = new ArtifactStub();
        a.setGroupId( "commons-logging" );
        a.setArtifactId( "commons-logging" );
        a.setVersion( "1.1" );
        a.setFile( getTestFile( "commons-logging-1.1.jar" ) );
        return a;
    }

    public static File getTestFile( String fileName )
    {
        return PlexusTestCase.getTestFile( "src/test/resources/" + fileName );
    }
}
