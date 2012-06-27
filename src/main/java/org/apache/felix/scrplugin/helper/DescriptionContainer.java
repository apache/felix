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
import java.util.List;

import org.apache.felix.scrplugin.Options;

/**
 * The description container holds all {@link ComponentContainer}s.
 */
public class DescriptionContainer {

    /** The options. */
    private final Options options;

    /** The list of {@link ComponentContainer}s. */
    private final List<ComponentContainer> containers = new ArrayList<ComponentContainer>();

    /**
     * Constructor
     * @param options The options for this module
     */
    public DescriptionContainer(final Options options) {
        this.options = options;
    }

    /**
     * Get the options
     * @return The options
     */
    public Options getOptions() {
        return this.options;
    }

    /**
     * Return the list of {@link ComponentContainer}s.
     */
    public List<ComponentContainer> getComponents() {
        return this.containers;
    }

    /**
     * Add a container to the list.
     */
    public void add(final ComponentContainer c) {
        this.containers.add(c);
    }

    @Override
    public String toString() {
        return "DescriptionContainer [options=" + options + ", containers=" + containers + "]";
    }
}
