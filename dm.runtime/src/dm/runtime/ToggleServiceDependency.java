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
package dm.runtime;

import java.util.Dictionary;

import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.osgi.framework.BundleContext;

/**
 * This is a custom DependencyManager Dependency, allowing to take control of
 * when the dependency is available or not. It's used in the context of the
 * LifecycleController class, in order to activate/deactivate a Component on
 * demand.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ToggleServiceDependency implements Dependency, ComponentDependencyDeclaration, DependencyContext {
    private volatile boolean m_isAvailable;
    protected volatile ComponentContext m_component;
    protected boolean m_instanceBound;
    private volatile boolean m_isStarted; // volatile because accessed by getState method
    
    public static class ToggleEvent implements Event {
        @Override
        public boolean equals(Object e) {
            if (e instanceof ToggleEvent) {
                return true;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return ToggleEvent.class.hashCode();
        }
        
        @Override
        public int compareTo(Object o) {
            return 0;
        }

        @Override
        public void close(BundleContext context) {
        }
    }

    public ToggleServiceDependency() {
    }

    public ToggleServiceDependency(ToggleServiceDependency other) {
        m_component = other.m_component;
        m_instanceBound = other.m_instanceBound;
        m_isAvailable = other.m_isAvailable;
        m_isStarted = other.m_isStarted;
    }

    public void activate(boolean active) {
        if (active) {
            add(new ToggleEvent());
        } else {
            remove(new ToggleEvent());
        }
    }
    
    @Override
    public void setAvailable(boolean available) {
        m_isAvailable = available;
    }

    @Override
    public void invokeAdd(Event e) {
    }

    @Override
    public void invokeChange(Event e) {
    }

    @Override
    public void invokeRemove(Event e) {
    }
    
    public void invokeSwap(Event event, Event newEvent) {        
    }

    @Override
    public void add(final Event e) {
        m_component.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                m_component.handleAdded(ToggleServiceDependency.this, e);
            }
        });
    }

    @Override
    public void change(Event e) {
    }

    @Override
    public void remove(final Event e) {
        m_component.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                m_component.handleRemoved(ToggleServiceDependency.this, e);
            }
        });
    }

    @Override
    public void add(ComponentContext component) {
        m_component = component;
    }

    @Override
    public void remove(ComponentContext component) {
        m_component = null;
    }

    @Override
    public void start() {
        m_isStarted = true;
    }

    @Override
    public void stop() {
        m_isStarted = false;
    }

    @Override
    public boolean isAvailable() {
        return m_isAvailable;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public boolean isInstanceBound() {
        return m_instanceBound;
    }

    @Override
    public void setInstanceBound(boolean instanceBound) {
        m_instanceBound = instanceBound;
    }

    @Override
    public boolean needsInstance() {
        return false;
    }

    @Override
    public Class getAutoConfigType() {
        return null;
    }

    @Override
    public Object getAutoConfigInstance() {
        return null;
    }

    @Override
    public boolean isAutoConfig() {
        return false;
    }

    @Override
    public String getAutoConfigName() {
        return null;
    }

    @Override
    public DependencyContext createCopy() {
        return new ToggleServiceDependency(this);
    }

    @Override
    public boolean isPropagated() {
        return false;
    }

    @Override
    public Dictionary getProperties() {
        return null;
    }

    @Override
    public String getName() {
        return "" + m_isAvailable;
    }

    @Override
    public String getType() {
        return "toggle";
    }

    @Override
    public int getState() {
        if (m_isStarted) {
            return (isAvailable() ? 1 : 0) + (isRequired() ? 2 : 0);
        }
        else {
            return isRequired() ? ComponentDependencyDeclaration.STATE_REQUIRED
                : ComponentDependencyDeclaration.STATE_OPTIONAL;
        }
    }
}
