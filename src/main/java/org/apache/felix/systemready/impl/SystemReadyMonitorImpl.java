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
package org.apache.felix.systemready.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.Status;
import org.apache.felix.systemready.SystemReady;
import org.apache.felix.systemready.SystemReadyCheck;
import org.apache.felix.systemready.SystemReadyMonitor;
import org.apache.felix.systemready.SystemStatus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = SystemReadyMonitor.PID
)
@Designate(ocd = SystemReadyMonitorImpl.Config.class)
public class SystemReadyMonitorImpl implements SystemReadyMonitor {

    @ObjectClassDefinition(
            name = "System Ready Monitor",
            description = "System ready monitor for System Ready Checks"
    )
    public @interface Config {

        @AttributeDefinition(name = "Poll interval",
                description = "Number of milliseconds between subsequents updates of all the checks")
        long poll_interval() default 5000;

    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.DYNAMIC)
    private volatile List<SystemReadyCheck> checks;

    private BundleContext context;

    private ServiceRegistration<SystemReady> sreg;

    private ScheduledExecutorService executor;
    
    private AtomicReference<SystemStatus> systemState;

    public SystemReadyMonitorImpl() {
        SystemStatus initialStatus = new SystemStatus(Status.State.YELLOW, Collections.emptyList());
        this.systemState = new AtomicReference<>(initialStatus);
    }

    @Activate
    public void activate(BundleContext context, final Config config) {
        this.context = context;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleAtFixedRate(this::check, 0, config.poll_interval(), TimeUnit.MILLISECONDS);
        log.info("Activated. Running checks every {} ms.", config.poll_interval());
    }

    @Deactivate
    public void deactivate() {
        executor.shutdown();
    }

    @Override
    /**
     * Returns whether the system is ready or not
     */
    public boolean isReady() {
        return systemState.get().getState() == Status.State.GREEN;
    }

    @Override
    /**
     * Returns a map of the statuses of all the checks
     */
    public SystemStatus getStatus() {
        return systemState.get();
    }

    private void check() {
        Status.State prevState = systemState.get().getState();
        List<SystemReadyCheck> currentChecks = new ArrayList<>(checks);
        List<String> checkNames = currentChecks.stream().map(check -> check.getName()).collect(Collectors.toList());
        log.debug("Running system checks {}", checkNames);
        List<CheckStatus> statuses = evaluateAllChecks(currentChecks);
        Status.State currState = Status.State.worstOf(statuses.stream().map(status -> status.getStatus().getState()));
        this.systemState.set(new SystemStatus(currState, statuses));
        if (currState != prevState) {
            manageMarkerService(currState);
        }
        log.debug("Checks finished");
    }

    private List<CheckStatus> evaluateAllChecks(List<SystemReadyCheck> currentChecks) {
        return currentChecks.stream()
                .map(SystemReadyMonitorImpl::getStatus)
                .sorted(Comparator.comparing(CheckStatus::getCheckName))
                .collect(Collectors.toList());
    }
    
    private void manageMarkerService(Status.State currState) {
        if (currState == Status.State.GREEN) {
            SystemReady readyService = new SystemReady() {
            };
            sreg = context.registerService(SystemReady.class, readyService, null);
        } else {
            sreg.unregister();
        }
    }

    private static final CheckStatus getStatus(SystemReadyCheck c) {
        try {
            return new CheckStatus(c.getName(), c.getStatus());
        } catch (Throwable e) {
            return new CheckStatus(c.getClass().getName(), new Status(Status.State.RED, e.getMessage()));
        }
    }

}
