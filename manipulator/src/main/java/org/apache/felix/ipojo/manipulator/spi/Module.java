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

package org.apache.felix.ipojo.manipulator.spi;

import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.Binding;

/**
 * A Module is the contributions from third party to the iPOJO manipulation process.
 * It is dedicated to Annotation binding support (executing a given ASM AnnotationVisitor
 * when a particular annotation is found).
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Module extends Iterable<Binding> {

    /**
     * Load the bindings provided by this module (only once).
     * @since 1.11.2
     */
    void load();
}
