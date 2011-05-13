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
package org.apache.felix.framework.resolver;

import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public interface ResolverWire
{
    /**
     * Returns the importing bundle revision.
     * @return The importing bundle revision.
    **/
    public BundleRevision getRequirer();
    /**
     * Returns the associated requirement from the importing bundle revision
     * that resulted in the creation of this wire.
     * @return
    **/
    public BundleRequirement getRequirement();
    /**
     * Returns the exporting bundle revision.
     * @return The exporting bundle revision.
    **/
    public BundleRevision getProvider();
    /**
     * Returns the associated capability from the exporting bundle revision
     * that satisfies the requirement of the importing bundle revision.
     * @return
    **/
    public BundleCapability getCapability();
}