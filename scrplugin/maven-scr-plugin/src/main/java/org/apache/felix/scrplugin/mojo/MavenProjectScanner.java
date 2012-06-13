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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.Source;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;


public class MavenProjectScanner {

    private final MavenProject project;

    private final String includeString;

    private final String excludeString;

    private final Log log;

    public MavenProjectScanner( final MavenProject project,
            final String includeString,
            final String excludeString,
            final Log log) {
        this.project = project;
        this.includeString = includeString;
        this.excludeString = excludeString;
        this.log = log;
    }

    /**
     * Return all sources.
     */
    public Collection<Source> getSources() {
        final ArrayList<Source> files = new ArrayList<Source>();

        @SuppressWarnings("unchecked")
        final Iterator<String> i = project.getCompileSourceRoots().iterator();

        // FELIX-509: check for excludes
        String[] includes = new String[] { "**/*.java" };
        if ( includeString != null ) {
        	includes = StringUtils.split( includeString, "," );
        }

        final String[] excludes;
        if ( excludeString != null ) {
            excludes = StringUtils.split( excludeString, "," );
        } else {
            excludes = null;
        }

        while ( i.hasNext() ) {
            final String tree = i.next();
            final File directory = new File( tree );
            if ( !directory.exists() ) {
                log.warn("Source tree does not exist. Ignoring " + tree);
                continue;
            }
            log.debug( "Scanning source tree " + tree );
            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( directory );

            if ( excludes != null && excludes.length > 0 ) {
                scanner.setExcludes( excludes );
            }
            scanner.addDefaultExcludes();
            scanner.setIncludes( includes );

            scanner.scan();

            for ( final String fileName : scanner.getIncludedFiles() ) {
                files.add( new Source() {

                    public File getFile() {
                        return new File(directory, fileName);
                    }

                    public String getClassName() {
                        // remove ".java"
                        String name = fileName.substring(0, fileName.length() - 5);
                        return name.replace(File.separatorChar, '/').replace('/', '.');
                    }
                });
            }
        }

        return files;
    }

    /**
     * Return all dependencies
     */
    public List<File> getDependencies() {
        final ArrayList<File> dependencies = new ArrayList<File>();

        @SuppressWarnings("unchecked")
        final Iterator<Artifact> it = project.getArtifacts().iterator();
        while ( it.hasNext() ) {
            final Artifact declared = it.next();
            this.log.debug( "Checking artifact " + declared );
            if ( this.isJavaArtifact( declared ) ) {
                if ( Artifact.SCOPE_COMPILE.equals( declared.getScope() )
                    || Artifact.SCOPE_RUNTIME.equals( declared.getScope() )
                    || Artifact.SCOPE_PROVIDED.equals( declared.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( declared.getScope() ) ) {
                    this.log.debug( "Resolving artifact " + declared );
                    if ( declared.getFile() != null ) {
                        dependencies.add( declared.getFile() );
                    } else {
                        this.log.debug( "Unable to resolve artifact " + declared );
                    }
                } else {
                    this.log.debug( "Artifact " + declared + " has not scope compile or runtime, but "
                        + declared.getScope() );
                }
            } else {
                this.log.debug( "Artifact " + declared + " is not a java artifact, type is " + declared.getType() );
            }
        }

        return dependencies;
    }


    /**
     * Check if the artifact is a java artifact (jar or bundle)
     */
    private boolean isJavaArtifact( Artifact artifact ) {
        if ( "jar".equals( artifact.getType() ) ) {
            return true;
        }
        if ( "bundle".equals( artifact.getType() ) ) {
            return true;
        }
        return false;
    }
}
