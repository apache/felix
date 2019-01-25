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

import javax.script.ScriptEngine;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.generalchecks.util.ScriptEnginesTracker;
import org.apache.felix.hc.generalchecks.util.ScriptHelper;
import org.osgi.framework.BundleContext;
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
            + "The script has the following default bindings available: 'log', 'scriptHelper' and 'bundleContext'. "
            + "'log' is an instance of org.apache.felix.hc.api.FormattingResultLog and is used to define the result of the HC. "
            + "'scriptHelper.getService(classObj)' can be used as shortcut to retrieve a service."
            + "'scriptHelper.getServices(classObj, filter)' used to retrieve multiple services for a class using given filter. "
            + "For all services retrieved via scriptHelper, unget() is called automatically at the end of the script execution."
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

    private ScriptHelper scriptHelper = new ScriptHelper();

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
        
        
        boolean urlIsUsed = StringUtils.isBlank(script);
        String scriptToExecute = urlIsUsed ? scriptHelper.getFileContents(scriptUrl): script;
        log.info("Executing script {} ({} lines)...", (urlIsUsed?scriptUrl:" as configured"), scriptToExecute.split("\n").length);

        try {
            ScriptEngine scriptEngine = scriptHelper.getScriptEngine(scriptEnginesTracker, language);
            scriptHelper.evalScript(bundleContext, scriptEngine, scriptToExecute, log, null, true);
        }  catch (Exception e) {
            log.healthCheckError("Exception while executing script: "+e, e);
        }

        return new Result(log);
    }


}
