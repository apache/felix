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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Resource;


/**
 * Generate an OSGi manifest for this project
 * 
 * @goal manifest
 * @phase process-classes
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class ManifestPlugin extends BundlePlugin
{
    /**
     * When true, generate the manifest by rebuilding the full bundle in memory 
     *
     * @parameter expression="${rebuildBundle}"
     */
    protected boolean rebuildBundle;


    @Override
    protected void execute( MavenProject project, Map instructions, Properties properties, Jar[] classpath )
        throws MojoExecutionException
    {
        Manifest manifest;
        try
        {
            manifest = getManifest( project, instructions, properties, classpath );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( "Cannot find " + e.getMessage()
                + " (manifest goal must be run after compile phase)", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error trying to generate Manifest", e );
        }
        catch ( MojoFailureException e )
        {
            getLog().error( e.getLocalizedMessage() );
            throw new MojoExecutionException( "Error(s) found in manifest configuration", e );
        }
        catch ( Exception e )
        {
            getLog().error( "An internal error occurred", e );
            throw new MojoExecutionException( "Internal error in maven-bundle-plugin", e );
        }

        File outputFile = new File( manifestLocation, "MANIFEST.MF" );

        try
        {
            writeManifest( manifest, outputFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error trying to write Manifest to file " + outputFile, e );
        }
    }


    public Manifest getManifest( MavenProject project, Jar[] classpath ) throws IOException, MojoFailureException,
        MojoExecutionException, Exception
    {
        return getManifest( project, new LinkedHashMap(), new Properties(), classpath );
    }


    public Manifest getManifest( MavenProject project, Map instructions, Properties properties, Jar[] classpath )
        throws IOException, MojoFailureException, MojoExecutionException, Exception
    {
        Analyzer analyzer = getAnalyzer( project, instructions, properties, classpath );
        boolean hasErrors = reportErrors( "Manifest " + project.getArtifact(), analyzer );
        if ( hasErrors )
        {
            String failok = analyzer.getProperty( "-failok" );
            if ( null == failok || "false".equalsIgnoreCase( failok ) )
            {
                throw new MojoFailureException( "Error(s) found in manifest configuration" );
            }
        }

        Jar jar = analyzer.getJar();

        if ( unpackBundle )
        {
            File outputFile = getOutputDirectory();
            for ( Entry<String, Resource> entry : jar.getResources().entrySet() )
            {
                File entryFile = new File( outputFile, entry.getKey() );
                if ( !entryFile.exists() || entry.getValue().lastModified() == 0 )
                {
                    entryFile.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream( entryFile );
                    entry.getValue().write( os );
                    os.close();
                }
            }
        }

        Manifest manifest = jar.getManifest();

        // cleanup...
        analyzer.close();

        return manifest;
    }


    protected Analyzer getAnalyzer( MavenProject project, Jar[] classpath ) throws IOException, MojoExecutionException,
        Exception
    {
        return getAnalyzer( project, new LinkedHashMap(), new Properties(), classpath );
    }


    protected Analyzer getAnalyzer( MavenProject project, Map instructions, Properties properties, Jar[] classpath )
        throws IOException, MojoExecutionException, Exception
    {
        if ( rebuildBundle && supportedProjectTypes.contains( project.getArtifact().getType() ) )
        {
            return buildOSGiBundle( project, instructions, properties, classpath );
        }

        File file = project.getArtifact().getFile();
        if ( file == null )
        {
            file = getOutputDirectory();
        }

        if ( !file.exists() )
        {
            throw new FileNotFoundException( file.getPath() );
        }

        Builder analyzer = getOSGiBuilder( project, instructions, properties, classpath );

        analyzer.setJar( file );

        if ( analyzer.getProperty( Analyzer.EXPORT_PACKAGE ) == null
            && analyzer.getProperty( Analyzer.EXPORT_CONTENTS ) == null
            && analyzer.getProperty( Analyzer.PRIVATE_PACKAGE ) == null )
        {
            String export = analyzer.calculateExportsFromContents( analyzer.getJar() );
            analyzer.setProperty( Analyzer.EXPORT_PACKAGE, export );
        }

        addMavenInstructions( project, analyzer );

        analyzer.mergeManifest( analyzer.getJar().getManifest() );
        analyzer.calcManifest();

        mergeMavenManifest( project, analyzer );

        return analyzer;
    }


    public static void writeManifest( Manifest manifest, File outputFile ) throws IOException
    {
        outputFile.getParentFile().mkdirs();

        FileOutputStream os;
        os = new FileOutputStream( outputFile );
        try
        {
            Jar.writeManifest( manifest, os );
        }
        finally
        {
            try
            {
                os.close();
            }
            catch ( IOException e )
            {
                // nothing we can do here
            }
        }
    }
}
