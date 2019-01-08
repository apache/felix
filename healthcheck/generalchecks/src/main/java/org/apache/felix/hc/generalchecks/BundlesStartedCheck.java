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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HealthCheckService(name = BundlesStartedCheck.HC_NAME)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = BundlesStartedCheck.Config.class, factory = true)
public class BundlesStartedCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(BundlesStartedCheck.class);

    public static final String HC_NAME = "Bundles Started";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks the configured path(s) against the given thresholds")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "Includes RegEx", description = "RegEx to select all relevant bundles for this check. The RegEx is matched against the symbolic name of the bundle.")
        String includesRegex() default ".*";

        @AttributeDefinition(name = "Excludes RegEx", description = "Optional RegEx to exclude bundles from this check (matched against symbolic name). Allows to exclude specific bundles from selected set as produced by 'Includes RegEx'.")
        String excludesRegex() default "";

        @AttributeDefinition(name = "CRITICAL for inactive bundles", description = "By default inactive bundles produce warnings, if this is set to true inactive bundles produce a CRITICAL result")
        boolean useCriticalForInactive() default false;
        
        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Bundles started includes: {includesRegex} excludes: {excludesRegex}";
    }

    private BundleContext bundleContext;
    private Pattern includesRegex;
    private Pattern excludesRegex;
    boolean useCriticalForInactive;

    @Activate
    protected void activate(BundleContext bundleContext, Config config) {
        this.bundleContext = bundleContext;
        this.includesRegex = Pattern.compile(config.includesRegex());
        this.excludesRegex = StringUtils.isNotBlank(config.excludesRegex()) ? Pattern.compile(config.excludesRegex()) : null;
        this.useCriticalForInactive = config.useCriticalForInactive();
        LOG.info("Activated bundles started HC for includesRegex={} excludesRegex={}% useCriticalForInactive={}", includesRegex, excludesRegex, useCriticalForInactive);
    }

    
    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        Bundle[] bundles = this.bundleContext.getBundles();
        log.debug("Framwork has {} bundles in total", bundles.length);
 
        int countExcluded = 0;
        int relevantBundlesCount = 0;
        int inctiveCount = 0;
        for (Bundle bundle : bundles) {
            String bundleSymbolicName = bundle.getSymbolicName();
            int bundleState = bundle.getState();
            
            if(!includesRegex.matcher(bundleSymbolicName).matches()) {
                LOG.debug("Bundle {} not matched by {}", bundleSymbolicName, includesRegex);
                continue;
            }

            if(excludesRegex!=null && excludesRegex.matcher(bundleSymbolicName).matches()) {
                LOG.debug("Bundle {} excluded {}", bundleSymbolicName, excludesRegex);
                countExcluded ++;
                continue;
            }
            relevantBundlesCount++;
            
            boolean bundleIsLogged = false;
            if (bundleState != Bundle.ACTIVE) {
                // support lazy activation (https://www.osgi.org/developer/design/lazy-start/)
                if (bundleState == Bundle.STARTING && isLazyActivation(bundle)) {
                    LOG.debug("Ignoring lazily activated bundle {}", bundleSymbolicName);
                } else  if (StringUtils.isNotBlank((String) bundle.getHeaders().get(Constants.FRAGMENT_HOST))) {
                    LOG.debug("Ignoring bundle fragment: {}", bundleSymbolicName);
                } else {
                    String msg = "Inactive bundle {} {}: {}";
                    Object[] msgObjs = new Object[] {bundle.getBundleId(), bundleSymbolicName, getStateLabel(bundleState)};
                    LOG.debug(msg, msgObjs);
                    if(useCriticalForInactive) {
                        log.critical(msg, msgObjs);
                    } else {
                        log.warn(msg, msgObjs);
                    }
                    bundleIsLogged = true;
                    inctiveCount++;
                }
            }
            if(!bundleIsLogged) {
                log.debug("Bundle {} {}: {}", bundle.getBundleId(), bundleSymbolicName, getStateLabel(bundleState));
            }
        }
        
        String baseMsg = relevantBundlesCount+" bundles"+(!includesRegex.pattern().equals(".*")?" for pattern "+includesRegex.pattern(): "");
        String excludedMsg = countExcluded > 0 ? " (" + countExcluded + " excluded via pattern "+excludesRegex.pattern()+")" : "";
        if (inctiveCount > 0) {
            log.info("Found  "+inctiveCount + " inactive of "+baseMsg + excludedMsg);
        } else {
            log.info("All "+baseMsg+" are started" + excludedMsg);
        }

        return new Result(log);
    }

    private static boolean isLazyActivation(Bundle b) {
        return Constants.ACTIVATION_LAZY.equals(b.getHeaders().get(Constants.BUNDLE_ACTIVATIONPOLICY));
    }
    
    private static String getStateLabel(int state) {
        switch(state) {
        case Bundle.UNINSTALLED: return "UNINSTALLED";
        case Bundle.INSTALLED: return "INSTALLED";
        case Bundle.RESOLVED: return "RESOLVED";
        case Bundle.STARTING: return "STARTING";
        case Bundle.STOPPING: return "STOPPING";
        case Bundle.ACTIVE: return "ACTIVE";
        default: return ""+state;
        }
    } 

}
