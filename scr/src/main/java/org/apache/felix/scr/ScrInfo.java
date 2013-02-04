/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr;

import java.io.PrintWriter;

/**
 * Abstraction of command interface.
 * 
 * @Since 1.8
 */
public interface ScrInfo
{

    /**
     * List in text the components for the bundle specified, or all components if null, sorted by component ID
     * @param bundleIdentifier bundle the components are in or null for all components
     * @param out PrintStream for normal output
     * @throws IllegalArgumentException if nothing can be found
     */
    void list(String bundleIdentifier, PrintWriter out);

    /**
     * List in text detailed information about the specified components.  Components can be specified by 
     * numeric componentId, component name, a regexp to match for component name, or null for all components.
     * @param componentId specifier for desired components
     * @param out PrintStream for normal output
     * @throws IllegalArgumentException if nothing can be found
     */
    void info(String componentId, PrintWriter out);

    /**
     * List in text the current SCR configuration
     * @param out PrintStream for output.
     */
    void config(PrintWriter out);

}