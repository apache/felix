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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.rootcause.DSComp;
import org.apache.felix.rootcause.DSRootCause;
import org.apache.felix.rootcause.RootCausePrinter;
import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyCheck;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        name = ComponentsCheck.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd=ComponentsCheck.Config.class)
public class ComponentsCheck implements SystemReadyCheck {

    public static final String PID = "org.apache.felix.systemready.impl.ComponentsCheck";

    @ObjectClassDefinition(
            name="DS Components System Ready Check",
            description="System ready check that checks a list of DS components"
                + "and provides root cause analysis in case of errors"
    )
    public @interface Config {

        @AttributeDefinition(name = "Components list", description = "The components that need to come up before this check reports GREEN")
        String[] components_list();
        
        @AttributeDefinition(name = "Check type") 
        StateType type() default StateType.ALIVE;

    }

    private List<String> componentsList;
    
    private DSRootCause analyzer;

    private StateType type;
    
    @Reference
    ServiceComponentRuntime scr;


    @Activate
    public void activate(final BundleContext ctx, final Config config) throws InterruptedException {
        this.analyzer = new DSRootCause(scr);
        this.type = config.type();
        componentsList = Arrays.asList(config.components_list());
    }

    @Override
    public String getName() {
        return "Components Check " + componentsList;
    }

    @Override
    public CheckStatus getStatus() {
        StringBuilder details = new StringBuilder();
        List<DSComp> watchedComps = scr.getComponentDescriptionDTOs().stream()
            .filter(desc -> componentsList.contains(desc.name))
            .map(analyzer::getRootCause)
            .collect(Collectors.toList());
        if (watchedComps.size() < componentsList.size()) {
            throw new IllegalStateException("Not all named components could be found");
        };
        watchedComps.stream().forEach(dsComp -> addDetails(dsComp, details));
        final CheckStatus.State state = CheckStatus.State.worstOf(watchedComps.stream().map(this::status));
        return new CheckStatus(getName(), type, state, details.toString());
    }
    
    private CheckStatus.State status(DSComp component) {
        boolean missingConfig = component.config == null && "require".equals(component.desc.configurationPolicy);
        boolean unsatisfied = !component.unsatisfied.isEmpty();
        return (missingConfig || unsatisfied) ? CheckStatus.State.YELLOW : CheckStatus.State.GREEN;
    }

    private void addDetails(DSComp component, StringBuilder details) {
        RootCausePrinter printer = new RootCausePrinter(st -> details.append(st + "\n"));
        printer.print(component);
    }

}
