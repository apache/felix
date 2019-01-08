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
package org.apache.felix.hc.core.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.util.HealthCheckFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that executes a number of other HealthChecks, selected by their tags, and merges their Results. */

@Component(service = HealthCheck.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = CompositeHealthCheckConfiguration.class, factory = true)
public class CompositeHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String PROP_FILTER_TAGS = "filter.tags";

    private String[] filterTags;

    private boolean combineTagsWithOr;

    @Reference
    private HealthCheckExecutor healthCheckExecutor;

    private BundleContext bundleContext;
    private HealthCheckFilter healthCheckFilter;

    private volatile ComponentContext componentContext;

    @Activate
    protected void activate(final CompositeHealthCheckConfiguration configuration, final ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        componentContext = ctx;
        healthCheckFilter = new HealthCheckFilter(bundleContext);

        filterTags = configuration.filter_tags();
        combineTagsWithOr = configuration.filter_combineTagsWithOr();
        log.debug("Activated, will select HealthCheck having tags {} {}", Arrays.asList(filterTags),
                combineTagsWithOr ? "using OR" : "using AND");
    }

    @Deactivate
    protected void deactivate() {
        bundleContext = null;
        healthCheckFilter = null;
        componentContext = null;
    }

    @Override
    public Result execute() {
        final ComponentContext localCtx = this.componentContext;
        final ServiceReference referenceToThis = localCtx == null ? null : localCtx.getServiceReference();
        Result result = referenceToThis == null ? null : checkForRecursion(referenceToThis, new HashSet<String>());
        if (result != null) {
            // return recursion error
            return result;
        }

        FormattingResultLog resultLog = new FormattingResultLog();
        HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
        options.setCombineTagsWithOr(combineTagsWithOr);
        List<HealthCheckExecutionResult> executionResults = healthCheckExecutor.execute(HealthCheckSelector.tags(filterTags), options);
        resultLog.debug("Executing {} HealthChecks selected by tags {}", executionResults.size(), Arrays.asList(filterTags));
        result = new CompositeResult(resultLog, executionResults);

        return result;
    }

    Result checkForRecursion(ServiceReference hcReference, Set<String> alreadyBannedTags) {

        HealthCheckMetadata thisCheckMetadata = new HealthCheckMetadata(hcReference);

        Set<String> bannedTagsForThisCompositeCheck = new HashSet<String>();
        bannedTagsForThisCompositeCheck.addAll(alreadyBannedTags);
        bannedTagsForThisCompositeCheck.addAll(thisCheckMetadata.getTags());

        String[] tagsForIncludedChecksArr = toStringArray(hcReference.getProperty(PROP_FILTER_TAGS), new String[0]);
        Set<String> tagsForIncludedChecks = new HashSet<String>(Arrays.asList(tagsForIncludedChecksArr));

        log.debug("HC {} has banned tags {}", thisCheckMetadata.getName(), bannedTagsForThisCompositeCheck);
        log.debug("tagsForIncludedChecks {}", tagsForIncludedChecks);

        // is this HC ok?
        Set<String> intersection = new HashSet<String>();
        intersection.addAll(bannedTagsForThisCompositeCheck);
        intersection.retainAll(tagsForIncludedChecks);

        if (!intersection.isEmpty()) {
            return new Result(Status.HEALTH_CHECK_ERROR,
                    "INVALID CONFIGURATION: Cycle detected in composite health check hierarchy. Health check '"
                            + thisCheckMetadata.getName()
                            + "' (" + hcReference.getProperty(Constants.SERVICE_ID) + ") must not have tag(s) " + intersection
                            + " as a composite check in the hierarchy is itself already tagged alike (tags assigned to composite checks: "
                            + bannedTagsForThisCompositeCheck + ")");
        }

        // check each sub composite check
        ServiceReference[] hcRefsOfCompositeCheck = healthCheckFilter
                .getHealthCheckServiceReferences(HealthCheckSelector.tags(tagsForIncludedChecksArr), combineTagsWithOr);
        for (ServiceReference hcRefOfCompositeCheck : hcRefsOfCompositeCheck) {
            if (CompositeHealthCheck.class.getName().equals(hcRefOfCompositeCheck.getProperty(ComponentConstants.COMPONENT_NAME))) {
                log.debug("Checking sub composite HC {}, {}", hcRefOfCompositeCheck,
                        hcRefOfCompositeCheck.getProperty(ComponentConstants.COMPONENT_NAME));
                Result result = checkForRecursion(hcRefOfCompositeCheck, bannedTagsForThisCompositeCheck);
                if (result != null) {
                    // found recursion
                    return result;
                }
            }

        }

        // no recursion detected
        return null;

    }

    public static String[] toStringArray(Object propValue, String[] defaultArray) {
        if (propValue instanceof String[]) {
            return (String[]) propValue;
        } else if (propValue instanceof String) {
            return new String[] { (String) propValue };
        } else if (propValue == null) {
            return defaultArray;
        }
        return defaultArray;
    }

    void setHealthCheckFilter(HealthCheckFilter healthCheckFilter) {
        this.healthCheckFilter = healthCheckFilter;
    }

    void setFilterTags(String[] filterTags) {
        this.filterTags = filterTags;
    }

    void setCombineTagsWithOr(boolean combineTagsWithOr) {
        this.combineTagsWithOr = combineTagsWithOr;
    }

    void setHealthCheckExecutor(HealthCheckExecutor healthCheckExecutor) {
        this.healthCheckExecutor = healthCheckExecutor;
    }

    void setComponentContext(ComponentContext ctx) {
        this.componentContext = ctx;
    }
}
