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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.description.ComponentDescription;

/**
 * <code>DescriptionContainer</code>...
 *
 * A description container is a collection of {@link ComponentDescription}s.
 */
public class DescriptionContainer {

    /** The options. */
    private final Options options;

    /** The list of {@link Component}s. */
    private final List<ComponentContainer> components = new ArrayList<ComponentContainer>();

    public DescriptionContainer(final Options options) {
        this.options = options;
    }

    public Options getOptions() {
        return this.options;
    }

    /**
     * Return the list of {@link Component}s.
     */
    public List<ComponentContainer> getComponents() {
        return this.components;
    }

    /**
     * Add a component to the list.
     */
    public void addComponent(ComponentContainer component) {
        this.components.add(component);
    }

    @Override
    public String toString() {
        return "DescriptionContainer [options=" + options + ", components=" + components + "]";
    }
}
