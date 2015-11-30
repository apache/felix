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

package org.apache.felix.webconsole.plugins.scriptconsole.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * It is based on org.apache.sling.scripting.core.impl.ScriptEngineManagerFactory
 */
class ScriptEngineManager implements BundleListener, ServiceTrackerCustomizer
{
    private static final String ENGINE_FACTORY_SERVICE = "META-INF/services/"
        + ScriptEngineFactory.class.getName();
    private final Set<Bundle> engineSpiBundles = new HashSet<Bundle>();
    private final Map<ServiceReference, ScriptEngineFactoryState> engineSpiServices
            = new ConcurrentHashMap<ServiceReference, ScriptEngineFactoryState>();

    private final Logger log;
    private EngineManagerState state = new EngineManagerState();

    /**
     * ServiceTracker for ScriptEngineFactory
     */
    private ServiceTracker scriptFactoryTracker;
    private final BundleContext context;

    public ScriptEngineManager(BundleContext context, Logger logger)
    {
        this.log = logger;
        this.context = context;

        this.context.addBundleListener(this);

        Bundle[] bundles = this.context.getBundles();
        synchronized (this.engineSpiBundles) {
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE
                        && bundle.getEntry(ENGINE_FACTORY_SERVICE) != null) {
                    this.engineSpiBundles.add(bundle);
                }
            }
        }

        // create a script engine manager
        this.refreshScriptEngineManager();

        this.scriptFactoryTracker = new ServiceTracker(context,
            ScriptEngineFactory.class.getName(), this);
        this.scriptFactoryTracker.open();
    }

    public List<ScriptEngineFactory> getEngineFactories()
    {
        return state.factories;
    }

    public ScriptEngine getEngineByExtension(String extension) {
        ScriptEngineFactory factory = state.extensionAssociations.get(extension);
        if (factory == null) return null;

        ScriptEngine engine = factory.getScriptEngine();

        //We do not support global scope for now
        //engine.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

        return engine;
    }

    // ---------- BundleListener interface -------------------------------------

    public void bundleChanged(BundleEvent event)
    {
        if (event.getType() == BundleEvent.STARTED
            && event.getBundle().getEntry(ENGINE_FACTORY_SERVICE) != null)
        {
            synchronized (this.engineSpiBundles)
            {
                this.engineSpiBundles.add(event.getBundle());
            }
            this.refreshScriptEngineManager();
        }
        else if (event.getType() == BundleEvent.STOPPED)
        {
            boolean refresh;
            synchronized (this.engineSpiBundles)
            {
                refresh = this.engineSpiBundles.remove(event.getBundle());
            }
            if (refresh)
            {
                this.refreshScriptEngineManager();
            }
        }
    }

    // ---------- ServiceTrackerCustomizer interface -------------------------------------

    public Object addingService(ServiceReference reference) {
        ScriptEngineFactory service = (ScriptEngineFactory) context.getService(reference);
        engineSpiServices.put(reference,new ScriptEngineFactoryState(service,getServiceProperties(reference)));
        refreshScriptEngineManager();
        return service;
    }

    public void modifiedService(ServiceReference reference, Object service)
    {
        ScriptEngineFactoryState state = engineSpiServices.get(reference);
        state.properties = getServiceProperties(reference);
        refreshScriptEngineManager();
    }

    public void removedService(ServiceReference reference, Object service)
    {
        context.ungetService(reference);
        engineSpiServices.remove(reference);
        refreshScriptEngineManager();

    }

    private void refreshScriptEngineManager()
    {
        EngineManagerState tmp = new EngineManagerState();
        // register script engines from bundles
        final SortedSet<Object> extensions = new TreeSet<Object>();
        synchronized (this.engineSpiBundles)
        {
            for (final Bundle bundle : this.engineSpiBundles)
            {
                extensions.addAll(registerFactories(tmp, bundle));
            }
        }

        // register script engines from registered services
        synchronized (this.engineSpiServices)
        {
            for (final ScriptEngineFactoryState state : this.engineSpiServices.values())
            {
                extensions.addAll(registerFactory(tmp, state.scriptEngineFactory,
                    state.properties));
            }
        }

        synchronized (this){
            this.state = tmp;
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<?> registerFactories(final EngineManagerState mgr,
        final Bundle bundle)
    {
        URL url = bundle.getEntry(ENGINE_FACTORY_SERVICE);
        InputStream ins = null;
        final SortedSet<String> extensions = new TreeSet<String>();
        try
        {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            for (String className : getClassNames(reader))
            {
                try
                {
                    Class<ScriptEngineFactory> clazz = bundle.loadClass(className);
                    ScriptEngineFactory spi = clazz.newInstance();
                    registerFactory(mgr, spi, null);
                    extensions.addAll(spi.getExtensions());
                }
                catch (Throwable t)
                {
                    log.log(LogService.LOG_ERROR,
                            "Cannot register ScriptEngineFactory " + className, t);
                }
            }

        }
        catch (IOException ioe)
        {
            // ignore
        }
        finally
        {
            IOUtils.closeQuietly(ins);
        }

        return extensions;
    }

    private Collection<?> registerFactory(final EngineManagerState mgr,
        final ScriptEngineFactory factory, final Map<Object, Object> props)
    {
        log.log(
            LogService.LOG_INFO,
            String.format("Adding ScriptEngine %s, %s for language %s, %s",
                factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion()));

        mgr.factories.add(factory);
        mgr.factoryProperties.put(factory, props);
        for (Object ext : factory.getExtensions()) {
            mgr.extensionAssociations.put((String) ext, factory);
        }
        return factory.getExtensions();
    }

    public void dispose()
    {
        if (scriptFactoryTracker != null)
        {
            scriptFactoryTracker.close();
        }
    }

    static List<String> getClassNames(BufferedReader reader) throws IOException {
        List<String> classNames = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null)
        {
            if (!line.startsWith("#") && line.trim().length() > 0)
            {
                int indexOfHash = line.indexOf('#');
                if (indexOfHash >= 0)
                {
                    line = line.substring(0, indexOfHash);
                }
                line = line.trim();
                classNames.add(line);
            }
        }
        return classNames;
    }

    private static Map<Object, Object> getServiceProperties(ServiceReference reference)
    {
        Map<Object, Object> props = new HashMap<Object, Object>();
        for (String key : reference.getPropertyKeys())
        {
            props.put(key, reference.getProperty(key));
        }
        return props;
    }

    private static class ScriptEngineFactoryState
    {
        final ScriptEngineFactory scriptEngineFactory;
        Map<Object, Object> properties;

        private ScriptEngineFactoryState(ScriptEngineFactory scriptEngineFactory, Map<Object, Object> properties)
        {
            this.scriptEngineFactory = scriptEngineFactory;
            this.properties = properties;
        }
    }

    private static class EngineManagerState
    {
        private final List<ScriptEngineFactory> factories = new ArrayList<ScriptEngineFactory>();
        private final Map<ScriptEngineFactory, Map<Object, Object>> factoryProperties
                            = new HashMap<ScriptEngineFactory, Map<Object, Object>>();
        private final Map<String, ScriptEngineFactory> extensionAssociations
                            = new HashMap<String, ScriptEngineFactory>();

    }
}
