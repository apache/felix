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
package org.apache.felix.scrplugin.tags.annotation;


import java.util.*;

import javax.imageio.spi.ServiceRegistry;

import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.tags.JavaField;
import org.apache.felix.scrplugin.tags.JavaTag;

import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaClass;


/**
 * Supports mapping of built-in and custom java anntoations to {@link JavaTag}
 * implementations.
 */
public class AnnotationTagProviderManager
{

    /**
     * Allows to define additional implementations of the interface
     * {@link org.apache.felix.scrplugin.tags.annotation.AnnotationTagProvider}
     * that provide mappings from custom annotations to
     * {@link org.apache.felix.scrplugin.tags.JavaTag} implementations.
     */
    private final Map<String, AnnotationTagProvider> annotationTagProviders = new LinkedHashMap<String, AnnotationTagProvider>();

    /**
     * @param annotationTagProviderClasses List of classes that implements
     *            {@link AnnotationTagProvider} interface.
     * @throws SCRDescriptorFailureException
     */
    public AnnotationTagProviderManager(
            final String[] annotationTagProviderClasses,
            final ClassLoader classLoader )
    throws SCRDescriptorFailureException
    {
        // search for providers
        final Iterator<AnnotationTagProvider> serviceIter = ServiceRegistry.lookupProviders(AnnotationTagProvider.class, classLoader);
        while ( serviceIter.hasNext() )
        {
            final AnnotationTagProvider provider = serviceIter.next();
            this.addProvider(provider);
        }

        // add custom providers defined in pom
        for ( int i = 0; i < annotationTagProviderClasses.length; i++ )
        {
            loadProvider( classLoader, annotationTagProviderClasses[i], false );
        }

        // always add provider supporting built-in SCR default properties (for compatibility with older
        // annotation versions)
        loadProvider( classLoader,
            "org.apache.felix.scrplugin.tags.annotation.defaulttag.DefaultAnnotationTagProvider", true );
        loadProvider( classLoader,
            "org.apache.felix.scrplugin.tags.annotation.sling.SlingAnnotationTagProvider", true );
    }

    /**
     * Add a provider (if not already available)
     */
    private void addProvider(final AnnotationTagProvider provider)
    {
        // check if this provider is already loaded
        final String key = provider.getClass().getName();
        if ( !this.annotationTagProviders.containsKey(key) )
        {
            this.annotationTagProviders.put(key, provider);
        }
    }

    private void loadProvider( final ClassLoader classLoader, final String className, final boolean silent )
        throws SCRDescriptorFailureException
    {
        String failureMessage = null;
        try
        {
            Class<?> clazz = classLoader.loadClass( className );
            try
            {
                addProvider( ( AnnotationTagProvider ) clazz.newInstance() );
            }
            catch ( ClassCastException e )
            {
                failureMessage = "Class '" + clazz.getName() + "' " + "does not implement interface '"
                    + AnnotationTagProvider.class.getName() + "'.";
            }
            catch ( InstantiationException e )
            {
                failureMessage = "Unable to instantiate class '" + clazz.getName() + "': " + e.getMessage();
            }
            catch ( IllegalAccessException e )
            {
                failureMessage = "Illegal access to class '" + clazz.getName() + "': " + e.getMessage();
            }
        }
        catch ( ClassNotFoundException e )
        {
            failureMessage = "Annotation provider class '" + className + "' not found.";
        }

        // throw an optional exception if not required to remaing silent
        if ( failureMessage != null && !silent )
        {
            throw new SCRDescriptorFailureException( failureMessage );
        }
    }


    /**
     * Converts a java annotation to {@link JavaTag} if a mapping can be found.
     *
     * @param annotation Java annotation
     * @param description Description
     * @return Tag declaration or null if no mapping found
     */
    public List<JavaTag> getTags( Annotation annotation, AnnotationJavaClassDescription description )
    {
        return getTags( annotation, description, null );
    }


    /**
     * Converts a java annotation to {@link JavaTag} if a mapping can be found.
     *
     * @param annotation Java annotation
     * @param description Description
     * @param field Field
     * @return Tag declaration or null if no mapping found
     */
    public List<JavaTag> getTags( Annotation annotation, AnnotationJavaClassDescription description, JavaField field )
    {
        List<JavaTag> tags = new ArrayList<JavaTag>();

        for ( AnnotationTagProvider provider : this.annotationTagProviders.values() )
        {
            tags.addAll( provider.getTags( annotation, description, field ) );
        }

        return tags;
    }


    /**
     * Checks if the given class has any SCR plugin java annotations defined.
     *
     * @param pClass Class
     * @return true if SCR plugin java annotation found
     */
    public boolean hasScrPluginAnnotation( final JavaClass pClass, final AnnotationJavaClassDescription description )
    {
        for ( com.thoughtworks.qdox.model.Annotation annotation : pClass.getAnnotations() )
        {
            if ( getTags( annotation, description ).size() > 0 )
            {
                return true;
            }
        }
        return false;
    }
}
