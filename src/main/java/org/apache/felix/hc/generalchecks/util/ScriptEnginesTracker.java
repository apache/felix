/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.generalchecks.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple service to track script engines available via osgi bundles that define META-INF/services/javax.script.ScriptEngineFactory, e.g. like groovy-all. */
@Component(immediate=true, service = ScriptEnginesTracker.class)
public class ScriptEnginesTracker implements BundleListener {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptEnginesTracker.class);

    private static final String ENGINE_FACTORY_SERVICE = "META-INF/services/"  + ScriptEngineFactory.class.getName();
    private final Map<String, ScriptEngineFactory> enginesByLanguage = new ConcurrentHashMap<String, ScriptEngineFactory>();
    private final Map<Bundle, List<String>> languagesByBundle = new ConcurrentHashMap<Bundle, List<String>>();

    /** ServiceTracker for ScriptEngineFactory */
    private BundleContext context;

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
        this.context.addBundleListener(this);
        registerInitialScriptEngineFactories();
    }
    
    @Deactivate
    public void deactivate() {
        this.context.removeBundleListener(this);
        
        enginesByLanguage.clear();
        languagesByBundle.clear();
    }

    public ScriptEngine getEngineByLanguage(String language) {
        ScriptEngineFactory factory = enginesByLanguage.get(language.toLowerCase());
        if (factory == null) {
            return null;
        }
        
        ScriptEngine engine = factory.getScriptEngine();
        return engine;
    }

    public Map<Bundle, List<String>> getLanguagesByBundle() {
        return languagesByBundle;
    }

    
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED && event.getBundle().getEntry(ENGINE_FACTORY_SERVICE) != null) {
            registerFactories(event.getBundle());
        } else if (event.getType() == BundleEvent.STOPPED) {
            unregisterFactories(event.getBundle());
        }
    }


    private void registerInitialScriptEngineFactories() {
        Bundle[] bundles = this.context.getBundles();
        for (Bundle bundle : bundles) {
            if ( bundle.getState() == Bundle.ACTIVE  && bundle.getEntry(ENGINE_FACTORY_SERVICE)!=null) {
                registerFactories(bundle);
            }
        }
    }

    private void registerFactories(Bundle bundle) {
        List<ScriptEngineFactory> scriptEngineFactoriesForBundle = getScriptEngineFactoriesForBundle(bundle);
        for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactoriesForBundle) {
            registerFactory(bundle, scriptEngineFactory);

        }
    }

    private void unregisterFactories(Bundle bundle) {
        List<String> languagesForBundle = languagesByBundle.get(bundle);
        if(languagesForBundle != null) {
            for (String lang : languagesForBundle) {
                ScriptEngineFactory removed = enginesByLanguage.remove(lang);
                LOG.info("Removing ScriptEngine {} for language {}", removed, lang);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<ScriptEngineFactory> getScriptEngineFactoriesForBundle(final Bundle bundle) {
        URL url = bundle.getEntry(ENGINE_FACTORY_SERVICE);
        InputStream ins = null;
        
        List<ScriptEngineFactory> scriptEngineFactoriesInBundle = new ArrayList<ScriptEngineFactory>();
        
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            for (String className : getClassNames(reader)) {
                try {
                    Class<ScriptEngineFactory> clazz = (Class<ScriptEngineFactory>) bundle.loadClass(className);
                    ScriptEngineFactory spi = clazz.newInstance();
                    scriptEngineFactoriesInBundle.add(spi);
                    
                } catch (Throwable t) {
                    LOG.error("Cannot register ScriptEngineFactory {}", className, t);
                }
            }

        } catch (IOException ioe) {
            LOG.warn("Exception while trying to load factories as defined in {}", ENGINE_FACTORY_SERVICE, ioe);
        } finally {
            closeQuietly(ins);
        }
        
        return scriptEngineFactoriesInBundle;
    }

    private void closeQuietly(InputStream ins) {
        if (ins != null) {
            try {
                ins.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void registerFactory(Bundle bundle, final ScriptEngineFactory factory) {
        LOG.info("Adding ScriptEngine {}, {} for language {}, {}",
                factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion());

        String scriptLang = factory.getLanguageName().toLowerCase();
        
        enginesByLanguage.put(scriptLang, factory);
        
        List<String> languages = languagesByBundle.get(bundle);
        if(languages==null) {
            languages = new ArrayList<String>();
            languagesByBundle.put(bundle, languages);
        }
        languages.add(scriptLang);
    }


    static List<String> getClassNames(BufferedReader reader) throws IOException {
        List<String> classNames = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#") && line.trim().length() > 0) {
                int indexOfHash = line.indexOf('#');
                if (indexOfHash >= 0) {
                    line = line.substring(0, indexOfHash);
                }
                line = line.trim();
                classNames.add(line);
            }
        }
        return classNames;
    }

}
