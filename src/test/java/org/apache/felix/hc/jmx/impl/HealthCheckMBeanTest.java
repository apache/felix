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
package org.apache.felix.hc.jmx.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.core.impl.executor.ExtendedHealthCheckExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.ServiceReference;

public class HealthCheckMBeanTest {
    private final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
    private boolean resultOk;
    public static final String OBJECT_NAME = "org.apache.sling.testing:type=HealthCheckMBeanTest";

    @Mock
    private HealthCheck testHealthCheck;

    @Mock
    private ServiceReference<HealthCheck> ref;

    @Mock
    private ExtendedHealthCheckExecutor extendedHealthCheckExecutor;

    @Mock
    private HealthCheckExecutionResult result;

    
    @Before
    public void setup() {
        initMocks(this);
        
        when(testHealthCheck.execute()).then(new Answer<Result>() {

            @Override
            public Result answer(InvocationOnMock invocation) throws Throwable {
                if (resultOk) {
                    return new Result(Result.Status.OK, "Nothing to report, result ok");
                } else {
                    return new Result(Result.Status.WARN, "Result is not ok!");
                }
            }
        });
        
        when(extendedHealthCheckExecutor.execute(ref)).thenReturn(result);
        when(result.getHealthCheckResult()).then(new Answer<Result>() {

            @Override
            public Result answer(InvocationOnMock invocation) throws Throwable {
                return testHealthCheck.execute();
            }
        });
    }

    @Test
    public void testBean() throws Exception {
        
        final HealthCheckMBean mbean = new HealthCheckMBean(ref, extendedHealthCheckExecutor);
        final ObjectName name = new ObjectName(OBJECT_NAME);
        jmxServer.registerMBean(mbean, name);
        try {
            resultOk = true;
            assertEquals(true, getJmxValue(OBJECT_NAME, "ok"));

            Thread.sleep(1500);
            resultOk = false;
            assertEquals(false, getJmxValue(OBJECT_NAME, "ok"));

            Thread.sleep(1500);
            assertThat(String.valueOf(getJmxValue(OBJECT_NAME, "log")), containsString("message=Result is not ok!"));

        } finally {
            jmxServer.unregisterMBean(name);
        }
    }
    

    private Object getJmxValue(String mbeanName, String attributeName) throws Exception {
        final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = new ObjectName(mbeanName);
        if (jmxServer.queryNames(objectName, null).size() == 0) {
            fail("MBean not found: " + objectName);
        }
        final Object value = jmxServer.getAttribute(objectName, attributeName);
        return value;
    }
    

}