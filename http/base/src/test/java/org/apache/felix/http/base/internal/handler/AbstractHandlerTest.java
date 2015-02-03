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
package org.apache.felix.http.base.internal.handler;

import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public abstract class AbstractHandlerTest
{
    protected ExtServletContext context;

    protected abstract AbstractHandler createHandler();

    protected abstract AbstractHandler createHandler(Map<String, String> map);

    public void setUp()
    {
        this.context = Mockito.mock(ExtServletContext.class);
    }

    @Test
    public void testId()
    {
        AbstractHandler h1 = createHandler();
        AbstractHandler h2 = createHandler();

        Assert.assertTrue(h1.getServiceId() < 0);
        Assert.assertTrue(h2.getServiceId() < 0);
        Assert.assertFalse(h1.getServiceId() == h2.getServiceId());
    }

    @Test
    public void testInitParams()
    {
        AbstractHandler handler = createHandler();
        Assert.assertEquals(0, handler.getInitParams().size());

        Map<String, String> map = new Hashtable<String, String>();
        map.put("key1", "value1");

        handler = createHandler(map);
        Assert.assertEquals(1, handler.getInitParams().size());
        Assert.assertEquals("value1", handler.getInitParams().get("key1"));
    }
}
