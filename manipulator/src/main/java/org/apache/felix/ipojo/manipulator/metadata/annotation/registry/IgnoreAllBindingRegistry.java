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

import static java.util.Collections.singletonList;

import java.util.List;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.ignore.NullBinding;
import org.objectweb.asm.Type;

/**
 * User: guillaume
 * Date: 11/07/13
 * Time: 16:09
 */
public class IgnoreAllBindingRegistry extends CompletableBindingRegistry {

    public IgnoreAllBindingRegistry(final BindingRegistry delegate, final Reporter reporter) {
        super(delegate, reporter);
    }

    @Override
    protected List<Binding> createBindings(final Type type) {
        return singletonList((Binding) new NullBinding(type));
    }

}
