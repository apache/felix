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
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;

class FrameworkWiringImpl implements FrameworkWiring, Runnable
{
    private final Felix m_felix;
    private List<Collection<Bundle>> m_requests = null;
    private List<FrameworkListener[]> m_requestListeners = null;
    private Thread m_thread = null;

    public FrameworkWiringImpl(Felix felix)
    {
        m_felix = felix;
    }

    /**
     * Stops the FelixFrameworkWiring thread on system shutdown. Shutting down the
     * thread explicitly is required in the embedded case, where Felix may be
     * stopped without the Java VM being stopped. In this case the
     * FelixFrameworkWiring thread must be stopped explicitly.
     * <p>
     * This method is called by the
     * {@link PackageAdminActivator#stop(BundleContext)} method.
     */
    synchronized void stop()
    {
        if (m_thread != null)
        {
            // Null thread variable to signal to the thread that
            // we want it to exit.
            m_thread = null;

            // Wake up the thread, if it is currently in the wait() state
            // for more work.
            notifyAll();
        }
    }

    public Bundle getBundle()
    {
        return m_felix;
    }

    public void refreshBundles(Collection<Bundle> bundles, FrameworkListener... listeners)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(m_felix, AdminPermission.RESOLVE));
        }
        synchronized (this)
        {
            // Start a thread to perform asynchronous package refreshes.
            if (m_thread == null)
            {
                m_thread = new Thread(this, "FelixFrameworkWiring");
                m_thread.setDaemon(true);
                m_thread.start();
            }

            // Save our request parameters and notify all.
            if (m_requests == null)
            {
                List<Collection<Bundle>> requests = new ArrayList<Collection<Bundle>>();
                requests.add(bundles);
                m_requests = requests;
                List<FrameworkListener[]> requestListeners =
                    new ArrayList<FrameworkListener[]>();
                requestListeners.add(listeners);
                m_requestListeners = requestListeners;
            }
            else
            {
                m_requests.add(bundles);
            }
            notifyAll();
        }
    }

    public boolean resolveBundles(Collection<Bundle> bundles)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(m_felix, AdminPermission.RESOLVE));
        }

        return m_felix.resolveBundles(bundles);
    }

    public Collection<Bundle> getRemovalPendingBundles()
    {
        return m_felix.getRemovalPendingBundles();
    }

    public Collection<Bundle> getDependencyClosure(Collection<Bundle> targets)
    {
        return m_felix.getDependencyClosure(targets);
    }

    /**
     * The OSGi specification states that package refreshes happen
     * asynchronously; this is the run() method for the package
     * refreshing thread.
    **/
    public void run()
    {
        // This thread loops forever, thus it should
        // be a daemon thread.
        while (true)
        {
            Collection<Bundle> bundles = null;
            FrameworkListener[] listeners = null;
            synchronized (this)
            {
                // Wait for a refresh request.
                while (m_requests == null)
                {
                    // Terminate the thread if requested to do so (see stop()).
                    if (m_thread == null)
                    {
                        return;
                    }

                    try
                    {
                        wait();
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }

                // Get the bundles parameter for the current refresh request.
                bundles = m_requests.get(0);
                listeners = m_requestListeners.get(0);
            }

            // Perform refresh.
            // NOTE: We don't catch any exceptions here, because
            // the invoked method shields us from exceptions by
            // catching Throwables when its invokes callbacks.
            m_felix.refreshPackages(bundles, listeners);

            // Remove the first request since it is now completed.
            synchronized (this)
            {
                m_requests.remove(0);
                m_requestListeners.remove(0);
                if (m_requests.isEmpty())
                {
                    m_requests = null;
                    m_requestListeners = null;
                }
            }
        }
    }
}