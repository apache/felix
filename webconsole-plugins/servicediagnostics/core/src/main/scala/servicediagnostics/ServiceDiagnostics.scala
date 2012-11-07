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
package org.apache.felix.servicediagnostics

/**
 * This is the main ServiceDiagnostics interface.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
trait ServiceDiagnostics 
{
    /**
     * returns a map of component name to list of leaf unresolved dependencies.
     */
    def notavail:Map[String, List[String]]

    /**
     * returns a graph of unresolvable components (typically loops)
     */
    def unresolved :Map[String, List[String]]

    /**
     * returns a map of resolved service names to list of bundles using the service
     */
    def allServices:Map[String, List[String]]
}

