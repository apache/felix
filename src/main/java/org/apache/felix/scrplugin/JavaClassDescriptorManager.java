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
package org.apache.felix.scrplugin;


import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.tags.JavaClassDescription;
import org.apache.felix.scrplugin.tags.JavaTag;
import org.apache.felix.scrplugin.tags.annotation.AnnotationJavaClassDescription;
import org.apache.felix.scrplugin.tags.annotation.AnnotationTagProviderManager;
import org.apache.felix.scrplugin.tags.cl.ClassLoaderJavaClassDescription;
import org.apache.felix.scrplugin.tags.qdox.QDoxJavaClassDescription;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;


/**
 * The <code>JavaClassDescriptorManager</code> must be implemented to provide
 * access to the java sources to be scanned for descriptor annotations and
 * JavaDoc tags, the descriptors of components from the class path and the
 * location of the generated class files to be able to add the bind and unbind
 * methods.
 */
public abstract class JavaClassDescriptorManager
{

    /** The maven log. */
    protected final Log log;

    /** The classloader used to compile the classes. */
    private final ClassLoader classloader;

    /** A cache containing the java class descriptions hashed by classname. */
    private final Map<String, JavaClassDescription> javaClassDescriptions = new HashMap<String, JavaClassDescription>();

    /**
     * Supports mapping of built-in and custom java anntoations to
     * {@link JavaTag} implementations.
     */
    private final AnnotationTagProviderManager annotationTagProviderManager;

    /** Parse Javadocs? */
    private final boolean parseJavadocs;

    /** Process Annotations? */
    private final boolean processAnnotations;

    /** The Java sources gathered by {@link #getSourceDescriptions()} */
    private JavaSource[] sources;

    /** The component definitions from other bundles hashed by classname. */
    private Map<String, Component> componentDescriptions;


    /**
     * Construct a new manager.
     *
     * @throws SCRDescriptorFailureException
     */
    public JavaClassDescriptorManager( final Log log, final ClassLoader classLoader,
        final String[] annotationTagProviders, final boolean parseJavadocs, final boolean processAnnotations )
        throws SCRDescriptorFailureException
    {
        this.processAnnotations = processAnnotations;
        this.parseJavadocs = parseJavadocs;
        this.log = log;
        this.annotationTagProviderManager = new AnnotationTagProviderManager( annotationTagProviders, classLoader );
        this.classloader = classLoader;
    }


    /**
     * Returns the QDox JavaSource instances representing the source files for
     * which the Declarative Services and Metatype descriptors have to be
     * generated.
     *
     * @throws SCRDescriptorException May be thrown if an error occurrs
     *             gathering the java sources.
     */
    protected JavaSource[] getSources() throws SCRDescriptorException
    {
        if ( this.sources == null )
        {
            this.log.debug( "Setting up QDox" );

            JavaDocBuilder builder = new JavaDocBuilder();
            builder.getClassLibrary().addClassLoader( this.getClassLoader() );

            final Iterator<File> i = getSourceFiles();
            while ( i.hasNext() )
            {
                File file = i.next();
                this.log.debug( "Adding source file " + file );
                try
                {
                    builder.addSource( file );
                }
                catch ( IOException e )
                {
                    // also FileNotFoundException
                    throw new SCRDescriptorException( "Unable to add source file", file.toString(), 0, e );
                }
            }
            this.sources = builder.getSources();
        }

        return this.sources;
    }


    /**
     * Returns an iterator of paths to directories providing Java source files
     * to be parsed.
     * <p>
     * This method is called by the default {@link #getSources()} implementation
     * to return the root directories for the Java files to be parsed. This
     * default implementation returns an empty iterator. Implementations of this
     * class not overwriting the {@link #getSources()} method should overwrite
     * this method by providing the concrete source locations.
     *
     * @return An iterator of Java source locations.
     */
    protected Iterator<File> getSourceFiles()
    {
        return Collections.<File> emptyList().iterator();
    }


