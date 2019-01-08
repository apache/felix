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
package org.apache.felix.hc.core.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.executor.ExecutionResult;
import org.apache.felix.hc.core.impl.util.HealthCheckFilter;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

public class CompositeHealthCheckTest {

    @Spy
    private CompositeHealthCheck compositeHealthCheck = new CompositeHealthCheck();

    @Mock
    private HealthCheckExecutor healthCheckExecutor;

    @Mock
    private ComponentContext componentContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        compositeHealthCheck.setHealthCheckExecutor(healthCheckExecutor);
        compositeHealthCheck.setFilterTags(new String[] {});
        compositeHealthCheck.setComponentContext(componentContext);
    }

    @Test
    public void testExecution() {

        doReturn((Result) null).when(compositeHealthCheck).checkForRecursion(Matchers.<ServiceReference> any(),
                Matchers.<Set<String>> any());
        String[] testTags = new String[] { "tag1" };
        compositeHealthCheck.setFilterTags(testTags);

        List<HealthCheckExecutionResult> executionResults = new LinkedList<HealthCheckExecutionResult>();
        executionResults.add(createExecutionResult("Check 1", testTags, new Result(Result.Status.OK, "Good")));
        executionResults.add(createExecutionResult("Check 2", testTags, new Result(Result.Status.CRITICAL, "Bad")));

        when(healthCheckExecutor.execute(any(HealthCheckSelector.class), any(HealthCheckExecutionOptions.class)))
                .thenReturn(executionResults);

        Result result = compositeHealthCheck.execute();

        verify(healthCheckExecutor, times(1)).execute(argThat(selectorWithTags(testTags)), argThat(andOptions));

        assertEquals(Result.Status.CRITICAL, result.getStatus());

    }

    private Matcher<HealthCheckSelector> selectorWithTags(final String[] tags) {
        return new TypeSafeMatcher<HealthCheckSelector>() {
            @Override
            protected boolean matchesSafely(HealthCheckSelector healthCheckSelector) {
                return Arrays.equals(healthCheckSelector.tags(), tags) && healthCheckSelector.names() == null;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a select with tags (" + Arrays.toString(tags) + ") and no names.");
            }
        };
    }

    private HealthCheckExecutionResult createExecutionResult(String name, String[] testTags, Result result) {
        HealthCheckExecutionResult healthCheckExecutionResult = new ExecutionResult(
                new HealthCheckMetadata(new DummyHcServiceReference(name, testTags,
                        new String[0])),
                result, 0L);
        return healthCheckExecutionResult;
    }

    @Test
    public void testSimpleRecursion() {

        // composite check referencing itself
        final String[] filterTags = new String[] { "check1" };
        final DummyHcServiceReference hcRef = new DummyHcServiceReference("Check 1", new String[] { "check1" }, filterTags);

        // test check is hcRef
        doReturn(hcRef).when(componentContext).getServiceReference();
        compositeHealthCheck.setFilterTags(filterTags);

        compositeHealthCheck.setHealthCheckFilter(new HealthCheckFilter(null) {

            @Override
            public ServiceReference[] getHealthCheckServiceReferences(HealthCheckSelector selector) {
                String[] tags = selector.tags();
                ServiceReference[] result = new ServiceReference[] {};
                if (tags.length > 0) {
                    if (tags[0].equals(filterTags[0])) {
                        result = new ServiceReference[] { hcRef };
                    }
                }
                return result;
            }

        });

        Result result = compositeHealthCheck.execute();

        verify(healthCheckExecutor, never()).execute(any(HealthCheckSelector.class));
        assertEquals(Result.Status.HEALTH_CHECK_ERROR, result.getStatus());
    }

    @Test
    public void testCyclicRecursion() {

        // three checks, cyclic
        final String[] filterTags = new String[] { "check2" };
        final DummyHcServiceReference hcRef1 = new DummyHcServiceReference("Check 1", new String[] { "check1" }, filterTags);
        final DummyHcServiceReference hcRef2 = new DummyHcServiceReference("Check 2", new String[] { "check2" }, new String[] { "check3" });
        final DummyHcServiceReference hcRef3 = new DummyHcServiceReference("Check 3", new String[] { "check3" }, new String[] { "check1" });

        // test check is hcRef1
        doReturn(hcRef1).when(componentContext).getServiceReference();
        compositeHealthCheck.setFilterTags(filterTags);

        compositeHealthCheck.setHealthCheckFilter(new HealthCheckFilter(null) {

            @Override
            public ServiceReference[] getHealthCheckServiceReferences(HealthCheckSelector selector, boolean combineTagsWithOr) {
                String[] tags = selector.tags();
                ServiceReference[] result = new ServiceReference[] {};
                if (tags.length > 0) {
                    if (tags[0].equals(filterTags[0])) {
                        result = new ServiceReference[] { hcRef2 };
                    } else if (tags[0].equals("check3")) {
                        result = new ServiceReference[] { hcRef3 };
                    } else if (tags[0].equals("check1")) {
                        result = new ServiceReference[] { hcRef1 };
                    }
                }

                return result;
            }

        });

        Result result = compositeHealthCheck.execute();

        verify(healthCheckExecutor, never()).execute(any(HealthCheckSelector.class));
        assertEquals(Result.Status.HEALTH_CHECK_ERROR, result.getStatus());
    }

    @Test
    public void testCombineWithOr() {

        // composite check referencing itself
        final String[] filterTags = new String[] { "check1" };
        compositeHealthCheck.setFilterTags(filterTags);
        compositeHealthCheck.setCombineTagsWithOr(true);

        compositeHealthCheck.execute();

        verify(healthCheckExecutor, times(1)).execute(argThat(selectorWithTags(filterTags)), argThat(orOptions));
    }

    private Matcher<HealthCheckExecutionOptions> orOptions = new TypeSafeMatcher<HealthCheckExecutionOptions>() {
        @Override
        protected boolean matchesSafely(HealthCheckExecutionOptions options) {
            return options.isCombineTagsWithOr();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("options combining tags with or.");
        }
    };

    private Matcher<HealthCheckExecutionOptions> andOptions = new TypeSafeMatcher<HealthCheckExecutionOptions>() {
        @Override
        protected boolean matchesSafely(HealthCheckExecutionOptions options) {
            return !options.isCombineTagsWithOr();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("options combining tags with and.");
        }
    };

    private static class DummyHcServiceReference implements ServiceReference {

        private long id;
        private String name;
        private String[] tags;
        private String[] filterTags;

        public DummyHcServiceReference(String name, String[] tags, String[] filterTags) {
            super();
            this.id = (long) (Math.random() * Long.MAX_VALUE);
            this.name = name;
            this.tags = tags;
            this.filterTags = filterTags;
        }

        @Override
        public Object getProperty(String key) {

            if (Constants.SERVICE_ID.equals(key)) {
                return id;
            } else if (HealthCheck.NAME.equals(key)) {
                return name;
            } else if (HealthCheck.MBEAN_NAME.equals(key)) {
                return name;
            } else if (HealthCheck.TAGS.equals(key)) {
                return tags;
            } else if (CompositeHealthCheck.PROP_FILTER_TAGS.equals(key)) {
                return filterTags;
            } else if (ComponentConstants.COMPONENT_NAME.equals(key)) {
                return filterTags != null ? CompositeHealthCheck.class.getName() : "some.other.HealthCheck";
            } else {
                return null;
            }
        }

        @Override
        public String[] getPropertyKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle getBundle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle[] getUsingBundles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Object reference) {
            throw new UnsupportedOperationException();
        }

    }
}
