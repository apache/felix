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
package org.apache.felix.configadmin.plugin.interpolation;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationPlugin;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;

public class ActivatorTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testStart() throws Exception {
        final Dictionary<String, Object> regProps = new Hashtable<>();

        BundleContext ctx = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getProperty(Activator.DIR_PROPERTY)).thenReturn("/tmp/somewhere");
        Mockito.when(ctx.registerService(
            Mockito.eq(ConfigurationPlugin.class),
            Mockito.isA(ConfigurationPlugin.class),
            Mockito.isA(Dictionary.class))).then(
                new Answer<ServiceRegistration<?>>() {
                    @Override
                    public ServiceRegistration<?> answer(InvocationOnMock invocation) throws Throwable {
                        Dictionary<String, Object> props = (Dictionary<String, Object>) invocation.getArguments()[2];
                        for (Enumeration<String> e = props.keys(); e.hasMoreElements(); ) {
                            String key = e.nextElement();
                            regProps.put(key, props.get(key));
                        }
                        return null;
                    }
                });
        Activator a = new Activator();
        a.start(ctx);

        Dictionary<String, Object> expected = new Hashtable<>();
        expected.put(Activator.DIR_PROPERTY, "/tmp/somewhere");
        expected.put(ConfigurationPlugin.CM_RANKING, Activator.PLUGIN_RANKING);
        expected.put("config.plugin.id", "org.apache.felix.configadmin.plugin.interpolation");
        assertEquals(expected, regProps);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMissingConfiguration() throws Exception {
        BundleContext bc = Mockito.mock(BundleContext.class);

        Activator a = new Activator();
        a.start(bc);
        a.stop(bc);

        Mockito.verify(bc).getProperty(Activator.DIR_PROPERTY);

        // Should still register the service
        Mockito.verify(bc).registerService(Mockito.eq(ConfigurationPlugin.class),
            Mockito.isA(ConfigurationPlugin.class),
            Mockito.isA(Dictionary.class));
    }
}
