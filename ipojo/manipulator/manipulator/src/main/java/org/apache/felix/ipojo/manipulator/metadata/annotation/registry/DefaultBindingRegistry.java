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

import static java.util.Collections.emptyList;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores all the {@link Binding}s coming from the {@link org.apache.felix.ipojo.manipulator.spi.Module}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DefaultBindingRegistry implements BindingRegistry {
    private final Map<String, List<Binding>> tree = new HashMap<String, List<Binding>>();
    protected final Reporter reporter;

    public DefaultBindingRegistry(Reporter reporter) {
        this.reporter = reporter;
    }

    /**
     * Stores the given Bindings
     */
    public void addBindings(Iterable<Binding> bindings) {
        for (Binding binding : bindings) {
            Type type = binding.getAnnotationType();

            List<Binding> potential = tree.get(type.getDescriptor());
            if (potential == null) {
                // Annotation is not already found in supported list
                potential = new ArrayList<Binding>();
                tree.put(type.getDescriptor(), potential);
            }

            //reporter.trace("Registered @%s", type.getClassName());
            potential.add(binding);
        }
    }

    /**
     * Initiate a {@link Selection} for the given workbench.
     */
    public Selection selection(ComponentWorkbench workbench) {
        return new Selection(this, workbench, reporter);
    }

    public List<Binding> getBindings(String descriptor) {
        List<Binding> bindings = tree.get(descriptor);
        if (bindings == null) {
            bindings = emptyList();
        }
        return bindings;
    }

}
