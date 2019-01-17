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
package org.apache.felix.hc.generalchecks;

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
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.generalchecks.util.ScriptEnginesTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that runs an arbitrary script. */
@Component(service = HealthCheck.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ScriptedHealthCheck.Config.class, factory = true)
public class ScriptedHealthCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptedHealthCheck.class);

    public static final String HC_LABEL = "Health Check: Script";

    @ObjectClassDefinition(name = HC_LABEL, description = "Runs an arbitrary script in given scriping language (via javax.script). "
            + "The script has the following default bindings available: 'log', 'osgi' and 'bundleContext'. "
            + "'log' is an instance of org.apache.felix.hc.api.FormattingResultLog and is used to define the result of the HC. "
            + "'osgi.getService(classObj)' can be used as shortcut to retrieve a service."
            + "'osgi.getServices(classObj, filter)' used to retrieve multiple services for a class using given filter. "
            + "For all services retrieved via osgi binding, unget() is called automatically at the end of the script execution."
            + "'bundleContext' is available for advanced use cases. The script does not need to return any value, but if it does and it is "
            + "a org.apache.felix.hc.api.Result, that result and entries in 'log' are combined then).")
    @interface Config {

        @AttributeDefinition(name = "Name", description = "Name of this health check.")
        String hc_name() default "Scripted Health Check";

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "Language", description = "The language the script is written in. To use e.g. 'groovy', ensure osgi bundle 'groovy-all' is available.")
        String language() default "groovy";

        @AttributeDefinition(name = "Script", description = "The script itself (either use 'script' or 'scriptUrl').")
        String script() default "log.info('ok'); log.warn('not so good'); log.critical('bad') // minimal example";
        
        @AttributeDefinition(name = "Script Url", description = "Url to the script to be used as alternative source (either use 'script' or 'scriptUrl').")
        String scriptUrl() default "";

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Scripted HC: {hc.name} (tags: {hc.tags}) {scriptUrl} language: {language}";
    }

    private String language;
    private String script;
    private String scriptUrl;
    
    private BundleContext bundleContext;
    
    @Reference
    private ScriptEnginesTracker scriptEnginesTracker;

    @Activate
    protected void activate(BundleContext context, Config config) {
        this.bundleContext = context;
        this.language = config.language().toLowerCase();
        this.script = config.script();
        this.scriptUrl = config.scriptUrl();
        
        if(StringUtils.isNotBlank(script) && StringUtils.isNotBlank(scriptUrl)) {
            LOG.info("Both 'script' and 'scriptUrl' (=()) are configured, ignoring 'scriptUrl'", scriptUrl);
            scriptUrl = null;
        }

        LOG.info("Activated Scripted HC "+config.hc_name()+" with "+ (StringUtils.isNotBlank(script)?"script "+script: "script url "+scriptUrl));

    }

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();
        
        ScriptEngine scriptEngine = scriptEnginesTracker.getEngineByLanguage(language);
        if(scriptEngine == null) {
            log.healthCheckError("No ScriptEngineFactory found for language "+ language + " (available languages: "+scriptEnginesTracker.getLanguagesByBundle()+")");
            return new Result(log);
        }
        
        final Bindings bindings = new SimpleBindings();
        final ScriptHelper osgi = new ScriptHelper(bundleContext);

        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();

        bindings.put("osgi", osgi);
        bindings.put("log", log);
        bindings.put("bundleContext", bundleContext);

        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        scriptContext.setWriter(stdout);
        scriptContext.setErrorWriter(stderr);

        try {
            boolean urlIsUsed = StringUtils.isBlank(script);
            String scriptToExecute = urlIsUsed ? getFileContents(scriptUrl): script;
            log.info("Executing script {} ({} lines)...", (urlIsUsed?scriptUrl:" as configured"), scriptToExecute.split("\n").length);
            log.debug(scriptToExecute);
            Object scriptResult = scriptEngine.eval(scriptToExecute, scriptContext);
            appendStreamsToResult(log, stdout, stderr, scriptContext);
            
            if(scriptResult instanceof Result) {
                Result result = (Result) scriptResult;
                for(ResultLog.Entry entry: result) {
                    log.add(entry);
                }
            } else if(scriptResult != null){
                log.info("Script result: {}", scriptResult);
            }
            
            
        }  catch (Exception e) {
            log.healthCheckError("Exception during script execution: "+e, e);
        } finally  {
            osgi.ungetServices();
        }

        return new Result(log);
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

    // Script Helper for OSGi available as binding 'osgi'
    class ScriptHelper {
        
        private final BundleContext bundleContext;
        private List<ServiceReference<?>> references;
        private Map<String, Object> services;

        public ScriptHelper(BundleContext bundleContext) {
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
