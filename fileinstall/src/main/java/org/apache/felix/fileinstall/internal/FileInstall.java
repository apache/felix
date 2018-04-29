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
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.util.*;
<<<<<<< HEAD
import java.util.concurrent.CopyOnWriteArrayList;
=======
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactTransformer;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.felix.fileinstall.internal.Util.Logger;
<<<<<<< HEAD
import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
=======
import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This clever little bundle watches a directory and will install any jar file
 * if finds in that directory (as long as it is a valid bundle and not a
 * fragment).
 *
 */
<<<<<<< HEAD
public class FileInstall implements BundleActivator, ServiceTrackerCustomizer, FrameworkListener
{
    static ServiceTracker padmin;
    static ServiceTracker startLevel;
    static Runnable cmSupport;
    static final Map /* <ServiceReference, ArtifactListener> */ listeners = new TreeMap /* <ServiceReference, ArtifactListener> */();
    static final BundleTransformer bundleTransformer = new BundleTransformer();
    BundleContext context;
    Map watchers = new HashMap();
    ServiceTracker listenersTracker;
    static boolean initialized;
    static final Object barrier = new Object();
    static final Object refreshLock = new Object();
    ServiceRegistration urlHandlerRegistration;
=======
public class FileInstall implements BundleActivator, ServiceTrackerCustomizer
{
    Runnable cmSupport;
    final Map<ServiceReference, ArtifactListener> listeners = new TreeMap<ServiceReference, ArtifactListener>();
    final BundleTransformer bundleTransformer = new BundleTransformer();
    BundleContext context;
    final Map<String, DirectoryWatcher> watchers = new HashMap<String, DirectoryWatcher>();
    ServiceTracker listenersTracker;
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    ServiceRegistration urlHandlerRegistration;
    volatile boolean stopped;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    public void start(BundleContext context) throws Exception
    {
        this.context = context;
<<<<<<< HEAD
        context.addFrameworkListener(this);

        Hashtable props = new Hashtable();
        props.put("url.handler.protocol", JarDirUrlHandler.PROTOCOL);
        urlHandlerRegistration = context.registerService(org.osgi.service.url.URLStreamHandlerService.class.getName(), new JarDirUrlHandler(), props);

        padmin = new ServiceTracker(context, PackageAdmin.class.getName(), null);
        padmin.open();
        startLevel = new ServiceTracker(context, StartLevel.class.getName(), null);
        startLevel.open();

        String flt = "(|(" + Constants.OBJECTCLASS + "=" + ArtifactInstaller.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + ArtifactTransformer.class.getName() + ")"
                     + "(" + Constants.OBJECTCLASS + "=" + ArtifactUrlTransformer.class.getName() + "))";
        listenersTracker = new ServiceTracker(context, FrameworkUtil.createFilter(flt), this);
        listenersTracker.open();

        try
        {
            cmSupport = new ConfigAdminSupport(context, this);
        }
        catch (NoClassDefFoundError e)
        {
            Util.log(context, Util.getGlobalLogLevel(context), Logger.LOG_DEBUG,
                "ConfigAdmin is not available, some features will be disabled", e);
        }

        // Created the initial configuration
        Hashtable ht = new Hashtable();

        set(ht, DirectoryWatcher.POLL);
        set(ht, DirectoryWatcher.DIR);
        set(ht, DirectoryWatcher.LOG_LEVEL);
        set(ht, DirectoryWatcher.FILTER);
        set(ht, DirectoryWatcher.TMPDIR);
        set(ht, DirectoryWatcher.START_NEW_BUNDLES);
        set(ht, DirectoryWatcher.USE_START_TRANSIENT);
        set(ht, DirectoryWatcher.NO_INITIAL_DELAY);
        set(ht, DirectoryWatcher.START_LEVEL);

        // check if dir is an array of dirs
        String dirs = (String)ht.get(DirectoryWatcher.DIR);
        if ( dirs != null && dirs.indexOf(',') != -1 )
        {
            StringTokenizer st = new StringTokenizer(dirs, ",");
            int index = 0;
            while ( st.hasMoreTokens() )
            {
                final String dir = st.nextToken().trim();
                ht.put(DirectoryWatcher.DIR, dir);

                String name = "initial";
                if ( index > 0 ) name = name + index;
                updated(name, new Hashtable(ht));

                index++;
            }
        }
        else
        {
            updated("initial", ht);
        }
        // now notify all the directory watchers to proceed
        // We need this to avoid race conditions observed in FELIX-2791
        synchronized (barrier) {
            initialized = true;
            barrier.notifyAll();
=======
        lock.writeLock().lock();

        try
        {
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("url.handler.protocol", JarDirUrlHandler.PROTOCOL);
            urlHandlerRegistration = context.registerService(org.osgi.service.url.URLStreamHandlerService.class.getName(), new JarDirUrlHandler(), props);

            String flt = "(|(" + Constants.OBJECTCLASS + "=" + ArtifactInstaller.class.getName() + ")"
                    + "(" + Constants.OBJECTCLASS + "=" + ArtifactTransformer.class.getName() + ")"
                    + "(" + Constants.OBJECTCLASS + "=" + ArtifactUrlTransformer.class.getName() + "))";
            listenersTracker = new ServiceTracker(context, FrameworkUtil.createFilter(flt), this);
            listenersTracker.open();

            try
            {
                cmSupport = new ConfigAdminSupport(context, this);
            }
            catch (NoClassDefFoundError e)
            {
                Util.log(context, Logger.LOG_DEBUG,
                        "ConfigAdmin is not available, some features will be disabled", e);
            }

            // Created the initial configuration
            Hashtable<String, String> ht = new Hashtable<String, String>();

            set(ht, DirectoryWatcher.POLL);
            set(ht, DirectoryWatcher.DIR);
            set(ht, DirectoryWatcher.LOG_LEVEL);
            set(ht, DirectoryWatcher.LOG_DEFAULT);
            set(ht, DirectoryWatcher.FILTER);
            set(ht, DirectoryWatcher.TMPDIR);
            set(ht, DirectoryWatcher.START_NEW_BUNDLES);
            set(ht, DirectoryWatcher.USE_START_TRANSIENT);
            set(ht, DirectoryWatcher.USE_START_ACTIVATION_POLICY);
            set(ht, DirectoryWatcher.NO_INITIAL_DELAY);
            set(ht, DirectoryWatcher.DISABLE_CONFIG_SAVE);
            set(ht, DirectoryWatcher.ENABLE_CONFIG_SAVE);
            set(ht, DirectoryWatcher.CONFIG_ENCODING);
            set(ht, DirectoryWatcher.START_LEVEL);
            set(ht, DirectoryWatcher.ACTIVE_LEVEL);
            set(ht, DirectoryWatcher.UPDATE_WITH_LISTENERS);
            set(ht, DirectoryWatcher.OPTIONAL_SCOPE);
            set(ht, DirectoryWatcher.FRAGMENT_SCOPE);
            set(ht, DirectoryWatcher.DISABLE_NIO2);
            set(ht, DirectoryWatcher.SUBDIR_MODE);

            // check if dir is an array of dirs
            String dirs = ht.get(DirectoryWatcher.DIR);
            if (dirs != null && dirs.indexOf(',') != -1)
            {
                StringTokenizer st = new StringTokenizer(dirs, ",");
                int index = 0;
                while (st.hasMoreTokens())
                {
                    final String dir = st.nextToken().trim();
                    ht.put(DirectoryWatcher.DIR, dir);

                    String name = "initial";
                    if (index > 0) name = name + index;
                    updated(name, new Hashtable<String, String>(ht));

                    index++;
                }
            }
            else
            {
                updated("initial", ht);
            }
        }
        finally
        {
            // now notify all the directory watchers to proceed
            // We need this to avoid race conditions observed in FELIX-2791
            lock.writeLock().unlock();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    public Object addingService(ServiceReference serviceReference)
    {
        ArtifactListener listener = (ArtifactListener) context.getService(serviceReference);
        addListener(serviceReference, listener);
        return listener;
    }
    public void modifiedService(ServiceReference reference, Object service)
    {
        removeListener(reference, (ArtifactListener) service);
        addListener(reference, (ArtifactListener) service);
    }
    public void removedService(ServiceReference serviceReference, Object service)
    {
        removeListener(serviceReference, (ArtifactListener) service);
    }

    // Adapted for FELIX-524
<<<<<<< HEAD
    private void set(Hashtable ht, String key)
    {
        Object o = context.getProperty(key);
=======
    private void set(Hashtable<String, String> ht, String key)
    {
        String o = context.getProperty(key);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (o == null)
        {
           o = System.getProperty(key.toUpperCase().replace('.', '_'));
            if (o == null)
            {
                return;
            }
        }
        ht.put(key, o);
    }

    public void stop(BundleContext context) throws Exception
    {
<<<<<<< HEAD
        synchronized (barrier) {
            initialized = false;
        }
        urlHandlerRegistration.unregister();
        List /*<DirectoryWatcher>*/ toClose = new ArrayList /*<DirectoryWatcher>*/();
        synchronized (watchers)
        {
            toClose.addAll(watchers.values());
            watchers.clear();
        }
        for (Iterator w = toClose.iterator(); w.hasNext();)
        {
            try
            {
                DirectoryWatcher dir = (DirectoryWatcher) w.next();
                dir.close();
            }
            catch (Exception e)
            {
                // Ignore
            }
        }
        if (listenersTracker != null)
        {
            listenersTracker.close();
        }
        if (cmSupport != null)
        {
            cmSupport.run();
        }
        if (padmin != null)
        {
            padmin.close();
=======
        lock.writeLock().lock();
        try
        {
            urlHandlerRegistration.unregister();
            List<DirectoryWatcher> toClose = new ArrayList<DirectoryWatcher>();
            synchronized (watchers)
            {
                toClose.addAll(watchers.values());
                watchers.clear();
            }
            for (DirectoryWatcher aToClose : toClose)
            {
                try
                {
                    aToClose.close();
                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
            if (listenersTracker != null)
            {
                listenersTracker.close();
            }
            if (cmSupport != null)
            {
                cmSupport.run();
            }
        }
        finally
        {
            stopped = true;
            lock.writeLock().unlock();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    public void deleted(String pid)
    {
        DirectoryWatcher watcher;
        synchronized (watchers)
        {
<<<<<<< HEAD
            watcher = (DirectoryWatcher) watchers.remove(pid);
=======
            watcher = watchers.remove(pid);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        if (watcher != null)
        {
            watcher.close();
        }
    }

<<<<<<< HEAD
    public void updated(String pid, Dictionary properties)
    {
        InterpolationHelper.performSubstitution(new DictionaryAsMap(properties), context);
        DirectoryWatcher watcher = null;
        synchronized (watchers)
        {
            watcher = (DirectoryWatcher) watchers.get(pid);
=======
    public void updated(String pid, Map<String, String> properties)
    {
        InterpolationHelper.performSubstitution(properties, context);
        DirectoryWatcher watcher;
        synchronized (watchers)
        {
            watcher = watchers.get(pid);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            if (watcher != null && watcher.getProperties().equals(properties))
            {
                return;
            }
        }
        if (watcher != null)
        {
            watcher.close();
        }
<<<<<<< HEAD
        watcher = new DirectoryWatcher(properties, context);
=======
        watcher = new DirectoryWatcher(this, properties, context);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        watcher.setDaemon(true);
        synchronized (watchers)
        {
            watchers.put(pid, watcher);
        }
        watcher.start();
    }

    public void updateChecksum(File file)
    {
<<<<<<< HEAD
        List /*<DirectoryWatcher>*/ toUpdate = new ArrayList /*<DirectoryWatcher>*/();
=======
        List<DirectoryWatcher> toUpdate = new ArrayList<DirectoryWatcher>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        synchronized (watchers)
        {
            toUpdate.addAll(watchers.values());
        }
<<<<<<< HEAD
        for (Iterator w = toUpdate.iterator(); w.hasNext();)
        {
            DirectoryWatcher watcher = (DirectoryWatcher) w.next();
=======
        for (DirectoryWatcher watcher : toUpdate)
        {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            watcher.scanner.updateChecksum(file);
        }
    }

    private void addListener(ServiceReference reference, ArtifactListener listener)
    {
        synchronized (listeners)
        {
            listeners.put(reference, listener);
        }
        
        long currentStamp = reference.getBundle().getLastModified();

<<<<<<< HEAD
        List /*<DirectoryWatcher>*/ toNotify = new ArrayList /*<DirectoryWatcher>*/();
=======
        List<DirectoryWatcher> toNotify = new ArrayList<DirectoryWatcher>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        synchronized (watchers)
        {
            toNotify.addAll(watchers.values());
        }
<<<<<<< HEAD
        for (Iterator w = toNotify.iterator(); w.hasNext();)
        {
            DirectoryWatcher dir = (DirectoryWatcher) w.next();
            dir.addListener( listener, currentStamp );
=======
        for (DirectoryWatcher dir : toNotify)
        {
            dir.addListener(listener, currentStamp);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

    private void removeListener(ServiceReference reference, ArtifactListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(reference);
        }
<<<<<<< HEAD
        List /*<DirectoryWatcher>*/ toNotify = new ArrayList /*<DirectoryWatcher>*/();
=======
        List<DirectoryWatcher> toNotify = new ArrayList<DirectoryWatcher>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        synchronized (watchers)
        {
            toNotify.addAll(watchers.values());
        }
<<<<<<< HEAD
        for (Iterator w = toNotify.iterator(); w.hasNext();)
        {
            DirectoryWatcher dir = (DirectoryWatcher) w.next();
=======
        for (DirectoryWatcher dir : toNotify)
        {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            dir.removeListener(listener);
        }
    }

<<<<<<< HEAD
    static List getListeners()
    {
        synchronized (listeners)
        {
            List l = new ArrayList(listeners.values());
=======
    List<ArtifactListener> getListeners()
    {
        synchronized (listeners)
        {
            List<ArtifactListener> l = new ArrayList<ArtifactListener>(listeners.values());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            Collections.reverse(l);
            l.add(bundleTransformer);
            return l;
        }
    }

    /**
     * Convenience to refresh the packages
     */
<<<<<<< HEAD
    static void refresh(Bundle[] bundles)
    {
        PackageAdmin padmin = getPackageAdmin();
        if (padmin != null)
        {
            synchronized (refreshLock) {
                padmin.refreshPackages(bundles);
                try {
                    refreshLock.wait(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void frameworkEvent(FrameworkEvent event)
    {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED
                || event.getType() == FrameworkEvent.ERROR)
        {
            synchronized (refreshLock)
            {
                refreshLock.notifyAll();
            }
        }
    }

    static PackageAdmin getPackageAdmin()
    {
        return getPackageAdmin(10000);
    }

    static PackageAdmin getPackageAdmin(long timeout)
    {
        try
        {
            return (PackageAdmin) padmin.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static StartLevel getStartLevel()
    {
        return getStartLevel(10000);
    }

    static StartLevel getStartLevel(long timeout)
    {
        try
        {
            return (StartLevel) startLevel.waitForService(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static class ConfigAdminSupport implements Runnable
=======
    static void refresh(Bundle systemBundle, Collection<Bundle> bundles) throws InterruptedException
    {
        final CountDownLatch latch = new CountDownLatch(1);
        FrameworkWiring wiring = systemBundle.adapt(FrameworkWiring.class);
        wiring.refreshBundles(bundles, new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                latch.countDown();
            }
        });
        latch.await();
    }

    private class ConfigAdminSupport implements Runnable
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        private Tracker tracker;
        private ServiceRegistration registration;

        private ConfigAdminSupport(BundleContext context, FileInstall fileInstall)
        {
            tracker = new Tracker(context, fileInstall);
<<<<<<< HEAD
            Hashtable props = new Hashtable();
=======
            Hashtable<String, Object> props = new Hashtable<String, Object>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            props.put(Constants.SERVICE_PID, tracker.getName());
            registration = context.registerService(ManagedServiceFactory.class.getName(), tracker, props);
            tracker.open();
        }

        public void run()
        {
<<<<<<< HEAD
            registration.unregister();
            tracker.close();
        }

        private class Tracker extends ServiceTracker implements ManagedServiceFactory {

            private final FileInstall fileInstall;
            private final Set configs = Collections.synchronizedSet(new HashSet());
            private ConfigInstaller configInstaller;
=======
            tracker.close();
            registration.unregister();
        }

        private class Tracker extends ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> implements ManagedServiceFactory {

            private final FileInstall fileInstall;
            private final Set<String> configs = Collections.synchronizedSet(new HashSet<String>());
            private final Map<Long, ConfigInstaller> configInstallers = new HashMap<Long, ConfigInstaller>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

            private Tracker(BundleContext bundleContext, FileInstall fileInstall)
            {
                super(bundleContext, ConfigurationAdmin.class.getName(), null);
                this.fileInstall = fileInstall;
            }

            public String getName()
            {
                return "org.apache.felix.fileinstall";
            }

<<<<<<< HEAD
            public void updated(String s, Dictionary dictionary) throws ConfigurationException
            {
                configs.add(s);
                fileInstall.updated(s, dictionary);
=======
            public void updated(String s, Dictionary<String, ?> dictionary) throws ConfigurationException
            {
                configs.add(s);
                Map<String, String> props = new HashMap<String, String>();
                for (Enumeration<String> e = dictionary.keys(); e.hasMoreElements();) {
                    String k = e.nextElement();
                    props.put(k, dictionary.get(k).toString());
                }
                fileInstall.updated(s, props);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            public void deleted(String s)
            {
                configs.remove(s);
                fileInstall.deleted(s);
            }

<<<<<<< HEAD
            public Object addingService(ServiceReference serviceReference)
            {
                ConfigurationAdmin cm = (ConfigurationAdmin) super.addingService(serviceReference);
                configInstaller = new ConfigInstaller(context, cm, fileInstall);
                configInstaller.init();
                return cm;
            }

            public void removedService(ServiceReference serviceReference, Object o)
            {
                Iterator iterator = configs.iterator();
                while (iterator.hasNext()) {
                    String s = (String) iterator.next();
                    fileInstall.deleted(s);
                    iterator.remove();
                }
                if (configInstaller != null)
                {
                    configInstaller.destroy();
                    configInstaller = null;
                }
                super.removedService(serviceReference, o);
=======
            public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> serviceReference)
            {
                lock.writeLock().lock();
                try
                {
                    if (stopped) {
                        return null;
                    }
                    ConfigurationAdmin cm = super.addingService(serviceReference);
                    long id = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
                    ConfigInstaller configInstaller = new ConfigInstaller(this.context, cm, fileInstall);
                    configInstaller.init();
                    configInstallers.put(id, configInstaller);
                    return cm;
                }
                finally
                {
                    lock.writeLock().unlock();
                }
            }

            public void removedService(ServiceReference<ConfigurationAdmin> serviceReference, ConfigurationAdmin o)
            {
                lock.writeLock().lock();
                try
                {
                    if (stopped) {
                        return;
                    }
                    Iterator iterator = configs.iterator();
                    while (iterator.hasNext())
                    {
                        String s = (String) iterator.next();
                        fileInstall.deleted(s);
                        iterator.remove();
                    }
                    long id = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
                    ConfigInstaller configInstaller = configInstallers.remove(id);
                    if (configInstaller != null)
                    {
                        configInstaller.destroy();
                    }
                    super.removedService(serviceReference, o);
                }
                finally
                {
                    lock.writeLock().unlock();
                }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
    }

}