    /**
     * Returns a map of component descriptors which may be extended by the java
     * sources returned by the {@link #getSources()} method.
     * <p>
     * This method calls the {@link #getDependencies()} method and checks for
     * any Service-Component descriptors in the returned files.
     * <p>
     * This method may be overwritten by extensions of this class.
     *
     * @throws SCRDescriptorException May be thrown if an error occurrs
     *             gethering the component descriptors.
     */
    protected Map<String, Component> getComponentDescriptors() throws SCRDescriptorException
    {
        if ( this.componentDescriptions == null )
        {
            final List<Component> components = new ArrayList<Component>();
            final List<File> dependencies = getDependencies();
            for ( File artifact : dependencies )
            {
                this.log.debug( "Trying to get manifest from artifact " + artifact );
                try
                {
                    final Manifest manifest = this.getManifest( artifact );
                    if ( manifest != null )
                    {
                        // read Service-Component entry
                        if ( manifest.getMainAttributes().getValue( Constants.SERVICE_COMPONENT ) != null )
                        {
                            final String serviceComponent = manifest.getMainAttributes().getValue(
                                Constants.SERVICE_COMPONENT );
                            this.log
                                .debug( "Found Service-Component: " + serviceComponent + " in artifact " + artifact );
                            final StringTokenizer st = new StringTokenizer( serviceComponent, "," );
                            while ( st.hasMoreTokens() )
                            {
                                final String entry = st.nextToken().trim();
                                if ( entry.length() > 0 )
                                {
                                    final Components c = this.readServiceComponentDescriptor( artifact, entry );
                                    if ( c != null )
                                    {
                                        components.addAll( c.getComponents() );
                                    }
                                }
                            }
                        }
                        else
                        {
                            this.log.debug( "Artifact has no service component entry in manifest " + artifact );
                        }
                    }
                    else
                    {
                        this.log.debug( "Unable to get manifest from artifact " + artifact );
                    }
                }
                catch ( IOException ioe )
                {
                    throw new SCRDescriptorException( "Unable to get manifest from artifact", artifact.toString(), 0,
                        ioe );
                }
                this.log.debug( "Trying to get scrinfo from artifact " + artifact );
                // now read the scr private file - components stored there
                // overwrite components already
                // read from the service component section.
                InputStream scrInfoFile = null;
                try
                {
                    scrInfoFile = this.getFile( artifact, Constants.ABSTRACT_DESCRIPTOR_ARCHIV_PATH );
                    if ( scrInfoFile != null )
                    {
                        components.addAll( this.parseServiceComponentDescriptor( scrInfoFile ).getComponents() );
                    }
                    else
                    {
                        this.log.debug( "Artifact has no scrinfo file (it's optional): " + artifact );
                    }
                }
                catch ( IOException ioe )
                {
                    throw new SCRDescriptorException( "Unable to get scrinfo from artifact", artifact.toString(), 0,
                        ioe );
                }
                finally
                {
                    if ( scrInfoFile != null )
                    {
                        try
                        {
                            scrInfoFile.close();
                        }
                        catch ( IOException ignore )
                        {
                        }
                    }
                }

            }
            // now create map with component descriptions
            this.componentDescriptions = new HashMap<String, Component>();
            for ( final Component component : components )
            {
                this.componentDescriptions.put( component.getImplementation().getClassame(), component );
            }
        }

        return this.componentDescriptions;
    }


    /**
     * Returns a list of files denoting dependencies of the module for which
     * descriptors are to be generated. The returned dependencies are expected
     * to be bundles which may (or may not) contain Service Component
     * descriptors (or internal descriptors in the case of abstract components
     * not listed in the "official" descriptors).
     * <p>
     * This method is called by the {@link #getComponentDescriptors()} method in
     * this class to get the list of bundles from where base component
     * descriptors are to be extracted.
     * <p>
     * Extensions of this class not overwriting the
     * {@link #getComponentDescriptors()} method should overwrite this method if
     * they wish to provide such base component descriptors.
     *
     * @return
     */
    protected List<File> getDependencies()
    {
        return Collections.<File> emptyList();
    }


    /**
     * Returns the absolute filesystem path to the directory where the classes
     * compiled from the java source files (see {@link #getSources()}) have been
     * placed.
     * <p>
     * This method is called to find the class files to which bind and unbind
     * methods are to be added.
     */
    public abstract String getOutputDirectory();


    /**
     * Return the log.
     */
    public Log getLog()
    {
        return this.log;
    }


    /**
     * Return the class laoder.
     */
    public ClassLoader getClassLoader()
    {
        return this.classloader;
    }


