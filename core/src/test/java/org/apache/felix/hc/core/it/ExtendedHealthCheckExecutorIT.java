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
package org.apache.felix.hc.core.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.util.HealthCheckFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/** Additional executor tests */
@RunWith(PaxExam.class)
public class ExtendedHealthCheckExecutorIT {

    @Inject
    private HealthCheckExecutor executor;

    @Inject
    private BundleContext bundleContext;

    @SuppressWarnings("rawtypes")
    private List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();

    private String testTag;
    private final Result.Status testResult = Result.Status.OK;

    @Configuration
    public Option[] config() {
        return U.config();
    }

    private void registerHC(final String... tags) {
        final HealthCheck hc = new HealthCheck() {
            @Override
            public Result execute() {
                return new Result(testResult, "Returning " + testResult + " for " + tags[0]);
            }

        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheck.NAME, "name_" + tags[0]);
        props.put(HealthCheck.TAGS, tags);

        regs.add(bundleContext.registerService(HealthCheck.class.getName(), hc, props));
    }

    @Before
    public void setup() {
        testTag = "TEST_" + UUID.randomUUID().toString();
        registerHC(testTag);
        U.expectHealthChecks(1, executor, testTag);
    }

    @After
    public void cleanup() {
        for (ServiceRegistration reg : regs) {
            reg.unregister();
        }
    }

    @Test
    public void testSingleExecution() throws Exception {
        final HealthCheckFilter filter = new HealthCheckFilter(bundleContext);
        final ServiceReference[] refs = filter.getHealthCheckServiceReferences(HealthCheckSelector.tags(testTag));
        assertNotNull(refs);
        assertEquals(1, refs.length);

        // The ExtendedHealthCheckExecutor interface is not public, so we cheat
        // to be able to test its implementation
        final Method m = executor.getClass().getMethod("execute", ServiceReference.class);
        final HealthCheckExecutionResult result = (HealthCheckExecutionResult) m.invoke(executor, refs[0]);
        assertEquals(testResult, result.getHealthCheckResult().getStatus());
    }
}