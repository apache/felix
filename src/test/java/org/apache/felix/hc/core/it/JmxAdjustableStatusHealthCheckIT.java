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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;
import javax.management.DynamicMBean;

import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.JmxAdjustableStatusHealthCheck;
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

/** Test jmx-adjustable status for testing HC. */
@RunWith(PaxExam.class)
public class JmxAdjustableStatusHealthCheckIT {

    @Inject
    private HealthCheckExecutor executor;

    @Inject
    private BundleContext bundleContext;

    private static String testTag = "testTagName";

    @SuppressWarnings("rawtypes")
    private List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();

    @Configuration
    public Option[] config() {
        return U.config();
    }

    private void assertResult(String tag, Result.Status expected) {
        final Result result = getOverallResult(executor.execute(HealthCheckSelector.tags(tag)));
        assertEquals("Expected status " + expected + " for tag " + tag, expected, result.getStatus());
    }

    @Before
    public void setup() {
        U.expectHealthChecks(0, executor, testTag);
        registerHC(testTag);
        U.expectHealthChecks(1, executor, testTag);
        assertResult(testTag, Result.Status.OK);
    }

    @After
    @SuppressWarnings("rawtypes")
    public void cleanup() throws Exception {
        invokeMBean("reset", new Object[] {}, new String[] {});

        U.expectHealthChecks(1, executor, testTag);
        assertResult(testTag, Result.Status.OK);

        for (ServiceRegistration r : regs) {
            r.unregister();
        }
        regs.clear();
        U.expectHealthChecks(0, executor, testTag);
    }

    @Test
    public void testWarnStatus() throws Exception {
        invokeMBean("addWarnResultForTags", new Object[] { testTag }, new String[] { String.class.getName() });
        U.expectHealthChecks(2, executor, testTag);
        assertResult(testTag, Result.Status.WARN);
    }

    @Test
    public void testCriticalStatus() throws Exception {
        invokeMBean("addCriticalResultForTags", new Object[] { "anotherTag," + testTag },
                new String[] { String.class.getName() });

        final String[] tags = { "anotherTag", testTag };
        for (String tag : tags) {
            U.expectHealthChecks(2, executor, testTag);
            assertResult(tag, Result.Status.CRITICAL);
        }
    }

    @Test
    public void testAnotherTag() throws Exception {
        // Selecting an unused tag returns WARN - not sure why but
        // if that changes we should detect it.
        assertResult("some_unused_tag", Result.Status.WARN);
    }

    private void registerHC(final String... tags) {
        final HealthCheck hc = new HealthCheck() {
            @Override
            public Result execute() {
                return new Result(Result.Status.OK, "All good for " + tags[0]);
            }
        };

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheck.NAME, "name_" + tags[0]);
        props.put(HealthCheck.TAGS, tags);

        regs.add(bundleContext.registerService(HealthCheck.class.getName(), hc, props));
    }

    private void invokeMBean(String operation, Object[] args, String[] signature) throws Exception {

        ServiceReference[] serviceReference = bundleContext.getServiceReferences(DynamicMBean.class.getName(),
                "(jmx.objectname=" + JmxAdjustableStatusHealthCheck.OBJECT_NAME + ")");
        DynamicMBean mBean = (DynamicMBean) bundleContext.getService(serviceReference[0]);
        mBean.invoke(operation, args, signature);

    }

    private Result getOverallResult(List<HealthCheckExecutionResult> results) {
        FormattingResultLog resultLog = new FormattingResultLog();
        for (HealthCheckExecutionResult executionResult : results) {
            for (Entry entry : executionResult.getHealthCheckResult()) {
                resultLog.add(new ResultLog.Entry(entry.getStatus(), entry.getMessage(), entry.getException()));
            }
        }
        return new Result(resultLog);
    }
}
