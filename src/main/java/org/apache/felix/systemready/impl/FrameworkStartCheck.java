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

import org.apache.felix.systemready.CheckStatus;
import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyCheck;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = FrameworkStartCheck.PID,
        immediate=true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(ocd=FrameworkStartCheck.Config.class)
public class FrameworkStartCheck implements SystemReadyCheck {

    public static final String PID = "org.apache.felix.systemready.impl.FrameworkStartCheck";
    public static final String FRAMEWORK_STARTED = "Framework started. ";
    public static final String FRAMEWORK_NOT_STARTED = "Framework NOT started. ";
    public static final String FRAMEWORK_START_CHECK_NAME = "Framework Start Ready Check";

    @ObjectClassDefinition(
            name=FRAMEWORK_START_CHECK_NAME,
            description="System ready that waits for the system bundle to be active"
    )
    public @interface Config {

        @AttributeDefinition(name = "Timeout (seconds)", description = "Number of seconds after which this is considered a failure")
        long timeout() default 1000;

        @AttributeDefinition(name = "Target start level", description = "The target start level at which the Framework " +
                "is considered started. If zero or negative, it will default to the default bundle start level")
        int target_start_level() default 0;

        @AttributeDefinition(name = "Target start level OSGi property name",
                description = "The name of the OSGi property which holds the " + "\"Target start level\". " +
                        "It takes precedence over the target.start.level config. " +
                        "If the startlevel cannot be derived from the osgi property, this config attribute is ignored.")
        String target_start_level_prop_name() default "";
        
        @AttributeDefinition(name = "Check type") 
        StateType type() default StateType.ALIVE;

    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    private BundleContext bundleContext;
    private long targetStartLevel;
	private StateType type;

    @Activate
    protected void activate(final BundleContext ctx, final Config config) throws InterruptedException {
        this.bundleContext = ctx;
        this.targetStartLevel = getTargetStartLevel(config);
        this.type = config.type();
        log.info("Activated");
    }

	private long getTargetStartLevel(final Config config) {
		final FrameworkStartLevel fsl = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_ID).adapt(FrameworkStartLevel.class);
        final long initial = fsl.getInitialBundleStartLevel();
        // get the configured target start level, otherwise use the initial bundle start level
        long tStartLevel = config.target_start_level() > 0 ? config.target_start_level() : initial;

        // overwrite with the value from #target_start_level_prop_name if present
        final String targetStartLevelKey = config.target_start_level_prop_name();
        if (null != targetStartLevelKey && !targetStartLevelKey.trim().isEmpty()) {
            try {
                tStartLevel = Long.valueOf(bundleContext.getProperty(targetStartLevelKey));
            } catch (NumberFormatException e) {
                log.info("Ignoring {} as it can't be parsed: {}", targetStartLevelKey, e.getMessage());
            }
        }
        return tStartLevel;
	}

    @Override
    public String getName() {
        return FRAMEWORK_START_CHECK_NAME;
    }

    @Override
    public CheckStatus getStatus() {
        Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_ID);
        FrameworkStartLevel fsl = systemBundle.adapt(FrameworkStartLevel.class);
        String message = String.format("Start level: %d; Target start level: %d; Framework state: %d",
                fsl.getStartLevel(), targetStartLevel, fsl.getBundle().getState());
        boolean started = (systemBundle.getState() == Bundle.ACTIVE) && (fsl.getStartLevel() >= targetStartLevel);
        if (started) {
            return new CheckStatus(getName(), type, CheckStatus.State.GREEN, FRAMEWORK_STARTED + message);
        } else {
            return new CheckStatus(getName(), type, CheckStatus.State.YELLOW, FRAMEWORK_NOT_STARTED + message);
        }
    }

}
