/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.hc.core.impl.util;

import static org.apache.felix.hc.api.execution.HealthCheckSelector.empty;
import static org.apache.felix.hc.api.execution.HealthCheckSelector.names;
import static org.apache.felix.hc.api.execution.HealthCheckSelector.tags;
import static org.junit.Assert.assertEquals;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.util.HealthCheckFilter;
import org.junit.Test;

public class HealthCheckFilterTest {

    private static final String HC_OBJ_CLASS_QUERY = "(|(objectClass=" + HealthCheck.class.getName()
            + ")(objectClass=org.apache.sling.hc.api.HealthCheck))";

    private HealthCheckFilter filter = new HealthCheckFilter(null);

    private static void assertStrEquals(String expected, CharSequence actual) {
        assertEquals(expected, actual.toString());
    }

    @Test
    public void testEmptyOptions() {
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + ")", filter.getServiceFilter(empty(), false));
    }

    @Test
    public void testWithOneTag() {
        HealthCheckSelector selector = tags("foo");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(hc.tags=foo))", filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithTwoTags() {
        HealthCheckSelector selector = tags("foo", "bar");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(hc.tags=foo)(hc.tags=bar))", filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithTwoTagsOr() {
        HealthCheckSelector selector = tags("foo", "bar");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(|(hc.tags=foo)(hc.tags=bar)))", filter.getServiceFilter(selector, true));
    }

    @Test
    public void testWithTwoTagsExcludeOne() {
        HealthCheckSelector selector = tags("foo", "bar").withTags("-baz");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(!(hc.tags=baz))(hc.tags=foo)(hc.tags=bar))",
                filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithOneName() {
        HealthCheckSelector selector = names("foo");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(|(hc.name=foo)))", filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithTwoNames() {
        HealthCheckSelector selector = names("foo").withNames("bar");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(|(hc.name=foo)(hc.name=bar)))", filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithTwoNamesExcludingOne() {
        HealthCheckSelector selector = names("foo", "bar", "-baz");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(!(hc.name=baz))(|(hc.name=foo)(hc.name=bar)))",
                filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithTagAndName() {
        HealthCheckSelector selector = empty().withTags("t1").withNames("foo");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(|(hc.name=foo)(hc.tags=t1)))", filter.getServiceFilter(selector, false));
    }

    @Test
    public void testWithTwoOrTagsAndTwoNames() {
        HealthCheckSelector selector = empty().withNames("foo", "bar").withTags("t1", "t2");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(|(hc.tags=t1)(hc.tags=t2)(hc.name=foo)(hc.name=bar)))",
                filter.getServiceFilter(selector, true));
    }

    @Test
    public void testWithTwoAndTagsAndTwoNames() {
        HealthCheckSelector selector = empty().withNames("foo", "bar").withTags("t1", "t2");
        assertStrEquals("(&" + HC_OBJ_CLASS_QUERY + "(|(hc.name=foo)(hc.name=bar)(&(hc.tags=t1)(hc.tags=t2))))",
                filter.getServiceFilter(selector, false));
    }

}
