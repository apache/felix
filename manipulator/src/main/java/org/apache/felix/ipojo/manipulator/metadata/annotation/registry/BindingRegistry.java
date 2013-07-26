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

package org.apache.felix.ipojo.manipulator.metadata.annotation.registry;

import java.util.List;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;

/**
 * User: guillaume
 * Date: 11/07/13
 * Time: 16:12
 */
public interface BindingRegistry {
    void addBindings(Iterable<Binding> bindings);

    Selection selection(ComponentWorkbench workbench);

    /**
     * Find the list of {@link Binding} registered with the given annotation type.
     * This method returns an empty List if no bindings are registered.
     * @param descriptor denotes the annotation's type
     * @return the list of {@link Binding} registered with the given descriptor, the list may be empty if no bindings are found.
     */
    List<Binding> getBindings(String descriptor);
}
