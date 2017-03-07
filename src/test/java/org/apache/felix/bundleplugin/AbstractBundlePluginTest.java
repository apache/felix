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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuilderConfiguration;


/**
 * Common methods for bundle plugin testing
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public abstract class AbstractBundlePluginTest extends AbstractMojoTestCase
{

    protected MavenProjectStub getMavenProjectStub()
    {
        MavenProjectStub project = new MavenProjectStub();
        project.setGroupId("group");
        project.setArtifactId("project");
        project.setVersion( "1.2.3.4" );

        VersionRange versionRange = VersionRange.createFromVersion( project.getVersion() );
        ArtifactHandler artifactHandler = new DefaultArtifactHandler("pom");
        Artifact artifact =
            new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                                 versionRange, null, "pom", null, artifactHandler );
        artifact.setResolved( true );
        project.setArtifact( artifact );
        ProjectBuilderConfiguration projectBuilderConfiguration = new DefaultProjectBuilderConfiguration();
        ArtifactRepositoryLayout layout = new LegacyRepositoryLayout();
        ArtifactRepository artifactRepository = new DefaultArtifactRepository( "scratch", new File( getBasedir(), "target" + File.separatorChar + "scratch" ).toURI().toString(), layout );
        projectBuilderConfiguration.setLocalRepository( artifactRepository );
        project.setProjectBuilderConfiguration( projectBuilderConfiguration );
        return project;
    }


    protected ArtifactStub getArtifactStub()
    {
        ArtifactStub artifact = new ArtifactStub();
        artifact.setGroupId( "group" );
        artifact.setArtifactId( "artifact" );
        artifact.setVersion( "1.0" );
        return artifact;
    }


    protected File getTestBundle()
    {
        String osgiBundleFileName = "org.apache.maven.maven-model_2.1.0.SNAPSHOT.jar";
        return getTestFile( getBasedir(), "src/test/resources/" + osgiBundleFileName );
    }

}