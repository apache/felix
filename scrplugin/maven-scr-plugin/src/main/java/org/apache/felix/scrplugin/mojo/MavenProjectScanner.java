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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;


public class MavenProjectScanner {

	private enum ScanKind {
		ADDED_OR_UPDATED, DELETED;
	}

    private final MavenProject project;

    private final String includeString;

    private final String excludeString;

    private final boolean scanClasses;
    private final Log log;

    private final BuildContext buildContext;

    public MavenProjectScanner( final BuildContext buildContext,
            final MavenProject project,
            final String includeString,
            final String excludeString,
            final boolean scanClasses,
            final Log log) {
        this.project = project;
        this.includeString = includeString;
        this.excludeString = excludeString;
        this.scanClasses = scanClasses;
        this.log = log;
        this.buildContext = buildContext;
    }

    /**
     * Return all sources.
     */
    public Collection<Source> getSources() {
    	if ( scanClasses ) {
            final ArrayList<Source> files = new ArrayList<Source>();

            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(this.project.getBuild().getOutputDirectory());
            if ( this.includeString != null ) {
                scanner.setIncludes(this.includeString.split(","));
            } else {
                scanner.setIncludes(new String[] {"**/*.class"});
            }
            if ( this.excludeString != null ) {
                scanner.setExcludes(this.excludeString.split(","));
            }
            scanner.addDefaultExcludes();

            scanner.scan();

            for ( final String fileName : scanner.getIncludedFiles() ) {
                files.add( new Source() {

                    public File getFile() {
                        return new File(project.getBuild().getOutputDirectory(), fileName);
                    }

                    public String getClassName() {
                        // remove ".class"
                        String name = fileName.substring(0, fileName.length() - 6);
                        return name.replace(File.separatorChar, '/').replace('/', '.');
                    }
                });
            }

    	    return files;
    	}
    	return getSourcesForScanKind(ScanKind.ADDED_OR_UPDATED);
    }

	private Collection<Source> getSourcesForScanKind(ScanKind scanKind)
			throws AssertionError {
		final ArrayList<Source> files = new ArrayList<Source>();

        @SuppressWarnings("unchecked")
        final Iterator<String> i = project.getCompileSourceRoots().iterator();

        // FELIX-509: check for excludes
        String[] includes = new String[] { "**/*.java" };
        if ( includeString != null ) {
        	includes = includeString.split( "," );
        }

        final String[] excludes;
        if ( excludeString != null ) {
            excludes = excludeString.split( "," );
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

            final Scanner scanner;
            switch ( scanKind ) {

            	case ADDED_OR_UPDATED:
            		scanner = this.buildContext.newScanner(directory, false);
            		break;

            	case DELETED:
            		scanner = this.buildContext.newDeleteScanner(directory);
            		break;

            	default:
            		throw new AssertionError("Unhandled ScanKind " + scanKind);

            }


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
	 * Returns all sources which were deleted since the previous build
	 *
	 * @return the deleted sources
	 */
    public Collection<Source> getDeletedSources() {

    	return getSourcesForScanKind(ScanKind.DELETED);
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
