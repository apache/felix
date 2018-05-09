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

import org.apache.felix.systemready.Status;
import org.apache.felix.systemready.SystemReadyCheck;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = "FrameworkStartCheck",
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(ocd=FrameworkStartCheck.Config.class)
public class FrameworkStartCheck implements SystemReadyCheck {

    public static final String FRAMEWORK_STARTED = "Framework started";
    public static final String FRAMEWORK_START_CHECK_NAME = "Framework Start Check";

    @ObjectClassDefinition(
            name="OSGi Installer System Ready Check",
            description="System ready that waits for the framework started OSGi event"
    )
    public @interface Config {

        @AttributeDefinition(name = "Timeout (seconds)", description = "Number of seconds after which this is considered a failure")
        long timeout() default 1000;

    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    private BundleContext bundleContext;

    private int count = 0;
    private Status state;

    @Activate
    protected void activate(final BundleContext ctx, final Config config) throws InterruptedException {
        this.bundleContext = ctx;
        this.bundleContext.addFrameworkListener(this::frameworkEvent);

        if (bundleContext.getBundle(0).getState() == Bundle.ACTIVE) {
            // The system bundle was already started when I joined
            this.state = new Status(Status.State.GREEN, FRAMEWORK_STARTED);
        } else {
            this.state = new Status(Status.State.YELLOW, "No OSGi Framework events received so far");
        }
        log.info("Activated");
    }

    @Deactivate
    protected void deactivate() throws InterruptedException {
        this.bundleContext = null;
    }

    @Override
    public String getName() {
        return FRAMEWORK_START_CHECK_NAME;
    }

    @Override
    public Status getStatus() {
        return this.state;
    }

    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
            this.count ++;
            this.state = new Status(Status.State.YELLOW, "Received " + count + " startlevel changes so far");
        } else if (event.getType() == FrameworkEvent.STARTED) {
            this.state = new Status(Status.State.GREEN, FRAMEWORK_STARTED);
        } // TODO: RED on timeout?
    }
}
