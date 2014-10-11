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

import java.util.Dictionary;

import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;

/**
 * This is a custom DependencyManager Dependency, allowing to take control of
 * when the dependency is available or not. It's used in the context of the
 * LifecycleController class, in order to activate/deactivate a Component on
 * demand.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ToggleServiceDependency extends AbstractDependency<ToggleServiceDependency> {
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
        public void close() {
        }

        @Override
        public Object getEvent() {
            return null;
        }

        @Override
        public Dictionary getProperties() {
            return null;
        }
    }
    
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
        if (active) {
            add(new ToggleEvent());
        } else {
            remove(new ToggleEvent());
        }
    }

    @Override
    public String getName() {
        return "" + isAvailable();
    }

    @Override
    public String getType() {
        return "toggle";
    }
}
