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
package org.apache.felix.scrplugin.helper;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.spi.ServiceRegistry;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.annotations.ScannedClass;
import org.apache.felix.scrplugin.description.ClassDescription;


/**
 * Supports mapping of built-in and custom java annotations to
 * descriptions.
 */
public class AnnotationProcessorManager implements AnnotationProcessor {

    /**
     * Allows to define additional implementations of the interface
     * {@linkAnnotationProcessor}
     * that provide mappings from custom annotations to descriptions.
     */
    private final Map<String, AnnotationProcessor> processors = new HashMap<String, AnnotationProcessor>();

    /**
     * Ordered list of processors
     */
    private final List<AnnotationProcessor> cachedProcessors = new ArrayList<AnnotationProcessor>();

    /**
     * @param annotationProcessorClasses List of classes that implements
     *            {@link AnnotationProcessor} interface.
     * @throws SCRDescriptorFailureException
     */
    public AnnotationProcessorManager(
            final String[] annotationProcessorClasses,
            final ClassLoader classLoader )
    throws SCRDescriptorFailureException {
        // search for providers
        final Iterator<AnnotationProcessor> serviceIter = ServiceRegistry.lookupProviders(AnnotationProcessor.class, classLoader);
        while ( serviceIter.hasNext() ) {
            final AnnotationProcessor provider = serviceIter.next();
            this.addProvider(provider);
        }

        // add custom processors defined in the tool (maven, ant...)
        for ( int i = 0; i < annotationProcessorClasses.length; i++ ) {
            loadProcessor( classLoader, annotationProcessorClasses[i] );
        }

        // create ordered list
        for(final AnnotationProcessor pro : this.processors.values() ) {
            this.cachedProcessors.add(pro);
        }
        Collections.sort(this.cachedProcessors, new Comparator<AnnotationProcessor>() {

            public int compare(AnnotationProcessor o1, AnnotationProcessor o2) {
                return Integer.valueOf(o1.getRanking()).compareTo(Integer.valueOf(o2.getRanking()));
            }
        });
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#process(org.apache.felix.scrplugin.annotations.ScannedClass, org.apache.felix.scrplugin.description.ClassDescription)
     */
    public void process(final ScannedClass scannedClass,
            final ClassDescription describedClass)
    throws SCRDescriptorException, SCRDescriptorFailureException {
        for(final AnnotationProcessor ap : this.cachedProcessors) {
            ap.process(scannedClass, describedClass);
        }
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getRanking()
     */
    public int getRanking() {
        return 0;
    }

    /**
     * Add a processor (if not already available)
     */
    private void addProvider(final AnnotationProcessor processor) {
        // check if this processor is already loaded
        final String key = processor.getClass().getName();
        if ( !this.processors.containsKey(key) ) {
            this.processors.put(key, processor);
        }
    }

    private void loadProcessor( final ClassLoader classLoader, final String className )
    throws SCRDescriptorFailureException {
        String failureMessage = null;
        try {
            Class<?> clazz = classLoader.loadClass( className );
            try {
                addProvider( ( AnnotationProcessor ) clazz.newInstance() );
            } catch ( final ClassCastException e ) {
                failureMessage = "Class '" + clazz.getName() + "' " + "does not implement interface '"
                    + AnnotationProcessor.class.getName() + "'.";
            } catch ( final InstantiationException e ) {
                failureMessage = "Unable to instantiate class '" + clazz.getName() + "': " + e.getMessage();
            } catch ( final IllegalAccessException e ) {
                failureMessage = "Illegal access to class '" + clazz.getName() + "': " + e.getMessage();
            }
        } catch ( final ClassNotFoundException e ) {
            failureMessage = "Annotation provider class '" + className + "' not found.";
        }

        // throw an exception
        if ( failureMessage != null  ) {
            throw new SCRDescriptorFailureException( failureMessage );
        }
    }
}
