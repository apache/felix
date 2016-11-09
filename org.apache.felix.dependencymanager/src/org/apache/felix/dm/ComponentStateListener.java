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
package org.apache.felix.dm;

/**
 * This interface can be used to register a component state listener. Component
 * state listeners are called whenever a component state changes. You get notified
 * when the component is starting, started, stopping and stopped. Each callback
 * includes a reference to the component in question.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentStateListener {
    public void changed(Component c, ComponentState state);
    
    /**
     * Called when the component is starting. At this point, the required
     * dependencies have been injected, but the service has not been registered
     * yet.
     * 
     * @param component the component
     */
    public default void starting(Component component) {}
    
    /**
     * Called when the component is started. At this point, the component has been
     * registered.
     * 
     * @param component the component
     */
    public default void started(Component component) {}
    
    /**
     * Called when the component is stopping. At this point, the component is still
     * registered.
     * 
     * @param component the component
     */
    public default void stopping(Component component) {}
    
    /**
     * Called when the component is stopped. At this point, the component has been
     * unregistered.
     * 
     * @param component the component
     */
    public default void stopped(Component component) {}
}
