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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Assume;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

import junit.framework.TestCase;

public class CycleDetectionWithWovenClassTest extends TestCase {
    
    public void testDoesBootdelegateForClassloaderClassload() throws Exception{
        withFelixDo(new ThrowingConsumer<Felix>() {
            @Override
            public void accept(Felix felix) throws Exception {
                BundleImpl bundle = (BundleImpl) felix.getBundleContext().installBundle(createBundle(
                        WeavingActivator.class, CycleDetectionWithWovenClassTest.class, Hook.class, getClass().getClassLoader().loadClass("org.apache.felix.framework.CycleDetectionWithWovenClassTest$WeavingActivator$1")
).toURI().toURL().toString());
                
                bundle.start();
                
                Runnable testClass = felix.getBundleContext().getService(
                        felix.getBundleContext().getServiceReference(Runnable.class));
                
                testClass.run();
            }
        });
    }
    
    
    public static class WeavingActivator implements BundleActivator, Runnable {
        private volatile BundleContext context;
        private volatile boolean wasNull = false;
        @Override
        public void start(final BundleContext context) throws Exception {
            this.context = context;
            context.registerService(WeavingHook.class.getName(), new ServiceFactory<WeavingHook>()
            {

                @Override
                public WeavingHook getService(Bundle bundle, ServiceRegistration<WeavingHook> registration)
                {
                    // TODO Auto-generated method stub
                    try
                    {
                        Class hook = getClass().getClassLoader().loadClass("org.apache.felix.framework.CycleDetectionWithWovenClassTest$Hook");
                        if (hook == null)
                        {
                            wasNull = true;
                            return null;
                        }
                        else {
                            return (WeavingHook) hook.newInstance();
                        }
                        
                    } 
                    catch (ClassNotFoundException e)
                    {
                        // This is what we expect.
                    }
                    catch (Exception ex) {
                        wasNull = true;
                    }
                    return null;
                }

                @Override
                public void ungetService(Bundle bundle, ServiceRegistration<WeavingHook> registration,
                        WeavingHook service)
                {
                    // TODO Auto-generated method stub
                    
                }
            }, null);
            
            context.registerService(Runnable.class, this, null);
        }

        @Override
        public void stop(BundleContext context) throws Exception {
            // TODO Auto-generated method stub
            
        }
        
        public void run() {
            try
            {
                ((Callable<Boolean>) context.getBundle().loadClass("org.apache.felix.framework.CycleDetectionWithWovenClassTest$Hook").newInstance()).call();
                
                if (wasNull)
                {
                    throw new IllegalStateException("Returned null from nested classload");
                }
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        
    }
    public static class Hook implements WeavingHook, Callable<Boolean> {

        private static boolean woven = false;
        @Override
        public void weave(WovenClass wovenClass)
        {
            woven = true;
        }

        @Override
        public Boolean call() throws Exception
        {
            return woven;
        }
        
    }
    
    private void withFelixDo(ThrowingConsumer<Felix> consumer) throws Exception {
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        try
        {
            Felix felix = getFramework(cacheDir);
            try
            {
                felix.init();
                felix.start();
                consumer.accept(felix);
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
    
    @FunctionalInterface
    private static interface ThrowingConsumer<T> {
        public void accept(T t) throws Exception;
    }
    
    
    private Felix getFramework(File cacheDir) {
        Map params = new HashMap();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0,"
            + "org.osgi.service.packageadmin; version=1.2.0,"
            + "org.osgi.service.startlevel; version=1.1.0,"
            + "org.osgi.util.tracker; version=1.3.3,"
            + "org.osgi.service.url; version=1.0.0,"
            + "org.osgi.framework.hooks.weaving");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);
        
        return new Felix(params);
    }
    
    private static File createBundle(Class activator, Class...classes) throws IOException
    {
        String mf = "Bundle-SymbolicName: " + activator.getName() +"\n"
                + "Bundle-Version: 1.0.0\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework,org.osgi.framework.hooks.weaving\n"
                + "Manifest-Version: 1.0\n"
                + "Bundle-Activator: " + activator.getName() + "\n\n";
        
        Class[] classesCombined;
        
        if (classes.length > 0) {
            List<Class> list = new ArrayList<Class>(Arrays.asList(classes));
            list.add(activator);
            classesCombined = list.toArray(new Class[0]);
        }
        else
        {
            classesCombined = new Class[]{activator};
        }
        return createBundle(mf,classesCombined);
        
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
