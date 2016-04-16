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
package org.apache.felix.dm.runtime.itest.components;

import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Utils {
    public static final String DM_BSN = "org.apache.felix.dependencymanager"; 

    public static void schedule(final Runnable task, final long n) {
        Thread t = new Thread() {
            public void run() {
                try {
                    sleep(n);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                task.run();
            }
        };
        t.start();
    }
        
    public static void assertEquals(Ensure e, ServiceReference ref, String property, Object expected, int step) {
        Object value = ref.getProperty(property);
        Assert.assertNotNull(value);
        Assert.assertEquals(value.getClass(), expected.getClass());
        Assert.assertEquals(value, expected);        
        e.step(step);
    }
    
    public static void assertArrayEquals(Ensure e, ServiceReference ref, String property, Object[] expected, int step) {
        Object values = ref.getProperty(property);
        Assert.assertNotNull(values);
        Assert.assertTrue(values.getClass().isArray());
        Assert.assertEquals(values.getClass(), expected.getClass());
        Object[] array = (Object[]) values;
        Assert.assertEquals(array.length, expected.length);
        for (int i = 0; i < array.length; i ++) {
            Assert.assertEquals(array[i].getClass(), expected[i].getClass());
            Assert.assertEquals(array[i], expected[i]);
        }
        e.step(step);
    }
}
