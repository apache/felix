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
package org.apache.felix.hc.generalchecks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.generalchecks.JmxAttributeCheck;
import org.junit.Test;

public class JmxAttributeHealthCheckTest {

    static void assertJmxValue(String objectName, String attributeName, String constraint, boolean expected) {
        final JmxAttributeCheck hc = new JmxAttributeCheck();

        final JmxAttributeCheck.Config configuration = mock(JmxAttributeCheck.Config.class);
        when(configuration.mbean_name()).thenReturn(objectName);
        when(configuration.attribute_name()).thenReturn(attributeName);
        when(configuration.attribute_value_constraint()).thenReturn(constraint);

        hc.activate(configuration);

        final Result r = hc.execute();
        assertEquals("Expected result " + expected, expected, r.isOk());
    }

    @Test
    public void testJmxAttributeMatch() {
        assertJmxValue("java.lang:type=ClassLoading", "LoadedClassCount", "> 10", true);
    }

    @Test
    public void testJmxAttributeNoMatch() {
        assertJmxValue("java.lang:type=ClassLoading", "LoadedClassCount", "< 10", false);
    }
}
