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

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.startlevel.StartLevel;

import org.apache.felix.connect.felix.framework.ServiceRegistry;
import org.apache.felix.connect.felix.framework.util.EventDispatcher;
import org.apache.felix.connect.launch.BundleDescriptor;
import org.apache.felix.connect.launch.ClasspathScanner;
import org.apache.felix.connect.launch.PojoServiceRegistry;
import org.apache.felix.connect.launch.PojoServiceRegistryFactory;

public class PojoSR implements PojoServiceRegistry
{
    private final BundleContext m_context;
    private final ServiceRegistry m_registry = new ServiceRegistry(
            new ServiceRegistry.ServiceRegistryCallbacks()
            {
                public void serviceChanged(ServiceEvent event, Dictionary<String, ?> oldProps)
                {
                    m_dispatcher.fireServiceEvent(event, oldProps, m_bundles.get(0l));
                }
            });

    private final EventDispatcher m_dispatcher = new EventDispatcher(m_registry);
    private final Map<Long, Bundle> m_bundles = new HashMap<Long, Bundle>();
    private final Map<String, Object> bundleConfig;
    private final boolean m_hasVFS;

    public static BundleDescriptor createSystemBundle() {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.BUNDLE_SYMBOLICNAME, "org.apache.felix.connect");
        headers.put(Constants.BUNDLE_VERSION, "0.0.0");
        headers.put(Constants.BUNDLE_NAME, "System Bundle");
        headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
        headers.put(Constants.BUNDLE_VENDOR, "Apache Software Foundation");


