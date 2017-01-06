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
 */package org.apache.felix.resolver;

import java.util.Collection;
import org.osgi.resource.Resource;

public interface FelixResolveContext
{
    /**
     * Return the resources that the resolver should attempt to resolve on
     * demand for specified resource which is being resolved. Inability to
     * resolve one of the on demand resources will not result in a resolution
     * exception.
     *
     * <p>
     * The resolver will ask for on demand resources for each resource that is
     * getting pulled into a resolve operation. An example of an on demand
     * resource is a fragment. When a host is being resolved the resolve context
     * will be asked if any additional resources should be added to the resolve
     * operation. The resolve context may decide that the potential fragments of
     * the host should be resolved along with the host.
     *
     * @return A collection of the resources that the resolver should attempt to
     * resolve for this resolve context. May be empty if there are no on demand
     * resources. The returned collection may be unmodifiable.
     */
    public Collection<Resource> getOndemandResources(Resource host);
}
