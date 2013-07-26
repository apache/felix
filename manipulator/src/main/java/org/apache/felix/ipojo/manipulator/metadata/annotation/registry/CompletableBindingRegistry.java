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

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.objectweb.asm.Type;

/**
 * User: guillaume
 * Date: 11/07/13
 * Time: 16:09
 */
public abstract class CompletableBindingRegistry implements BindingRegistry {
    private final BindingRegistry m_delegate;
    private final Reporter m_reporter;

    public CompletableBindingRegistry(final BindingRegistry delegate, final Reporter reporter) {
        m_delegate = delegate;
        m_reporter = reporter;
    }

    public List<Binding> getBindings(final String descriptor) {
        List<Binding> bindings = m_delegate.getBindings(descriptor);
        if (bindings.isEmpty()) {
            List<Binding> ignored = createBindings(Type.getType(descriptor));
            m_delegate.addBindings(ignored);
            return ignored;
        }
        return bindings;
    }

    protected abstract List<Binding> createBindings(final Type type);

    public void addBindings(final Iterable<Binding> bindings) {
        m_delegate.addBindings(bindings);
    }

    public Selection selection(final ComponentWorkbench workbench) {
        return new Selection(this, workbench, m_reporter);
    }

}