    /**
     * @return Annotation tag provider manager
     */
    public AnnotationTagProviderManager getAnnotationTagProviderManager()
    {
        return this.annotationTagProviderManager;
    }


    /**
     * Returns <code>true</code> if this class descriptor manager is parsing
     * JavaDoc tags.
     */
    public boolean isParseJavadocs()
    {
        return parseJavadocs;
    }


    /**
     * Returns <code>true</code> if this class descriptor manager is parsing
     * Java 5 annotations.
     */
    public boolean isProcessAnnotations()
    {
        return processAnnotations;
    }


    /**
     * Parses the descriptors read from the given input stream. This method may
     * be called by the {@link #getComponentDescriptors()} method to parse the
     * descriptors gathered in an implementation dependent way.
     *
     * @throws SCRDescriptorException If an error occurrs reading the
     *             descriptors from the stream.
     */
    protected Components parseServiceComponentDescriptor( InputStream file ) throws SCRDescriptorException
    {
        return ComponentDescriptorIO.read( file );
    }


    /**
     * Return all source descriptions of this project.
     *
     * @return All contained java class descriptions.
     */
    public JavaClassDescription[] getSourceDescriptions() throws SCRDescriptorException
    {
        final JavaClass[] javaClasses = getJavaClassesFromSources();
        final JavaClassDescription[] descs = new JavaClassDescription[javaClasses.length];
        for ( int i = 0; i < javaClasses.length; i++ )
        {
            descs[i] = this.getJavaClassDescription( javaClasses[i].getFullyQualifiedName() );
        }
        return descs;
    }

    private boolean doingHasScrPluginAnnotationCheck = false;

    private boolean hasScrPluginAnnotation(final Class<?> clazz, final JavaClass javaClass)
    {
        boolean result;
        doingHasScrPluginAnnotationCheck = true;

        result = getAnnotationTagProviderManager().hasScrPluginAnnotation( javaClass,
                new AnnotationJavaClassDescription( clazz, javaClass, this ));
        doingHasScrPluginAnnotationCheck = false;
        return result;
    }

    /**
     * Get a java class description for the class.
     *
     * @param className
     * @return The java class description.
     * @throws SCRDescriptorException
     */
    public JavaClassDescription getJavaClassDescription( String className ) throws SCRDescriptorException
    {
        JavaClassDescription result = this.javaClassDescriptions.get( className );
        if ( result == null )
        {
            this.log.debug( "Searching description for: " + className );
            int index = 0;
            final JavaClass[] javaClasses = getJavaClassesFromSources();
            while ( result == null && index < javaClasses.length )
            {
                final JavaClass javaClass = javaClasses[index];
                if ( javaClass.getFullyQualifiedName().equals( className ) )
                {
                    try
                    {
                        // check for java annotation descriptions - fallback to
                        // QDox if none found
                        Class<?> clazz = this.classloader.loadClass( className );
                        if ( this.processAnnotations && !doingHasScrPluginAnnotationCheck
                            && hasScrPluginAnnotation(clazz, javaClass) )
                        {
                            this.log.debug( "Generating java annotation description for: " + className );
                            result = new AnnotationJavaClassDescription( clazz, javaClass, this );
                        }
                        else if ( this.parseJavadocs )
                        {
                            this.log.debug( "Generating qdox description for: " + className );
                            result = new QDoxJavaClassDescription( clazz, javaClass, this );
                        }
                        else
                        {
                            index++;
                        }
                    }
                    catch ( ClassNotFoundException e )
                    {
                        throw new SCRDescriptorException( "Unable to load class", className, 0 );
                    }
                }
                else
                {
                    index++;
                }
            }
            if ( result == null )
            {
                try
                {
                    this.log.debug( "Generating classloader description for: " + className );
                    result = new ClassLoaderJavaClassDescription( this.classloader.loadClass( className ), this
                        .getComponentDescriptors().get( className ), this );
                }
                catch ( ClassNotFoundException e )
                {
                    throw new SCRDescriptorException( "Unable to load class", className, 0 );
                }
            }
            if ( !doingHasScrPluginAnnotationCheck ) {
                this.javaClassDescriptions.put( className, result );
            }
        }
        return result;
    }