        Revision revision = new Revision()
        {
            final long lastModified = System.currentTimeMillis();
            @Override
            public long getLastModified()
            {
                return lastModified;
            }

            @Override
            public Enumeration<String> getEntries()
            {
                return Collections.enumeration(Collections.EMPTY_LIST);
            }

            @Override
            public URL getEntry(String entryName)
            {
                return getClass().getClassLoader().getResource(entryName);
            }
        };
        Map<Class, Object> services = new HashMap<Class, Object>();
        services.put(FrameworkStartLevel.class, new FrameworkStartLevelImpl());
        return new BundleDescriptor(
                PojoSR.class.getClassLoader(),
                "System Bundle",
                headers,
                revision,
                services
                );
    }

    public PojoSR(Map<String, ?> config) throws Exception
    {
        this(config, null);
    }

    public PojoSR(Map<String, ?> config, BundleDescriptor systemBundle) throws Exception
    {
        if (systemBundle == null) {
            systemBundle = createSystemBundle();
        }
        bundleConfig = new HashMap<String, Object>(config);
        final Bundle b = new PojoSRBundle(
                        m_registry,
                        m_dispatcher,
                        m_bundles,
                        systemBundle.getUrl(),
                        0,
                        "org.apache.felix.connect",
                        new Version(0, 0, 1),
                                systemBundle.getRevision(),
                        systemBundle.getClassLoader(),
                        systemBundle.getHeaders(),
                        systemBundle.getServices(),
                        bundleConfig)
        {
            @Override
            public synchronized void start() throws BundleException
            {
                if (m_state != Bundle.RESOLVED)
                {
                    return;
                }
                m_dispatcher.startDispatching();
                m_state = Bundle.STARTING;

                m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTING, this));
                m_context = new PojoSRBundleContext(this, m_registry, m_dispatcher, m_bundles, bundleConfig);
                int i = 0;
                for (Bundle b : m_bundles.values())
                {
                    i++;
                    try
                    {
                        if (b != this)
                        {
                            b.start();
                        }
                    }
                    catch (Throwable t)
                    {
                        System.out.println("Unable to start bundle: " + i);
                        t.printStackTrace();
                    }
                }
                m_state = Bundle.ACTIVE;
                m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTED, this));

                m_dispatcher.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, this, null));
                super.start();
            }

            @Override
            public synchronized void stop() throws BundleException
            {
                if ((m_state == Bundle.STOPPING) || m_state == Bundle.RESOLVED)
                {
                    return;

                }
                else if (m_state != Bundle.ACTIVE)
                {
                    throw new BundleException("Can't stop pojosr because it is not ACTIVE");
                }
                final Bundle systemBundle = this;
                Runnable r = new Runnable()
                {

                    public void run()
                    {
                        m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPING, systemBundle));
                        for (Bundle b : m_bundles.values())
                        {
                            try
                            {
                                if (b != systemBundle)
                                {
                                    b.stop();
                                }
                            }
                            catch (Throwable t)
                            {
                                t.printStackTrace();
                            }
                        }
                        m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPED, systemBundle));
                        m_state = Bundle.RESOLVED;
                        m_dispatcher.stopDispatching();
                    }
                };
                m_state = Bundle.STOPPING;
                if ("true".equalsIgnoreCase(System.getProperty("org.apache.felix.connect.events.sync")))
                {
                    r.run();
                }
                else
                {
                    new Thread(r).start();
                }
            }
        };
        m_bundles.put(0l, b);
        b.start();
        b.getBundleContext().registerService(StartLevel.class.getName(), new StartLevelImpl(), null);

        b.getBundleContext().registerService(PackageAdmin.class.getName(), new PackageAdminImpl(), null);
        m_context = b.getBundleContext();

        boolean hasVFS;
        try
        {
            hasVFS = org.jboss.vfs.VFS.class != null;
        } catch (Throwable t) {
            hasVFS = false;
        }
        m_hasVFS = hasVFS;

        Collection<BundleDescriptor> scan = (Collection<BundleDescriptor>) config.get(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS);

        if (scan != null)
        {
            startBundles(scan);
        }
    }

    public void startBundles(Collection<BundleDescriptor> scan) throws Exception
    {
        for (BundleDescriptor desc : scan)
        {
            Revision revision = desc.getRevision();
            if (revision == null)
            {
                revision = buildRevision(desc);
            }
            Map<String, String> bundleHeaders = desc.getHeaders();
            Version osgiVersion;
            try
            {
                osgiVersion = Version.parseVersion(bundleHeaders.get(Constants.BUNDLE_VERSION));
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                osgiVersion = Version.emptyVersion;
            }
            String sym = bundleHeaders.get(Constants.BUNDLE_SYMBOLICNAME);
            if (sym != null)
            {
                int idx = sym.indexOf(';');
                if (idx > 0)
                {
                    sym = sym.substring(0, idx);
                }
                sym = sym.trim();
            }

            Bundle bundle = new PojoSRBundle(
                    m_registry,
                    m_dispatcher,
                    m_bundles,
                    desc.getUrl(),
                    m_bundles.size(),
                    sym,
                    osgiVersion,
                    revision,
                    desc.getClassLoader(),
                    bundleHeaders,
                    desc.getServices(),
                    bundleConfig);
            m_bundles.put(bundle.getBundleId(), bundle);
        }


        for (Bundle bundle : m_bundles.values())
        {
            try
            {
                bundle.start();
            }
            catch (Throwable e)
            {
                System.out.println("Unable to start bundle: " + bundle);
                e.printStackTrace();
            }
        }

    }

    private Revision buildRevision(BundleDescriptor desc) throws IOException
    {
        Revision r;
        URL url = new URL(desc.getUrl());
        URL u = new URL(desc.getUrl() + "META-INF/MANIFEST.MF");
        String extF = u.toExternalForm();
        if (extF.startsWith("file:"))
        {
            File root = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
            r = new DirRevision(root);
        }
        else
        {
            URLConnection uc = u.openConnection();
            if (uc instanceof JarURLConnection)
            {
                String target = ((JarURLConnection) uc).getJarFileURL().toExternalForm();
                String prefix = null;
                if (!("jar:" + target + "!/").equals(desc.getUrl()) && desc.getUrl().startsWith("jar:" + target + "!/"))
                {
                    System.out.println(desc.getUrl() + " " + target);
                    prefix = desc.getUrl().substring(("jar:" + target + "!/").length());
                }
                r = new JarRevision(
                        ((JarURLConnection) uc).getJarFile(),
                        ((JarURLConnection) uc).getJarFileURL(),
                        prefix,
                        uc.getLastModified());
            }
            else if (m_hasVFS && extF.startsWith("vfs"))
            {
                r = new VFSRevision(url, url.openConnection().getLastModified());
            }
            else
            {
                r = new URLRevision(url, url.openConnection().getLastModified());
            }
        }
        return r;
    }

    public static void main(String[] args) throws Exception
    {
        Filter filter = null;
        Class<?> main = null;
        for (int i = 0; (args != null) && (i < args.length) && (i < 2); i++)
        {
            try
            {
                filter = FrameworkUtil.createFilter(args[i]);
            }
            catch (InvalidSyntaxException ie)
            {
                try
                {
                    main = PojoSR.class.getClassLoader().loadClass(args[i]);
                }
                catch (Exception ex)
                {
                    throw new IllegalArgumentException("Argument is neither a filter nor a class: " + args[i]);
                }
            }
        }
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(
                PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS,
                (filter != null) ? new ClasspathScanner()
                        .scanForBundles(filter.toString()) : new ClasspathScanner()
                        .scanForBundles());
        new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(config);
        if (main != null)
        {
            int count = 0;
            if (filter != null)
            {
                count++;
            }
            count++;
            String[] newArgs = args;
            if (count > 0)
            {
                newArgs = new String[args.length - count];
                System.arraycopy(args, count, newArgs, 0, newArgs.length);
            }
            main.getMethod("main", String[].class).invoke(null, newArgs);
        }
    }

    public BundleContext getBundleContext()
    {
        return m_context;
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException
    {
        m_context.addServiceListener(listener, filter);
    }

    @Override
    public void addServiceListener(ServiceListener listener)
    {
        m_context.addServiceListener(listener);
    }

    @Override
    public void removeServiceListener(ServiceListener listener)
    {
        m_context.removeServiceListener(listener);
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties)
    {
        return m_context.registerService(clazzes, service, properties);
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties)
    {
        return m_context.registerService(clazz, service, properties);
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        return m_context.getServiceReferences(clazz, filter);
    }

    @Override
    public ServiceReference<?> getServiceReference(String clazz)
    {
        return m_context.getServiceReference(clazz);
    }

    @Override
    public <S> S getService(ServiceReference<S> reference)
    {
        return m_context.getService(reference);
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference)
    {
        return m_context.ungetService(reference);
    }

    private static class FrameworkStartLevelImpl implements FrameworkStartLevel, BundleAware
    {

        private Bundle bundle;

        @Override
        public void setBundle(Bundle bundle)
        {
            this.bundle = bundle;
        }

        @Override
        public int getStartLevel()
        {
            return 0;
        }

        @Override
        public void setStartLevel(int startlevel, FrameworkListener... listeners)
        {
        }

        @Override
        public int getInitialBundleStartLevel()
        {
            return 0;
        }

        @Override
        public void setInitialBundleStartLevel(int startlevel)
        {
        }

        @Override
        public Bundle getBundle()
        {
            return bundle;
        }
    }

    private static class StartLevelImpl implements StartLevel
    {
        @Override
        public void setStartLevel(int startlevel)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void setInitialBundleStartLevel(int startlevel)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void setBundleStartLevel(Bundle bundle, int startlevel)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean isBundlePersistentlyStarted(Bundle bundle)
        {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public boolean isBundleActivationPolicyUsed(Bundle bundle)
        {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int getStartLevel()
        {
            // TODO Auto-generated method stub
            return 1;
        }

        @Override
        public int getInitialBundleStartLevel()
        {
            // TODO Auto-generated method stub
            return 1;
        }

        @Override
        public int getBundleStartLevel(Bundle bundle)
        {
            // TODO Auto-generated method stub
            return 1;
        }
    }

    private class PackageAdminImpl implements PackageAdmin
    {

        @Override
        public boolean resolveBundles(Bundle[] bundles)
        {
            return true;
        }

        @Override
        public void refreshPackages(Bundle[] bundles)
        {
            FrameworkEvent event = new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, m_bundles.get(0l), null);
            m_dispatcher.fireFrameworkEvent(event);
        }

        @Override
        public RequiredBundle[] getRequiredBundles(String symbolicName)
        {
            List list = new ArrayList();
            for (Bundle bundle : PojoSR.this.m_bundles.values())
            {
                if ((symbolicName == null) || (symbolicName.equals(bundle.getSymbolicName())))
                {
                    list.add(new RequiredBundleImpl(bundle));
                }
            }
            return (list.isEmpty())
                    ? null
                    : (RequiredBundle[]) list.toArray(new RequiredBundle[list.size()]);
        }

        @Override
        public Bundle[] getHosts(Bundle bundle)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle[] getFragments(Bundle bundle)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ExportedPackage[] getExportedPackages(String name)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ExportedPackage[] getExportedPackages(Bundle bundle)
        {
            List<ExportedPackage> list = new ArrayList<ExportedPackage>();
            // If a bundle is specified, then return its
            // exported packages.
            if (bundle != null)
            {
                getExportedPackages(bundle, list);
            }
            // Otherwise return all exported packages.
            else
            {
                for (Bundle b : m_bundles.values())
                {
                    getExportedPackages(b, list);
                }
            }
            return list.isEmpty() ? null : list.toArray(new ExportedPackage[list.size()]);
        }

        private void getExportedPackages(Bundle bundle, List<ExportedPackage> list)
        {
            // Since a bundle may have many revisions associated with it,
            // one for each revision in the cache, search each revision
            // to get all exports.
            for (BundleCapability cap : bundle.adapt(BundleWiring.class).getCapabilities(null))
            {
                if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    list.add(new ExportedPackageImpl(cap));
                }
            }
        }

        @Override
        public ExportedPackage getExportedPackage(String name)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle[] getBundles(String symbolicName, String versionRange)
        {
            Set<Bundle> result = new HashSet<Bundle>();
            VersionRange range = versionRange != null ? new VersionRange(versionRange) : null;
            for (Bundle bundle : m_bundles.values())
            {
                if (symbolicName != null && !bundle.getSymbolicName().equals(symbolicName))
                {
                    continue;
                }
                if (range != null && !range.includes(bundle.getVersion()))
                {
                    continue;
                }
                result.add(bundle);
            }
            return result.isEmpty() ? null : result.toArray(new Bundle[result.size()]);
        }

        @Override
        public int getBundleType(Bundle bundle)
        {
            return bundle.adapt(BundleRevision.class).getTypes();
        }

        @Override
        public Bundle getBundle(Class clazz)
        {
            return m_context.getBundle();
        }
    }
}
