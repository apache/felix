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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import aQute.bnd.osgi.Jar;
import org.apache.maven.shared.dependency.graph.DependencyNode;


/**
 * Generate BND instructions for this project
 */
@Mojo( name = "instructions", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true  )
public class InstructionsPlugin extends BundlePlugin
{
    @Override
    protected void execute( MavenProject project, DependencyNode dependencyGraph, Map<String, String> instructions, Properties properties, Jar[] classpath )
        throws MojoExecutionException
    {
        if ( dumpInstructions == null )
        {
            dumpInstructions = new File( getBuildDirectory(), "instructions.bnd" );
        }

        try
        {
            addMavenInstructions( project, dependencyGraph, getOSGiBuilder(project, instructions, properties, classpath) );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Cannot find " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error trying to generate instructions", e );
        }
        catch ( MojoFailureException e )
        {
            getLog().error( e.getLocalizedMessage() );
            throw new MojoExecutionException( "Error(s) found in instructions", e );
        }
        catch ( Exception e )
        {
            getLog().error( "An internal error occurred", e );
            throw new MojoExecutionException( "Internal error in maven-bundle-plugin", e );
        }
    }
}
