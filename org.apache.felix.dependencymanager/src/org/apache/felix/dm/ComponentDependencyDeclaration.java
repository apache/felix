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
 * Describes a component dependency. They form descriptions of dependencies
 * that are managed by the dependency manager. They can be used to query their state
 * for monitoring tools. The dependency manager shell command is an example of
 * such a tool.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentDependencyDeclaration {
    /** Names for the states of this dependency. */
    public static final String[] STATE_NAMES = { 
        "optional unavailable", 
        "optional available", 
        "required unavailable", 
        "required available",
        "optional (not tracking)",
        "required (not tracking)"
        };
    /** State constant for an unavailable, optional dependency. */
    public static final int STATE_UNAVAILABLE_OPTIONAL = 0;
    /** State constant for an available, optional dependency. */
    public static final int STATE_AVAILABLE_OPTIONAL = 1;
    /** State constant for an unavailable, required dependency. */
    public static final int STATE_UNAVAILABLE_REQUIRED = 2;
    /** State constant for an available, required dependency. */
    public static final int STATE_AVAILABLE_REQUIRED = 3;
    /** State constant for an optional dependency that has not been started yet. */
    public static final int STATE_OPTIONAL = 4;
    /** State constant for a required dependency that has not been started yet. */
    public static final int STATE_REQUIRED = 5;
    /** Returns the name of this dependency (a generic name with optional info separated by spaces)*/
    public String getName();
    /** Returns the simple dependency name (service classname for example) */
    public String getSimpleName();
    /** Returns the Dependency filter or null */
    public String getFilter();
    /** Returns the name of the type of this dependency. */
    public String getType();
    /** Returns the state of this dependency. */
    public int getState();
}
