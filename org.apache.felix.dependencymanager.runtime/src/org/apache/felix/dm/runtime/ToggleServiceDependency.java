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
package org.apache.felix.dm.runtime;

import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;

/**
 * This is a custom DependencyManager Dependency, allowing to take control of
 * when the dependency is available or not. It's used in the context of the
 * LifecycleController class, in order to activate/deactivate a Component on
 * demand.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ToggleServiceDependency extends AbstractDependency<ToggleServiceDependency> {    
    public ToggleServiceDependency() {
        super.setRequired(true);
    }

    public ToggleServiceDependency(ToggleServiceDependency prototype) {
        super(prototype);
    }

    @Override
    public DependencyContext createCopy() {
        return new ToggleServiceDependency(this);
    }

    public void activate(boolean active) {
        m_component.handleEvent(this, active ? EventType.ADDED : EventType.REMOVED, new Event(active));
    }

    @Override
    public String getSimpleName() {
        return "" + isAvailable();
    }

    @Override
    public String getType() {
        return "toggle";
    }

    @Override
    public Class<?> getAutoConfigType() {
        return null; // we don't support auto config mode.
    }
}