    /**
     * Get a list of all {@link JavaClass} definitions four all source files
     * (including nested/inner classes)
     *
     * @return List of {@link JavaClass} definitions
     */
    private JavaClass[] getJavaClassesFromSources() throws SCRDescriptorException
    {
        final JavaSource[] sources = this.getSources();
        final List<JavaClass> classes = new ArrayList<JavaClass>();
        for ( int i = 0; i < sources.length; i++ )
        {
            if ( sources[i].getClasses() == null || sources[i].getClasses().length == 0 )
            {
                continue;
            }
            for ( int j = 0; j < sources[i].getClasses().length; j++ )
            {
                final JavaClass clazz = sources[i].getClasses()[j];
                classes.add( clazz );
                for ( int k = 0; k < clazz.getNestedClasses().length; k++ )
                {
                    final JavaClass nestedClass = clazz.getNestedClasses()[k];
                    classes.add( nestedClass );
                }
            }
        }
        return classes.toArray( new JavaClass[classes.size()] );
    }


    /**
     * Read the service component description.
     *
     * @param artifact
     * @param entry
     * @throws IOException
     * @throws SCRDescriptorException
     */
    private Components readServiceComponentDescriptor( final File artifactFile, String entry )
    {
        this.log.debug( "Reading " + entry + " from " + artifactFile );
        InputStream xml = null;
        try
        {
            xml = this.getFile( artifactFile, entry );
            if ( xml == null )
            {
                throw new SCRDescriptorException( "Entry " + entry + " not contained in JAR File ", artifactFile.toString(),
                    0 );
            }
            return this.parseServiceComponentDescriptor( xml );
        }
        catch ( IOException mee )
        {
            this.log.warn( "Unable to read SCR descriptor file from JAR File " + artifactFile + " at " + entry );
            this.log.debug( "Exception occurred during reading: " + mee.getMessage(), mee );
        }
        catch ( SCRDescriptorException mee )
        {
            this.log.warn( "Unable to read SCR descriptor file from JAR File " + artifactFile + " at " + entry );
            this.log.debug( "Exception occurred during reading: " + mee.getMessage(), mee );
        }
        finally
        {
            if ( xml != null )
            {
                try
                {
                    xml.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
        return null;
    }


    private Manifest getManifest( File artifact ) throws IOException
    {
        if ( artifact.isDirectory() )
        {
            // this is maybe a classes directory, try to read manifest file directly
            final File dir = new File(artifact, "META-INF");
            if ( !dir.exists() || !dir.isDirectory() )
            {
                return null;
            }
            final File mf = new File(dir, "MANIFEST.MF");
            if ( !mf.exists() || !mf.isFile() )
            {
                return null;
            }
            final InputStream is = new FileInputStream(mf);
            try
            {
                return new Manifest(is);
            }
            finally
            {
                try
                {
                    is.close();
                }
                catch (final IOException ignore) { }
            }
        }
        JarFile file = null;
        try
        {
            file = new JarFile( artifact );
            return file.getManifest();
        }
        finally
        {
            if ( file != null )
            {
                try
                {
                    file.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
    }


    private InputStream getFile( final File artifactFile, final String path ) throws IOException
    {
        if ( artifactFile.isDirectory() )
        {
            final String filePath = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
            final File file = new File(artifactFile, filePath);
            if ( file.exists() && file.isFile() )
            {
                return new FileInputStream(file);
            }
            return null;
        }
        JarFile file = null;
        try
        {
            file = new JarFile( artifactFile );
            final JarEntry entry = file.getJarEntry( path );
            if ( entry != null )
            {
                final InputStream stream = new ArtifactFileInputStream( file, entry );
                file = null; // prevent file from being closed now
                return stream;
            }
            return null;
        }
        finally
        {
            if ( file != null )
            {
                try
                {
                    file.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
    }

    private static class ArtifactFileInputStream extends FilterInputStream
    {
        final JarFile jarFile;


        ArtifactFileInputStream( JarFile jarFile, JarEntry jarEntry ) throws IOException
        {
            super( jarFile.getInputStream( jarEntry ) );
            this.jarFile = jarFile;
        }


        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            catch ( IOException ioe )
            {
            }
            jarFile.close();
        }


        @Override
        protected void finalize() throws Throwable
        {
            try
            {
                close();
            }
            finally
            {
                super.finalize();
            }
        }
    }

}
