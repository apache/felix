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
package org.apache.felix.framework.wiring;

import org.apache.felix.framework.resolver.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.apache.felix.framework.BundleRevisionImpl;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

// TODO: OSGi R4.3 - Should this be in framework package?
public class FelixBundleWireImpl implements FelixBundleWire
{
    private final BundleRevision m_requirer;
    private final BundleRequirement m_req;
    private final BundleRevision m_provider;
    private final BundleCapability m_cap;
    private volatile Set<String> m_packages;

    public FelixBundleWireImpl(BundleRevision requirer, BundleRequirement req,
        BundleRevision provider, BundleCapability cap)
    {
        m_requirer = requirer;
        m_req = req;
        m_provider = provider;
        m_cap = cap;
    }

    public BundleRevision getRequirer()
    {
        return m_requirer;
    }

    public BundleWiring getRequirerWiring()
    {
        return m_requirer.getWiring();
    }

    public BundleRequirement getRequirement()
    {
        return m_req;
    }

    public BundleRevision getProvider()
    {
        return m_provider;
    }

    public BundleWiring getProviderWiring()
    {
        return m_provider.getWiring();
    }

    public BundleCapability getCapability()
    {
        return m_cap;
    }

    public String toString()
    {
        return "[" + m_requirer + "] "
            + m_req
            + " -> "
            + "[" + m_provider + "]";
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public boolean hasPackage(String pkgName)
    {
        boolean result = false;
        if (m_cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
        {
            result = m_cap.getAttributes()
                .get(BundleCapabilityImpl.PACKAGE_ATTR).equals(pkgName); 
        }
        else if (m_cap.getNamespace().equals(BundleCapabilityImpl.BUNDLE_NAMESPACE))
        {
            if (m_packages == null)
            {
                m_packages = calculateRequiredPackages(
                    m_provider.getWiring(), new HashSet<String>());
            }
            result = m_packages.contains(pkgName);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public Class getClass(String name) throws ClassNotFoundException
    {
        Class clazz = null;

        // Get the package of the target class.
        String pkgName = Util.getClassPackage(name);

        // Only check if this wire provides the target package.
        if (hasPackage(pkgName))
        {
            String namespace = m_cap.getNamespace();
            if (namespace.equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
            {
                // Check the include/exclude filters from the target package
                // to make sure that the class is actually visible. We delegate
                // to the exporting revision, rather than its content, so it can
                // it can follow any internal wires it may have (e.g., if the
                // package has multiple sources).
                if (((BundleCapabilityImpl) m_cap).isIncluded(name))
                {
                    clazz = ((BundleRevisionImpl) m_provider).getClassByDelegation(name);
                }

                // If no class was found, then we must throw an exception
                // since the exporter for this package did not contain the
                // requested class.
                if (clazz == null)
                {
                    throw new ClassNotFoundException(name);
                }
            }
            else if (namespace.equals(BundleCapabilityImpl.BUNDLE_NAMESPACE))
            {
                try
                {
                    clazz = ((BundleRevisionImpl) m_provider).getClassByDelegation(name);
                }
                catch (ClassNotFoundException ex)
                {
                    // Do not throw the exception here, since we want
                    // to continue search other package sources and
                    // ultimately the revision's own content.
                }
            }
        }

        return clazz;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResource(java.lang.String)
     */
    public URL getResource(String name) throws ResourceNotFoundException
    {
        URL url = null;

        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // Only check if this wire provides the target package.
        if (hasPackage(pkgName))
        {
            String namespace = m_cap.getNamespace();
            if (namespace.equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
            {
                // Delegate to the exporting revision, rather than its
                // content, so that it can follow any internal wires it may have
                // (e.g., if the package has multiple sources).
                url = ((BundleRevisionImpl) m_provider).getResourceByDelegation(name);

                // If no resource was found, then we must throw an exception
                // since the exporter for this package did not contain the
                // requested class.
                if (url == null)
                {
                    throw new ResourceNotFoundException(name);
                }
            }
            else if (namespace.equals(BundleCapabilityImpl.BUNDLE_NAMESPACE))
            {
                url = ((BundleRevisionImpl) m_provider).getResourceByDelegation(name);

                // Don't throw ResourceNotFoundException because require-bundle
                // dependencies support split packages.
            }

        }

        return url;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResources(java.lang.String)
     */
    public Enumeration getResources(String name) throws ResourceNotFoundException
    {
        Enumeration urls = null;

        // Get the package of the target class.
        String pkgName = Util.getResourcePackage(name);

        // Only check if this wire provides the target package.
        if (hasPackage(pkgName))
        {
            String namespace = m_cap.getNamespace();
            if (namespace.equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
            {
                urls = ((BundleRevisionImpl) m_provider).getResourcesByDelegation(name);

                // If no resource was found, then we must throw an exception
                // since the exporter for this package did not contain the
                // requested class.
                if ((urls == null) || !urls.hasMoreElements())
                {
                    throw new ResourceNotFoundException(name);
                }
            }
            else if (namespace.equals(BundleCapabilityImpl.BUNDLE_NAMESPACE))
            {
                urls = ((BundleRevisionImpl) m_provider).getResourcesByDelegation(name);

                // Don't throw ResourceNotFoundException because require-bundle
                // dependencies support split packages.
            }
        }

        return urls;
    }

    private static Set<String> calculateRequiredPackages(
        BundleWiring providerWiring, Set<String> packages)
    {
// TODO: OSGi R4.3 - This might be calcualted differently when BundleWiring
//       returns the proper information.

        // Add exported packages.
        for (BundleCapability cap : providerWiring.getCapabilities(null))
        {
            if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
            {
                packages.add(
                    (String) cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR));
            }
        }

        // Add re-exported packages for any required bundle dependencies
        // that are re-exported.
        for (BundleWire bw : providerWiring.getRequiredWires(null))
        {
            if (bw.getRequirement().getNamespace().equals(BundleCapabilityImpl.BUNDLE_NAMESPACE))
            {
                String dir =
                    bw.getRequirement().getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                if ((dir != null) && (dir.equals(Constants.VISIBILITY_REEXPORT)))
                {
                    calculateRequiredPackages(bw.getProviderWiring(), packages);
                }
            }
        }
        return packages;
    }
}