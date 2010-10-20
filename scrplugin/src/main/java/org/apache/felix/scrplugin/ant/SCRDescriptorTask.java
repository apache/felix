/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.felix.scrplugin.ant;


import java.io.File;
import java.util.*;

import org.apache.felix.scrplugin.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;


/**
 * The <code>SCRDescriptorTask</code> generates a service descriptor file based
 * on annotations found in the sources.
 */
public class SCRDescriptorTask extends MatchingTask
{

    private File destdir;

    private Path classpath;

    /**
     * Name of the generated descriptor.
     */
    private String finalName = "serviceComponents.xml";

    /**
     * Name of the generated meta type file.
     */
    private String metaTypeName = "metatype.xml";

    /**
     * This flag controls the generation of the bind/unbind methods.
     */
    private boolean generateAccessors = true;

    /**
     * This flag controls whether the javadoc source code will be scanned for
     * tags.
     */
    protected boolean parseJavadoc = true;

    /**
     * This flag controls whether the annotations in the sources will be
     * processed.
     */
    protected boolean processAnnotations = true;

    /**
     * In strict mode the plugin even fails on warnings.
     */
    protected boolean strictMode = false;

    /**
     * Allows to define additional implementations of the interface
     * {@link org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider}
     * that provide mappings from custom annotations to
     * {@link org.apache.felix.scrplugin.tags.JavaTag} implementations. List of
     * full qualified class file names.
     *
     * @parameter
     */
    private String[] annotationTagProviders = {};

    /**
     * The version of the DS spec this plugin generates a descriptor for. By
     * default the version is detected by the used tags.
     *
     * @parameter
     */
    private String specVersion;


    @Override
    public void execute() throws BuildException
    {

        // ensure we know the source
        if (getImplicitFileSet().getDir() == null) {
            throw new BuildException( "srcdir attribute must be set!", getLocation());
        }

        // while debugging
        final org.apache.felix.scrplugin.Log scrLog = new AntLog( this );

        scrLog.debug( "SCRDescriptorTask Configuration" );
        scrLog.debug( "  implicitFileset: " + getImplicitFileSet() );
        scrLog.debug( "  outputDirectory: " + destdir );
        scrLog.debug( "  classpath: " + classpath );
        scrLog.debug( "  finalName: " + finalName );
        scrLog.debug( "  metaTypeName: " + metaTypeName );
        scrLog.debug( "  generateAccessors: " + generateAccessors );
        scrLog.debug( "  parseJavadoc: " + parseJavadoc );
        scrLog.debug( "  processAnnotations: " + processAnnotations );
        scrLog.debug( "  strictMode: " + strictMode );
        scrLog.debug( "  specVersion: " + specVersion );

        try
        {
            final ClassLoader classLoader = getClassLoader( this.getClass().getClassLoader() );
            final JavaClassDescriptorManager jManager = new AntJavaClassDescriptorManager( scrLog, classLoader,
                getImplicitFileSet(), destdir, createClasspath(), this.annotationTagProviders, this.parseJavadoc, this.processAnnotations );

            final SCRDescriptorGenerator generator = new SCRDescriptorGenerator( scrLog );

            // setup from plugin configuration
            generator.setOutputDirectory( destdir );
            generator.setDescriptorManager( jManager );
            generator.setFinalName( finalName );
            generator.setMetaTypeName( metaTypeName );
            generator.setGenerateAccessors( generateAccessors );
            generator.setStrictMode( strictMode );
            generator.setProperties( new HashMap<String, String>() );
            generator.setSpecVersion( specVersion );

            generator.execute();
        }
        catch ( SCRDescriptorException sde )
        {
            if ( sde.getSourceLocation() != null )
            {
                Location loc = new Location( sde.getSourceLocation(), sde.getLineNumber(), 0 );
                throw new BuildException( sde.getMessage(), sde.getCause(), loc );
            }
            throw new BuildException( sde.getMessage(), sde.getCause() );
        }
        catch ( SCRDescriptorFailureException sdfe )
        {
            throw new BuildException( sdfe.getMessage(), sdfe.getCause() );
        }
    }


    private ClassLoader getClassLoader( final ClassLoader parent ) throws BuildException
    {
        Path classPath = createClasspath();
        log( "Using classes from: " + classPath, Project.MSG_DEBUG );
        return getProject().createClassLoader( parent, classpath );
    }


    // ---------- setters for configuration fields

    public Path createClasspath()
    {
        if ( this.classpath == null )
        {
            this.classpath = new Path( getProject() );
        }
        return this.classpath;
    }


    public void setClasspath( Path classPath )
    {
        createClasspath().add( classPath );
    }


    public void setClasspathRef( Reference classpathRef )
    {
        if ( classpathRef != null && classpathRef.getReferencedObject() instanceof Path )
        {
            createClasspath().add( ( Path ) classpathRef.getReferencedObject() );
        }
    }


    public void setSrcdir( File srcdir )
    {
        getImplicitFileSet().setDir( srcdir );
    }


    public void setDestdir( File outputDirectory )
    {
        this.destdir = outputDirectory;
        if ( destdir != null )
        {
            Path dst = new Path( getProject() );
            dst.setLocation( destdir );
            createClasspath().add( dst );
        }
    }


    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    }


    public void setMetaTypeName( String metaTypeName )
    {
        this.metaTypeName = metaTypeName;
    }


    public void setGenerateAccessors( boolean generateAccessors )
    {
        this.generateAccessors = generateAccessors;
    }


    public void setParseJavadoc( boolean parseJavadoc )
    {
        this.parseJavadoc = parseJavadoc;
    }


    public void setProcessAnnotations( boolean processAnnotations )
    {
        this.processAnnotations = processAnnotations;
    }


    public void setStrictMode( boolean strictMode )
    {
        this.strictMode = strictMode;
    }


    public void setAnnotationTagProviders( String[] annotationTagProviders )
    {
        this.annotationTagProviders = annotationTagProviders;
    }


    public void setSpecVersion( String specVersion )
    {
        this.specVersion = specVersion;
    }

}
