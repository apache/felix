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

import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.BundleContext;

/**
 * Describes a component. Component declarations form descriptions of components
 * that are managed by the dependency manager. They can be used to query their state
 * for monitoring tools. The dependency manager shell command is an example of
 * such a tool.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentDeclaration {
    /** Names for the states of this component. */
    public static final String[] STATE_NAMES = { "unregistered", "registered" };
    /** State constant for an unregistered component. */
    public static final int STATE_UNREGISTERED = 0;
    /** State constant for a registered component. */
    public static final int STATE_REGISTERED = 1;
    /** Returns a list of dependencies associated with this component. */
    public ComponentDependencyDeclaration[] getComponentDependencies();
    /** Returns the description of this component (the classname or the provided service(s)) */
    public String getName();
    /** Returns the class name of the Component implementation. */
    public String getClassName();
    /** Returns the service optionally provided by this component, or null */
    public String[] getServices();
    /** Returns the service properties, or null */
    public <K,V> Dictionary<K, V> getServiceProperties();     
    /** Returns the state of this component. */
    public int getState();
    /** Returns the instance id of this component. */
    public long getId();
    /** Returns the bundle context associated with this component. */
    public BundleContext getBundleContext();
    /** Returns the dependency manager for this component */
    public DependencyManager getDependencyManager();
    /** Returns the execution time in nanos for each component callbacks (init/start/stop/destroy) */
    public Map<String, Long> getCallbacksTime(); 
}
