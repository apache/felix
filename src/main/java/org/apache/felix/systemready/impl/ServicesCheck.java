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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.CheckStatus.State;
import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyCheck;
import org.apache.felix.systemready.rootcause.DSComp;
import org.apache.felix.systemready.rootcause.DSRootCause;
import org.apache.felix.systemready.rootcause.RootCausePrinter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        name = ServicesCheck.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd=ServicesCheck.Config.class)
public class ServicesCheck implements SystemReadyCheck {

    public static final String PID = "org.apache.felix.systemready.impl.ServicesCheck";

    @ObjectClassDefinition(
            name="Services Registered System Ready Check",
            description="System ready check that waits for a list of services to be registered"
    )
    public @interface Config {

        @AttributeDefinition(name = "Services list", description = "The services that need to be registered for the check to pass")
        String[] services_list();

        @AttributeDefinition(name = "Check type") 
        StateType type() default StateType.ALIVE;
    }

    private List<String> servicesList;

    private Map<String, Tracker> trackers;
    
    private DSRootCause analyzer;

    private StateType type;

    @Reference
    private ServiceComponentRuntime scr;


    @Activate
    public void activate(final BundleContext ctx, final Config config) throws InterruptedException {
        this.analyzer = new DSRootCause(scr);
        this.servicesList = Arrays.asList(config.services_list());
        this.trackers = this.servicesList.stream()
        	.collect(toMap(identity(), serviceName -> new Tracker(ctx, serviceName)));
        this.type = config.type();
    }

    @Deactivate
    protected void deactivate() {
        trackers.values().stream().forEach(Tracker::close);
        trackers.clear();
    }


    @Override
    public String getName() {
        return "Services Check";
    }

    @Override
    public CheckStatus getStatus() {
        boolean allPresent = trackers.values().stream().allMatch(Tracker::present);
        // TODO: RED on timeouts
        final CheckStatus.State state = State.fromBoolean(allPresent);
        return new CheckStatus(getName(), type, state, getDetails()); // TODO: out of sync? do we care?
    }

    private String getDetails() {
        List<String> missing = getMissing();
        StringBuilder missingSt = new StringBuilder();
        RootCausePrinter printer = new RootCausePrinter(st -> missingSt.append(st + "\n"));
        for (String iface : missing) {
            Optional<DSComp> rootCause = analyzer.getRootCause(iface);
            if (rootCause.isPresent()) {
                printer.print(rootCause.get());
            } else {
                missingSt.append("Missing service without matching DS component: " + iface);
            }
        }
        return missingSt.toString();
    }

    private List<String> getMissing() {
        List<String> missing = trackers.entrySet().stream()
                .filter(entry -> !entry.getValue().present())
                .map(entry -> entry.getKey())
                .collect(toList());
        return missing;
    }

}
