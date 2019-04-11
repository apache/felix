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
package org.apache.felix.hc.generalchecks;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.Closeable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.generalchecks.scrutil.DsRootCauseAnalyzer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@HealthCheckService(name = ServicesCheck.HC_NAME, tags = { ServicesCheck.HC_DEFAULT_TAG })
@Designate(ocd = ServicesCheck.Config.class, factory = true)
public class ServicesCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesCheck.class);

    public static final String HC_NAME = "Services Ready Check";
    public static final String HC_DEFAULT_TAG = "systemalive";

    @ObjectClassDefinition(name = "Health Check: " + HC_NAME, description = "System ready check that checks a list of DS components "
            + "and provides root cause analysis in case of errors")

    public @interface Config {

        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default { HC_DEFAULT_TAG };

        @AttributeDefinition(name = "Services list", description = "The services that need to be registered for the check to pass. This can be either the service name (objectClass) or an arbitrary filter expression if the expression starts with '(' (for that case at least one service for the filter needs to be available)")
        String[] services_list();

        @AttributeDefinition(name = "Status for missing services", description = "Status in case services are missing")
        Result.Status statusForMissing() default Result.Status.TEMPORARILY_UNAVAILABLE;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "{hc.name}: {services.list} / missing -> {statusForMissing}";
    }

    private List<String> servicesList;
    private Result.Status statusForMissing;

    private Map<String, Tracker> trackers;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private DsRootCauseAnalyzer analyzer;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ServiceComponentRuntime scr;

    @Activate
    public void activate(final BundleContext ctx, final Config config) throws InterruptedException {
        this.servicesList = Arrays.asList(config.services_list());
        this.trackers = this.servicesList.stream().collect(toMap(identity(), serviceName -> new Tracker(ctx, serviceName)));
        statusForMissing = config.statusForMissing();
        LOG.debug("Activated Services HC for servicesList={}", servicesList);

    }

    @Deactivate
    protected void deactivate() {
        trackers.values().stream().forEach(Tracker::close);
        trackers.clear();
    }

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();
        List<String> missingServiceNames = getMissingServiceNames(log);


        for (String missingServiceName : missingServiceNames) {
            if (!missingServiceName.startsWith("(")) {
                analyzer.logMissingService(log, missingServiceName, statusForMissing);
            } else {
                log.info("Service '{}' is missing", missingServiceName);
            }
        }

        if (missingServiceNames.isEmpty()) {
            log.info("All {} required services are available", servicesList.size());
        } else {
            log.add(new Entry(statusForMissing, "Not all required services are available ("+missingServiceNames.size()+" are missing)"));
        }
        
        return new Result(log);
    }

    private List<String> getMissingServiceNames(FormattingResultLog log) {
        List<String> missingServicesNames = new LinkedList<>();

        for(Map.Entry<String, Tracker> entry: trackers.entrySet()) {
            if(!entry.getValue().present()) {
                missingServicesNames.add(entry.getKey());
            } else {
                log.debug("Found {} services for '{}'", entry.getValue().getTrackingCount(), entry.getKey());
            }
        }
        return missingServicesNames;
    }

    public class Tracker implements Closeable {
        private ServiceTracker<?, ?> stracker;

        public Tracker(BundleContext context, String nameOrFilter) {
            String filterSt = nameOrFilter.startsWith("(") ? nameOrFilter : String.format("(objectClass=%s)", nameOrFilter);
            Filter filter;
            try {
                filter = FrameworkUtil.createFilter(filterSt);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Error creating filter for " + nameOrFilter);
            }
            this.stracker = new ServiceTracker<>(context, filter, null);
            this.stracker.open();
        }

        public boolean present() {
            return getTrackingCount() > 0;
        }
        
        public int getTrackingCount() {
            return this.stracker.getTrackingCount();
        }
        
        @Override
        public void close() {
            stracker.close();
        }

    }

}
