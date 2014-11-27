/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.RequiredBundle;

public class RequiredBundleImpl implements RequiredBundle
{

    private final Bundle m_bundle;

    public RequiredBundleImpl(Bundle bundle)
    {
        m_bundle = bundle;
    }

    public String getSymbolicName()
    {
        return m_bundle.getSymbolicName();
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Bundle[] getRequiringBundles()
    {
        Set<Bundle> set = new HashSet<Bundle>();
        for (BundleWire wire : m_bundle.adapt(BundleWiring.class).getProvidedWires(null))
        {
            if (BundleNamespace.BUNDLE_NAMESPACE.equals(wire.getCapability().getNamespace()))
            {
                set.add(wire.getRequirer().getBundle());
            }
        }
        return set.toArray(new Bundle[set.size()]);
    }

    public Version getVersion()
    {
        return m_bundle.getVersion();
    }

    public boolean isRemovalPending()
    {
        return false;
    }

    public String toString()
    {
        return m_bundle.getSymbolicName() + "; version=" + m_bundle.getVersion();
    }

}
