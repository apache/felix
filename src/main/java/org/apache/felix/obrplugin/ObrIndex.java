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
package org.apache.felix.obrplugin;


import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.RepositoryImpl;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * Index the content of a maven repository using OBR
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Mojo( name = "index", requiresProject = false )
public final class ObrIndex extends AbstractMojo
{

    /**
     * OBR Repository.
     */
    @Parameter( property = "obrRepository" )
    private String obrRepository;

    /**
     * Template for urls
     */
    @Parameter( property = "urlTemplate" )
    private String urlTemplate;

    /**
     * The repository to index
     */
    @Parameter( property = "mavenRepository" )
    private String mavenRepository;

    /**
     * Local Repository.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;


    public void execute() throws MojoExecutionException
    {
        Log log = getLog();
        try
        {
            log.info( "Indexing..." );

            String repo = mavenRepository;
            if ( repo == null )
            {
                repo = localRepository.getBasedir();
            }
            URI mavenRepoUri = new File( repo ).toURI();

            URI repositoryXml = ObrUtils.findRepositoryXml( repo, obrRepository );

            log.info( "Repository:   " + mavenRepoUri );
            log.info( "OBR xml:      " + repositoryXml );
            log.info( "URL template: " + urlTemplate );

            List<File> files = new ArrayList<File>();
            findAllJars( new File( repo ), files );

            DataModelHelperImpl dmh = new DataModelHelperImpl();
            RepositoryImpl repository;

            File obrRepoFile = new File( repositoryXml );
            if ( obrRepoFile.isFile() )
            {
                repository = ( RepositoryImpl ) dmh.repository( repositoryXml.toURL() );
            }
            else
            {
                repository = new RepositoryImpl();
            }

            for ( File file : files )
            {
                try
                {
                    ResourceImpl resource = ( ResourceImpl ) dmh.createResource( file.toURI().toURL() );
                    if ( resource != null )
                    {
                        repository.addResource( resource );
                        doTemplate( mavenRepoUri, file, resource );
                        log.info( "Adding resource: " + file );
                    }
                    else
                    {
                        log.info( "Ignoring non OSGi bundle: " + file );
                    }
                }
                catch ( Exception e )
                {
                    log.warn( "Error processing bundle: " + file + " " + e.getMessage() );
                }
            }
            Writer writer = new FileWriter( obrRepoFile );
            try
            {
                dmh.writeRepository( repository, writer );
            }
            finally
            {
                writer.close();
            }
        }
        catch ( Exception e )
        {
            log.warn( "Exception while updating local OBR: " + e.getLocalizedMessage(), e );
        }
    }


    protected void doTemplate( URI root, File path, ResourceImpl resource ) throws IOException, URISyntaxException
    {
        path = path.getAbsoluteFile().getCanonicalFile();
        String finalUri = root.relativize( path.toURI() ).toString();
        if ( "maven".equals( urlTemplate ) )
        {
            String dir = root.relativize( path.toURI() ).toString();
            String[] p = dir.split( "/" );
            if ( p.length >= 4 && p[p.length - 1].startsWith( p[p.length - 3] + "-" + p[p.length - 2] ) )
            {
                String artifactId = p[p.length - 3];
                String version = p[p.length - 2];
                String classifier;
                String type;
                String artifactIdVersion = artifactId + "-" + version;
                StringBuffer sb = new StringBuffer();
                if ( p[p.length - 1].charAt( artifactIdVersion.length() ) == '-' )
                {
                    classifier = p[p.length - 1].substring( artifactIdVersion.length() + 1,
                        p[p.length - 1].lastIndexOf( '.' ) );
                }
                else
                {
                    classifier = null;
                }
                type = p[p.length - 1].substring( p[p.length - 1].lastIndexOf( '.' ) + 1 );
                sb.append( "mvn:" );
                for ( int j = 0; j < p.length - 3; j++ )
                {
                    if ( j > 0 )
                    {
                        sb.append( '.' );
                    }
                    sb.append( p[j] );
                }
                sb.append( '/' ).append( artifactId ).append( '/' ).append( version );
                if ( !"jar".equals( type ) || classifier != null )
                {
                    sb.append( '/' );
                    if ( !"jar".equals( type ) )
                    {
                        sb.append( type );
                    }
                    if ( classifier != null )
                    {
                        sb.append( '/' ).append( classifier );
                    }
                }
                finalUri = sb.toString();
            }
        }
        else if ( urlTemplate != null )
        {
            URI parentDir = path.getParentFile().toURI();

            String absoluteDir = trim( root.toString(), parentDir.toURL().toString() );
            String relativeDir = trim( root.toString(), root.relativize( parentDir ).toString() );

            String url = urlTemplate.replaceAll( "%v", "" + resource.getVersion() );
            url = url.replaceAll( "%s", resource.getSymbolicName() );
            url = url.replaceAll( "%f", path.getName() );
            url = url.replaceAll( "%p", absoluteDir );
            url = url.replaceAll( "%rp", relativeDir );
            finalUri = url;
        }
        resource.put( Resource.URI, finalUri, Property.URI );
    }


    private String trim( String prefix, String path )
    {
        if ( path.endsWith( "/" ) )
            path = path.substring( 0, path.length() - 1 );

        if ( path.startsWith( prefix ) )
            path = path.substring( prefix.length() );

        return path;
    }


    private final FileFilter filter = new FileFilter()
    {
        public boolean accept( File pathname )
        {
            return pathname.getName().endsWith( "ar" );
        }
    };


    private void findAllJars( File mainRoot, List<File> files )
    {
        List<File> roots = new ArrayList<File>();
        roots.add( mainRoot );
        while ( !roots.isEmpty() )
        {
            File root = roots.remove( 0 );
            File[] children = root.listFiles();
            if ( children != null )
            {
                for ( File child : children )
                {
                    if ( child.isFile() && filter.accept( child ) )
                    {
                        files.add( child );
                    }
                    else if ( child.isDirectory() )
                    {
                        roots.add( child );
                    }
                }
            }
        }
    }

}
