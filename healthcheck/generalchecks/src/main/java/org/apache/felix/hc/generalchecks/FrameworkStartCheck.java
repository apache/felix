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

import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
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

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@HealthCheckService(name = FrameworkStartCheck.HC_NAME, tags = { FrameworkStartCheck.HC_DEFAULT_TAG })
@Designate(ocd = FrameworkStartCheck.Config.class)
public class FrameworkStartCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(FrameworkStartCheck.class);

    public static final String HC_NAME = "OSGi Framework Ready Check";
    public static final String HC_DEFAULT_TAG = "systemalive";

    public static final String FRAMEWORK_STARTED = "Framework started. ";
    public static final String FRAMEWORK_NOT_STARTED = "Framework NOT started. ";

    @ObjectClassDefinition(name = "Health Check: " + HC_NAME, description = "System ready that waits for the system bundle to be active")
    public @interface Config {

        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default { HC_DEFAULT_TAG };

        @AttributeDefinition(name = "Target start level", description = "The target start level at which the Framework " +
                "is considered started. If zero or negative, it will default to the default bundle start level")
        int targetStartLevel() default 0;

        @AttributeDefinition(name = "Target start level OSGi property name", description = "The name of the OSGi property which holds the "
                + "\"Target start level\". " +
                "It takes precedence over the 'targetStartLevel' config. " +
                "If the startlevel cannot be derived from the osgi property, this config attribute is ignored.")
        String targetStartLevel_propName() default "";

    }

    private BundleContext bundleContext;
    private long targetStartLevel;

    @Activate
    protected void activate(final BundleContext ctx, final Config config) throws InterruptedException {
        this.bundleContext = ctx;
        this.targetStartLevel = getTargetStartLevel(config);
        LOG.info("Activated");
    }

    private long getTargetStartLevel(final Config config) {
        final FrameworkStartLevel fsl = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_ID).adapt(FrameworkStartLevel.class);
        final long initial = fsl.getInitialBundleStartLevel();
        // get the configured target start level, otherwise use the initial bundle start level
        long tStartLevel = config.targetStartLevel() > 0 ? config.targetStartLevel() : initial;

        // overwrite with the value from #targetStartLevel_propName if present
        final String targetStartLevelKey = config.targetStartLevel_propName();
        if (null != targetStartLevelKey && !targetStartLevelKey.trim().isEmpty()) {
            try {
                tStartLevel = Long.valueOf(bundleContext.getProperty(targetStartLevelKey));
            } catch (NumberFormatException e) {
                LOG.info("Ignoring {} as it can't be parsed: {}", targetStartLevelKey, e.getMessage());
            }
        }
        return tStartLevel;
    }

    @Override
    public Result execute() {
        Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_ID);
        FrameworkStartLevel fsl = systemBundle.adapt(FrameworkStartLevel.class);
        String message = String.format("Start level: %d; Target start level: %d; Framework state: %d",
                fsl.getStartLevel(), targetStartLevel, fsl.getBundle().getState());
        boolean started = (systemBundle.getState() == Bundle.ACTIVE) && (fsl.getStartLevel() >= targetStartLevel);
        if (started) {
            return new Result(Result.Status.OK, FRAMEWORK_STARTED + message);
        } else {
            return new Result(Result.Status.TEMPORARILY_UNAVAILABLE, FRAMEWORK_NOT_STARTED + message);
        }
    }

}
