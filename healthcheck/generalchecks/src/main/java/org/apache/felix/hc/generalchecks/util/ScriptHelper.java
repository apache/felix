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
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/** Script Helper to interact with . */
public class ScriptHelper {

    public String getFileContents(String url) {
        String content;
        try {
            URLConnection conn = new URL(url).openConnection();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }
            return content;
        }catch(IOException e) {
            throw new IllegalArgumentException("Could not read URL "+url+": "+e, e);
        }
    }
    
    public ScriptEngine getScriptEngine(ScriptEnginesTracker scriptEnginesTracker, String language) {
        ScriptEngine scriptEngine = scriptEnginesTracker.getEngineByLanguage(language);
        if(scriptEngine == null) {
            throw new IllegalArgumentException("No ScriptEngineFactory found for language "+ language + " (available languages: "+scriptEnginesTracker.getLanguagesByBundle()+")");
        }
        return scriptEngine;
    }
    
    public Object evalScript(BundleContext bundleContext, ScriptEngine scriptEngine, String scriptToExecute, FormattingResultLog log, Map<String,Object> additionalBindings, boolean logScriptResult) throws ScriptException, IOException {

        final Bindings bindings = new SimpleBindings();
        final ScriptHelperBinding scriptHelper = new ScriptHelperBinding(bundleContext);

        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();

        bindings.put("scriptHelper", scriptHelper);
        bindings.put("osgi", scriptHelper); // also register script helper like in web console script console
        bindings.put("log", log);
        bindings.put("bundleContext", bundleContext);
        if (additionalBindings != null) {
            for (Map.Entry<String, Object> additionalBinding : additionalBindings.entrySet()) {
                bindings.put(additionalBinding.getKey(), additionalBinding.getValue());
            }
        }
        
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        scriptContext.setWriter(stdout);
        scriptContext.setErrorWriter(stderr);

        try {
            log.debug(scriptToExecute);
            Object scriptResult = scriptEngine.eval(scriptToExecute, scriptContext);
            appendStreamsToResult(log, stdout, stderr, scriptContext);

            if(scriptResult instanceof Result) {
                Result result = (Result) scriptResult;
                for(ResultLog.Entry entry: result) {
                    log.add(entry);
                }
            } else if(scriptResult != null && logScriptResult){
                log.info("Script result: {}", scriptResult);
            }
            
            return scriptResult;
        } finally  {
            scriptHelper.ungetServices();
        }
    }

    
    
    private void appendStreamsToResult(FormattingResultLog log, StringWriter stdout, StringWriter stderr, SimpleScriptContext scriptContext)
            throws IOException {
        scriptContext.getWriter().flush();
        String stdoutStr = stdout.toString();
        if(StringUtils.isNotBlank(stdoutStr)) {
            log.info("stdout of script: {}", stdoutStr);
        }
        
        scriptContext.getErrorWriter().flush();
        String stderrStr = stderr.toString();
        if(StringUtils.isNotBlank(stderrStr)) {
            log.critical("stderr of script: {}", stderrStr);
        }
    }

    // Script Helper for OSGi available as binding 'scriptHelper'
    class ScriptHelperBinding {
        
        private final BundleContext bundleContext;
        private List<ServiceReference<?>> references;
        private Map<String, Object> services;

        public ScriptHelperBinding(BundleContext bundleContext) {
            this.bundleContext = bundleContext;
        }

        @SuppressWarnings("unchecked")
        public <ServiceType> ServiceType getService(Class<ServiceType> type) {
            ServiceType service = (this.services == null ? null  : (ServiceType) this.services.get(type.getName()));
            if (service == null) {
                final ServiceReference<?> ref = this.bundleContext.getServiceReference(type.getName());
                if (ref != null) {
                    service = (ServiceType) this.bundleContext.getService(ref);
                    if (service != null) {
                        if (this.services == null) {
                            this.services = new HashMap<String, Object>();
                        }
                        if (this.references == null) {
                            this.references = new ArrayList<ServiceReference<?>>();
                        }
                        this.references.add(ref);
                        this.services.put(type.getName(), service);
                    }
                }
            }
            return service;
        }

        public <T> T[] getServices(Class<T> serviceType,  String filter) throws InvalidSyntaxException {
            final ServiceReference<?>[] refs = this.bundleContext.getServiceReferences(serviceType.getName(), filter);
            T[] result = null;
            if (refs != null) {
                final List<T> objects = new ArrayList<T>();
                for (int i = 0; i < refs.length; i++) {
                    @SuppressWarnings("unchecked")
                    final T service = (T) this.bundleContext.getService(refs[i]);
                    if (service != null) {
                        if (this.references == null) {
                            this.references = new ArrayList<ServiceReference<?>>();
                        }
                        this.references.add(refs[i]);
                        objects.add(service);
                    }
                }
                if (objects.size() > 0) {
                    @SuppressWarnings("unchecked")
                    T[] srv = (T[]) Array.newInstance(serviceType,  objects.size());
                    result = objects.toArray(srv);
                }
            }
            return result;
        }

        public void ungetServices() {
            if (this.references != null) {
                final Iterator<ServiceReference<?>> i = this.references.iterator();
                while (i.hasNext()) {
                    final ServiceReference<?> ref = i.next();
                    this.bundleContext.ungetService(ref);
                }
                this.references.clear();
            }
            if (this.services != null) {
                this.services.clear();
            }
        }
    }
    
}
