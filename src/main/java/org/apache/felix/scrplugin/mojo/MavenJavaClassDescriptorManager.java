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
import java.util.*;

import org.apache.felix.scrplugin.*;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.om.Component;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;


public class MavenJavaClassDescriptorManager extends JavaClassDescriptorManager
{

    private final MavenProject project;

    private final String excludeString;

    /** The component definitions from other bundles hashed by classname. */
    private Map<String, Component> componentDescriptions;


    public MavenJavaClassDescriptorManager( MavenProject project, Log log, ClassLoader classLoader,
        String[] annotationTagProviders, String excludeString, boolean parseJavadocs, boolean processAnnotations )
        throws SCRDescriptorFailureException
    {
        super( log, classLoader, annotationTagProviders, parseJavadocs, processAnnotations );

        this.project = project;
        this.excludeString = excludeString;
    }


    public String getOutputDirectory()
    {
        return this.project.getBuild().getOutputDirectory();
    }


    @Override
    protected Iterator<File> getSourceFiles()
    {
        ArrayList<File> files = new ArrayList<File>();

        @SuppressWarnings("unchecked")
        final Iterator<String> i = project.getCompileSourceRoots().iterator();

        // FELIX-509: check for excludes
        final String[] includes = new String[] { "**/*.java" };
        final String[] excludes;
        if ( excludeString != null )
        {
            excludes = StringUtils.split( excludeString, "," );
        }
        else
        {
            excludes = null;
        }

        while ( i.hasNext() )
        {
            final String tree = i.next();
            final File directory = new File( tree );
            if ( !directory.exists() )
            {
                this.log.warn("Source tree does not exist. Ignoring " + tree);
                continue;
            }
            this.log.debug( "Scanning source tree " + tree );
            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( directory );

            if ( excludes != null && excludes.length > 0 )
            {
                scanner.setExcludes( excludes );
            }
            scanner.addDefaultExcludes();
            scanner.setIncludes( includes );

            scanner.scan();

            for ( String fileName : scanner.getIncludedFiles() )
            {
                files.add( new File( directory, fileName ) );

            }
        }

        return files.iterator();
    }


    @Override
    protected List<File> getDependencies()
    {
        ArrayList<File> dependencies = new ArrayList<File>();

        @SuppressWarnings("unchecked")
        final Map<String, Artifact> resolved = project.getArtifactMap();
        final Iterator<Artifact> it = resolved.values().iterator();
        while ( it.hasNext() )
        {
            final Artifact declared = it.next();
            this.log.debug( "Checking artifact " + declared );
            if ( this.isJavaArtifact( declared ) )
            {
                if ( Artifact.SCOPE_COMPILE.equals( declared.getScope() )
                    || Artifact.SCOPE_RUNTIME.equals( declared.getScope() )
                    || Artifact.SCOPE_PROVIDED.equals( declared.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( declared.getScope() ) )
                {
                    this.log.debug( "Resolving artifact " + declared );
                    final Artifact artifact = resolved.get( ArtifactUtils.versionlessKey( declared ) );
                    if ( artifact != null )
                    {
                        dependencies.add( artifact.getFile() );
                    }
                    else
                    {
                        this.log.debug( "Unable to resolve artifact " + declared );
                    }
                }
                else
                {
                    this.log.debug( "Artifact " + declared + " has not scope compile or runtime, but "
                        + declared.getScope() );
                }
            }
            else
            {
                this.log.debug( "Artifact " + declared + " is not a java artifact, type is " + declared.getType() );
            }
        }

        return dependencies;
    }


    /**
     * Check if the artifact is a java artifact (jar or bundle)
     */
    private boolean isJavaArtifact( Artifact artifact )
    {
        if ( "jar".equals( artifact.getType() ) )
        {
            return true;
        }
        if ( "bundle".equals( artifact.getType() ) )
        {
            return true;
        }
        return false;
    }

}
