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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.SCRDescriptorGenerator;
import org.apache.felix.scrplugin.Source;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;


/**
 * The <code>SCRDescriptorTask</code> generates a service descriptor file based
 * on annotations found in the sources.
 */
public class SCRDescriptorTask extends MatchingTask {

    private File destdir;

    private Path classpath;

    /**
     * This flag controls the generation of the bind/unbind methods.
     */
    private boolean generateAccessors = true;

    /**
     * In strict mode the plugin even fails on warnings.
     */
    protected boolean strictMode = false;

    /**
     * Set to true to scan classes instead of sources.
     * By default scan sources to be backwards compatible
     */
    private boolean scanClasses = false;

    /**
     * The version of the DS spec this plugin generates a descriptor for. By
     * default the version is detected by the used tags.
     *
     * @parameter
     */
    private String specVersion;


    @Override
    public void execute() throws BuildException {

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
        scrLog.debug( "  generateAccessors: " + generateAccessors );
        scrLog.debug( "  strictMode: " + strictMode );
        scrLog.debug( "  specVersion: " + specVersion );

        try {
            final Path classPath = createClasspath();
            final org.apache.felix.scrplugin.Project project = new org.apache.felix.scrplugin.Project();
            project.setClassLoader(getClassLoader( this.getClass().getClassLoader() ));

            project.setDependencies(getDependencies(classPath));
            project.setSources(getSourceFiles(getImplicitFileSet()));
            project.setClassesDirectory(destdir.getAbsolutePath());

            // create options
            final Options options = new Options();
            options.setOutputDirectory(destdir);
            options.setGenerateAccessors(generateAccessors);
            options.setStrictMode(strictMode);
            options.setProperties(new HashMap<String, String>());
            options.setSpecVersion(SpecVersion.fromName(specVersion));
            if ( specVersion != null && options.getSpecVersion() == null ) {
                throw new BuildException("Unknown spec version specified: " + specVersion);
            }

            final SCRDescriptorGenerator generator = new SCRDescriptorGenerator( scrLog );

            // setup from plugin configuration
            generator.setOptions(options);
            generator.setProject(project);

            generator.execute();
        } catch ( final SCRDescriptorException sde ) {
            if ( sde.getSourceLocation() != null )  {
                final Location loc = new Location( sde.getSourceLocation(), -1, 0 );
                throw new BuildException( sde.getMessage(), sde.getCause(), loc );
            }
            throw new BuildException( sde.getMessage(), sde.getCause() );
        } catch ( SCRDescriptorFailureException sdfe ) {
            throw new BuildException( sdfe.getMessage(), sdfe.getCause() );
        }
    }

    protected Collection<Source> getSourceFiles(final FileSet sourceFiles) {
        final String prefix = sourceFiles.getDir().getAbsolutePath();
        final int prefixLength = prefix.length() + 1;

        final List<Source> result = new ArrayList<Source>();
        @SuppressWarnings("unchecked")
        final Iterator<Resource> resources = sourceFiles.iterator();

        final String ext;
        if(scanClasses) {
            ext = ".class";
        } else {
            ext = ".java";
        }

        while ( resources.hasNext() ) {
            final Resource r = resources.next();
            if ( r instanceof FileResource ) {
                final File file = ( ( FileResource ) r ).getFile();

                if ( file.getName().endsWith(ext) ) {
                    result.add(new Source() {

                        public File getFile() {
                            return file;
                        }

                        public String getClassName() {
                            String name = file.getAbsolutePath().substring(prefixLength).replace(File.separatorChar, '/').replace('/', '.');
                            return name.substring(0, name.length() - ext.length());
                        }
                    });
                }
            }
        }

        return result;
    }


    private List<File> getDependencies(final Path classPath) {
        ArrayList<File> files = new ArrayList<File>();
        for ( String entry : classPath.list() ) {
            File file = new File( entry );
            if ( file.isFile() ) {
                files.add( file );
            }
        }
        return files;
    }

    private ClassLoader getClassLoader( final ClassLoader parent ) throws BuildException {
        Path classPath = createClasspath();
        log( "Using classes from: " + classPath, Project.MSG_DEBUG );
        return getProject().createClassLoader( parent, classpath );
    }


    // ---------- setters for configuration fields

    public Path createClasspath() {
        if ( this.classpath == null ) {
            this.classpath = new Path( getProject() );
        }
        return this.classpath;
    }

    public void setClasspath( Path classPath ) {
        createClasspath().add( classPath );
    }

    public void setClasspathRef( Reference classpathRef ) {
        if ( classpathRef != null && classpathRef.getReferencedObject() instanceof Path ) {
            createClasspath().add( ( Path ) classpathRef.getReferencedObject() );
        }
    }

    public void setSrcdir( File srcdir )  {
        getImplicitFileSet().setDir( srcdir );
    }

    public void setDestdir( File outputDirectory ) {
        this.destdir = outputDirectory;
        if ( destdir != null ) {
            Path dst = new Path( getProject() );
            dst.setLocation( destdir );
            createClasspath().add( dst );
        }
    }

    public void setGenerateAccessors( boolean generateAccessors ) {
        this.generateAccessors = generateAccessors;
    }

    public void setStrictMode( boolean strictMode ) {
        this.strictMode = strictMode;
    }

    public void setSpecVersion( String specVersion ) {
        this.specVersion = specVersion;
    }

    public boolean isScanClasses() {
        return scanClasses;
    }

    public void setScanClasses(boolean scanClasses) {
        this.scanClasses = scanClasses;
    }
}
