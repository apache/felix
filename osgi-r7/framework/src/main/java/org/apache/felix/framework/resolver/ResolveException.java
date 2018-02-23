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

import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.resolver.ResolutionException;

public class ResolveException extends ResolutionException
{
    private final BundleRevision m_revision;
    private final BundleRequirement m_req;

    /**
     * Constructs an instance of <code>ResolveException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ResolveException(String msg, BundleRevision revision, BundleRequirement req)
    {
        super(msg);
        m_revision = revision;
        m_req = req;
    }

    public BundleRevision getRevision()
    {
        return m_revision;
    }

    public BundleRequirement getRequirement()
    {
        return m_req;
    }
}