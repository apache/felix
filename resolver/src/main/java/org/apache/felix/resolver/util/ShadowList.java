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
package org.apache.felix.resolver.util;

import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

public class ShadowList extends CandidateSelector
{
    public static  ShadowList createShadowList(CandidateSelector original) {
        return new ShadowList(original);
    }

    private final List<Capability> m_original;

    private ShadowList(CandidateSelector original)
    {
        super(original);
        m_original = new ArrayList<Capability>(original.getRemainingCandidates());
    }

    private ShadowList(CandidateSelector shadow, List<Capability> original)
    {
        super(shadow);
        m_original = original;
    }

    public ShadowList copy() {
        return new ShadowList(this, m_original);
    }

    public void insertHostedCapability(ResolveContext context, HostedCapability wrappedCapability, HostedCapability toInsertCapability) {
        checkModifiable();
        int removeIdx = m_original.indexOf(toInsertCapability.getDeclaredCapability());
        if (removeIdx != -1)
        {
            m_original.remove(removeIdx);
            unmodifiable.remove(removeIdx);
        }
        int insertIdx = context.insertHostedCapability(m_original, toInsertCapability);
        unmodifiable.add(insertIdx, wrappedCapability);
    }

    public void replace(Capability origCap, Capability c) {
        checkModifiable();
        int idx = unmodifiable.indexOf(origCap);
        unmodifiable.set(idx, c);
    }
}