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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;

import junit.framework.TestCase;

public class ConcurrentBundleUpdateTest extends TestCase 
{
    public void testConcurrentBundleUpdate() throws Exception
    {
        Map params = new HashMap();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0,"
            + "org.osgi.service.packageadmin; version=1.2.0,"
            + "org.osgi.service.startlevel; version=1.1.0,"
            + "org.osgi.util.tracker; version=1.3.3,"
            + "org.osgi.service.url; version=1.0.0");
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);
        
        
        
        try
        {
            Felix felix = new Felix(params);
            try
            {
                felix.init();
                felix.start();
                
                String mf = "Bundle-SymbolicName: test.concurrent.bundleupdate\n"
                        + "Bundle-Version: 1.0.0\n"
                        + "Bundle-ManifestVersion: 2\n"
                        + "Import-Package: org.osgi.framework\n"
                        + "Manifest-Version: 1.0\n"
                        + "Bundle-Activator: " + ConcurrentBundleUpdaterActivator.class.getName() + "\n\n";
                
                final BundleImpl updater = (BundleImpl) felix.getBundleContext().installBundle(createBundle(mf, ConcurrentBundleUpdaterActivator.class).toURI().toURL().toString());
                
                final Semaphore step = new Semaphore(0);
                SynchronousBundleListener listenerStarting = new SynchronousBundleListener() 
                {
                    
                    @Override
                    public void bundleChanged(BundleEvent event) 
                    {
                        if (event.getBundle().equals(updater) && event.getType() == BundleEvent.STARTING)
                        {
                            step.release();
                        }
                    }
                };
                felix.getBundleContext().addBundleListener(listenerStarting);
                new Thread()
                {
                    public void run() 
                    {
                        try
                        {
                            updater.start();
                        }
                        catch (Exception ex) 
                        {
                            
                        }
                    }
                }.start();
                
                assertTrue(step.tryAcquire(1, TimeUnit.SECONDS));
                
                felix.getBundleContext().removeBundleListener(listenerStarting);
                
                assertEquals(Bundle.STARTING, updater.getState());
                assertEquals(0, step.availablePermits());
                
                new Thread() 
                {
                    public void run() 
                    {
                        try 
                        {
                            step.release();
                            updater.update();
                            step.release();
                        }
                        catch (Exception ex)
                        {
                        }
                    }
                }.start();
                assertTrue(step.tryAcquire(1, TimeUnit.SECONDS));
                SynchronousBundleListener listenerStarted = new SynchronousBundleListener() 
                {    
                    @Override
                    public void bundleChanged(BundleEvent event) 
                    {
                        if (event.getBundle().equals(updater) && event.getType() == BundleEvent.STARTED)
                        {
                            step.release();
                        }
                        if (event.getBundle().equals(updater) && event.getType() == BundleEvent.STOPPING)
                        {
                            step.release();
                        }
                    }
                };
                felix.getBundleContext().addBundleListener(listenerStarted);
                
                ((Runnable) updater.getActivator()).run();
                
                assertTrue(step.tryAcquire(2, 1, TimeUnit.SECONDS));
                
                felix.getBundleContext().removeBundleListener(listenerStarted);
                
                assertEquals(0, step.availablePermits());
                
                assertEquals(Bundle.STOPPING, updater.getState());
                
                felix.getBundleContext().addBundleListener(listenerStarting);

                ((Runnable) updater.getActivator()).run();
            
                assertTrue(step.tryAcquire(1, TimeUnit.SECONDS));
                
                felix.getBundleContext().removeBundleListener(listenerStarting);
                
                assertEquals(Bundle.STARTING, updater.getState());
                
                ((Runnable) updater.getActivator()).run();
                
                assertTrue(step.tryAcquire(1, TimeUnit.SECONDS));
                
                assertEquals(Bundle.ACTIVE, updater.getState());
                
                ((Runnable) updater.getActivator()).run();
                
                updater.uninstall();
                
                assertEquals(Bundle.UNINSTALLED, updater.getState());
                
                try 
                {
                    updater.update();
                    fail("Expected exception on update of uninstalled bundle");
                }
                catch (IllegalStateException expected) {
                    
                }
            }
            finally 
            {
                felix.stop();
                felix.waitForStop(1000);
            }
        } 
        finally
        {
            delete(cacheDir);
        }
    }
    
    public void testConcurrentBundleCycleUpdate() throws Exception
    {
        Map params = new HashMap();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0,"
            + "org.osgi.service.packageadmin; version=1.2.0,"
            + "org.osgi.service.startlevel; version=1.1.0,"
            + "org.osgi.util.tracker; version=1.3.3,"
            + "org.osgi.service.url; version=1.0.0");
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);
        
        
        
        try
        {
            Felix felix = new Felix(params);
            try
            {
                felix.init();
                felix.start();
                
                String mf = "Bundle-SymbolicName: test.concurrent.bundleupdate\n"
                        + "Bundle-Version: 1.0.0\n"
                        + "Bundle-ManifestVersion: 2\n"
                        + "Import-Package: org.osgi.framework\n"
                        + "Manifest-Version: 1.0\n"
                        + "Bundle-Activator: " + ConcurrentBundleUpdaterCycleActivator.class.getName() + "\n\n";
                
                final BundleImpl updater = (BundleImpl) felix.getBundleContext().installBundle(createBundle(mf, ConcurrentBundleUpdaterCycleActivator.class).toURI().toURL().toString());
                
                final Semaphore step = new Semaphore(0);
                SynchronousBundleListener listenerStarting = new SynchronousBundleListener() 
                {
                    @Override
                    public void bundleChanged(BundleEvent event) 
                    {
                        if (event.getBundle().equals(updater) && event.getType() == BundleEvent.STARTING)
                        {
                            step.release();
                        }
                    }
                };
                felix.getBundleContext().addBundleListener(listenerStarting);
                new Thread()
                {
                    public void run() 
                    {
                        try 
                        {
                            updater.start();
                        } 
                        catch (Exception ex)
                        {
                            step.release();
                        }
                    }
                }.start();
                
                assertTrue(step.tryAcquire(1, TimeUnit.SECONDS));
                
                felix.getBundleContext().removeBundleListener(listenerStarting);
                
                assertEquals(Bundle.STARTING, updater.getState());
                assertEquals(0, step.availablePermits());

                ((Runnable) updater.getActivator()).run();
                
                assertTrue(step.tryAcquire(1, TimeUnit.SECONDS));
                assertEquals(Bundle.RESOLVED, updater.getState());
                
                updater.uninstall();
                
                assertEquals(Bundle.UNINSTALLED, updater.getState());
                
                try 
                {
                    updater.update();
                    fail("Expected exception on update of uninstalled bundle");
                }
                catch (IllegalStateException expected) {
                    
                }
            }
            finally 
            {
                felix.stop();
                felix.waitForStop(1000);
            }
        } 
        finally
        {
            delete(cacheDir);
        }
    }
    
    public static final class ConcurrentBundleUpdaterActivator implements BundleActivator, Runnable 
    {
        Semaphore semaphore = new Semaphore(0);
        
        private BundleContext context;
        
        @Override
        public void start(BundleContext context) throws Exception 
        {
            this.context = context;
            if (!semaphore.tryAcquire(1, TimeUnit.SECONDS))
            {
                throw new BundleException("Timeout");
            }
        }

        @Override
        public void stop(BundleContext context) throws Exception 
        {
            this.context = context;
            if (!semaphore.tryAcquire(1, TimeUnit.SECONDS))
            {
                throw new BundleException("Timeout");
            }
        }

        @Override
        public void run() 
        {
            semaphore.release();
        }
        
    }
    
    public static final class ConcurrentBundleUpdaterCycleActivator implements BundleActivator, Runnable 
    {
        Semaphore semaphore = new Semaphore(0);
        
        private BundleContext context;
        
        @Override
        public void start(BundleContext context) throws Exception 
        {
            this.context = context;
            if (!semaphore.tryAcquire(1, TimeUnit.SECONDS))
            {
                throw new BundleException("Timeout");
            }
            context.getBundle().update();
        }

        @Override
        public void stop(BundleContext context) throws Exception {
            this.context = context;
            if (!semaphore.tryAcquire(1, TimeUnit.SECONDS))
            {
                throw new BundleException("Timeout");
            }
        }

        @Override
        public void run() 
        {
            semaphore.release();
        }
        
    }
    
    private static File createBundle(String manifest, Class... classes) throws IOException
    {
        File f = File.createTempFile("felix-bundle", ".jar");
        f.deleteOnExit();

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);

        for (Class clazz : classes)
        {
            String path = clazz.getName().replace('.', '/') + ".class";
            os.putNextEntry(new ZipEntry(path));

            InputStream is = clazz.getClassLoader().getResourceAsStream(path);
            byte[] buffer = new byte[8 * 1024];
            for (int i = is.read(buffer); i != -1; i = is.read(buffer))
            {
                os.write(buffer, 0, i);
            }
            is.close();
            os.closeEntry();
        }
        os.close();
        return f;
    }
    
    private static void delete(File file) throws IOException
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                delete(child);
            }
        }
        file.delete();
    }
}
