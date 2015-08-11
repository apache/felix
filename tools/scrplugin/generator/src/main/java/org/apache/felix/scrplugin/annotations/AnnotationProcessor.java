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
package org.apache.felix.scrplugin.annotations;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.description.ClassDescription;

/**
 * This service provides a plugin for annotation processing. Custom tags
 * can be processed.
 * On a scanned class all available annotation processors are called in
 * order of their {@link #getRanking()} value (lowest value first).
 */
public interface AnnotationProcessor {

    /**
     * Processes annotations from the provided scanned class and adds
     * descriptions to the object model based on the read annotations.
     *
     * If this service processes an annotation, it should remove this
     * annotation from the provided list to avoid duplicate processing
     * by other processors (with higher ranking)
     *
     * @param scannedClass The scanned class.
     * @param describedClass The description container.
     */
    void process(final ScannedClass scannedClass,
            final ClassDescription describedClass)
    throws SCRDescriptorException, SCRDescriptorFailureException;

    /**
     * A user friendly name
     */
    String getName();

    /**
     * The ranking of this processor.
     */
    int getRanking();
}
