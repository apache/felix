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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public interface Resolver
{
    Map<BundleRevision, List<ResolverWire>> resolve(
        ResolverState state,
        Set<BundleRevision> mandatoryRevisions,
        Set<BundleRevision> optionalRevisions,
        Set<BundleRevision> ondemandFragments);
    Map<BundleRevision, List<ResolverWire>> resolve(
        ResolverState state, BundleRevision revision, String pkgName,
        Set<BundleRevision> ondemandFragments);

    public static interface ResolverState
    {
        boolean isEffective(BundleRequirement req);
        SortedSet<BundleCapability> getCandidates(
            BundleRequirement req, boolean obeyMandatory);
        void checkExecutionEnvironment(BundleRevision revision) throws ResolveException;
        void checkNativeLibraries(BundleRevision revision) throws ResolveException;
    }
}