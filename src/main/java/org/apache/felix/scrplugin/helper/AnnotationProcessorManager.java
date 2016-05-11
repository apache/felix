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
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.felix.scrplugin.Log;
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
     * Ordered list of processors
     */
    private final List<AnnotationProcessor> processors = new ArrayList<AnnotationProcessor>();

    /**
     * Create annotation processor manager.
     * @throws SCRDescriptorFailureException
     */
    public AnnotationProcessorManager(final Log log,
            final ClassLoader classLoader )
    throws SCRDescriptorFailureException {
        // search for providers
        final Map<String, AnnotationProcessor> processorMap = new HashMap<String, AnnotationProcessor>();

        for(final AnnotationProcessor processor : ServiceLoader.load(AnnotationProcessor.class, classLoader)) {
            // check if this processor is already loaded
            final String key = processor.getClass().getName();
            if ( !processorMap.containsKey(key) ) {
                processorMap.put(key, processor);
            }
        }

        // create ordered list sorted by ranking
        for(final AnnotationProcessor pro : processorMap.values() ) {
            this.processors.add(pro);
        }
        Collections.sort(this.processors, new Comparator<AnnotationProcessor>() {

            public int compare(AnnotationProcessor o1, AnnotationProcessor o2) {
                return Integer.valueOf(o1.getRanking()).compareTo(Integer.valueOf(o2.getRanking()));
            }
        });
        if ( this.processors.size() == 0 ) {
            throw new SCRDescriptorFailureException("No annotation processors found in classpath.");
        }
        log.debug("..using annotation processors: ");
        for(final AnnotationProcessor pro : this.processors) {
            log.debug("  - " + pro.getName() + " - " + pro.getRanking());
        }
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#process(org.apache.felix.scrplugin.annotations.ScannedClass, org.apache.felix.scrplugin.description.ClassDescription)
     */
    public void process(final ScannedClass scannedClass,
            final ClassDescription describedClass)
    throws SCRDescriptorException, SCRDescriptorFailureException {
        // forward do all processors
        for(final AnnotationProcessor ap : this.processors) {
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
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getName()
     */
    public String getName() {
        return "Annotation Processor Manager";
    }
}
