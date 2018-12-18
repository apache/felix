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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Test the HealthCheckExecutor selection mechanism */
@RunWith(PaxExam.class)
public class HealthCheckExecutorSelectionIT {

    @Inject
    private HealthCheckExecutor executor;

    @Inject
    private BundleContext bundleContext;

    private static String idA;
    private static String idB;
    private HealthCheckExecutionOptions options;

    @SuppressWarnings("rawtypes")
    private List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();

    @Configuration
    public Option[] config() {
        return U.config();
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

    @BeforeClass
    public static void setId() {
        idA = UUID.randomUUID().toString();
        idB = UUID.randomUUID().toString();
    }

    @Before
    public void setup() {
        options = new HealthCheckExecutionOptions();

        U.expectHealthChecks(0, executor, idA);
        U.expectHealthChecks(0, executor, idB);

        registerHC(idA);
        registerHC(idB);
        registerHC(idB);
        registerHC(idA, idB);
    }

    @After
    @SuppressWarnings("rawtypes")
    public void cleanup() {
        for (ServiceRegistration r : regs) {
            r.unregister();
        }
        regs.clear();

        U.expectHealthChecks(0, executor, idA);
        U.expectHealthChecks(0, executor, idB);
    }

    @Test
    public void testDefaultSelectionA() {
        U.expectHealthChecks(2, executor, idA);
        U.expectHealthChecks(2, executor, options, idA);
    }

    @Test
    public void testDefaultSelectionB() {
        U.expectHealthChecks(3, executor, idB);
        U.expectHealthChecks(3, executor, options, idB);
    }

    @Test
    public void testDefaultSelectionAB() {
        U.expectHealthChecks(1, executor, idA, idB);
        U.expectHealthChecks(1, executor, options, idA, idB);
    }

    @Test
    public void testOrSelectionA() {
        options.setCombineTagsWithOr(true);
        U.expectHealthChecks(1, executor, options, idA);
    }

    @Test
    public void testOrSelectionB() {
        options.setCombineTagsWithOr(true);
        U.expectHealthChecks(3, executor, options, idB);
    }

    @Test
    public void testOrSelectionAB() {
        options.setCombineTagsWithOr(true);
        U.expectHealthChecks(4, executor, options, idA, idB);
    }
}
