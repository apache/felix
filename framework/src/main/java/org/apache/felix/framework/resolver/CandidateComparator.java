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

import java.util.Comparator;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

public class CandidateComparator implements Comparator<BundleCapability>
{
    public int compare(BundleCapability cap1, BundleCapability cap2)
    {
        // First check resolved state, since resolved capabilities have priority
        // over unresolved ones. Compare in reverse order since we want to sort
        // in descending order.
        int c = 0;
        if ((cap1.getRevision().getWiring() != null)
            && (cap2.getRevision().getWiring() == null))
        {
            c = -1;
        }
        else if ((cap1.getRevision().getWiring() == null)
            && (cap2.getRevision().getWiring() != null))
        {
            c = 1;
        }

        // Compare revision capabilities.
        if ((c == 0) && cap1.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttributes().get(BundleRevision.BUNDLE_NAMESPACE))
                .compareTo(cap2.getAttributes().get(BundleRevision.BUNDLE_NAMESPACE));
            if (c == 0)
            {
                Version v1 = (!cap1.getAttributes().containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    ? Version.emptyVersion
                    : (Version) cap1.getAttributes().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                Version v2 = (!cap2.getAttributes().containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    ? Version.emptyVersion
                    : (Version) cap2.getAttributes().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = v2.compareTo(v1);
            }
        }
        // Compare package capabilities.
        else if ((c == 0) && cap1.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE))
                .compareTo(cap2.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE));
            if (c == 0)
            {
                Version v1 = (!cap1.getAttributes().containsKey(BundleCapabilityImpl.VERSION_ATTR))
                    ? Version.emptyVersion
                    : (Version) cap1.getAttributes().get(BundleCapabilityImpl.VERSION_ATTR);
                Version v2 = (!cap2.getAttributes().containsKey(BundleCapabilityImpl.VERSION_ATTR))
                    ? Version.emptyVersion
                    : (Version) cap2.getAttributes().get(BundleCapabilityImpl.VERSION_ATTR);
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = v2.compareTo(v1);
            }
        }

        // Finally, compare bundle identity.
        if (c == 0)
        {
            if (cap1.getRevision().getBundle().getBundleId() <
                cap2.getRevision().getBundle().getBundleId())
            {
                c = -1;
            }
            else if (cap1.getRevision().getBundle().getBundleId() >
                cap2.getRevision().getBundle().getBundleId())
            {
                c = 1;
            }
        }

        return c;
    }
}