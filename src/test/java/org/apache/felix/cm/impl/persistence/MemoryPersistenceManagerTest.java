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
package org.apache.felix.cm.impl.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.PersistenceManager;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;


public class MemoryPersistenceManagerTest
{
    private static final String PID_A = "foo.a";
    private static final String PID_B = "foo.b";
    private static final String PID_C = "foo.c";
    private static final String FACTORY_PID_A = "bla.a";
    private static final String FACTORY_PID_B = "bla.b";
    private static final String FA_PID_A = "728206-a";
    private static final String FA_PID_B = "728206-b";
    private static final String FA_PID_C = "728206-c";
    private static final String FB_PID_A = "992101-a";
    private static final String FB_PID_B = "992101-b";

    private static final String PREFIX = "this-is-";

    private Dictionary<String, Object> createConfiguration(final String pid, final String factoryPid)
    {
        final Dictionary<String, Object> dict = new Hashtable<>();

        dict.put(Constants.SERVICE_PID, pid);
        if ( factoryPid != null )
        {
            dict.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }
        dict.put("value", PREFIX + pid);

        return dict;
    }

    private PersistenceManager createAndPopulatePersistenceManager()
    throws IOException
    {
        final PersistenceManager pm = new MemoryPersistenceManager();

        pm.store(PID_A, createConfiguration(PID_A, null));
        pm.store(PID_B, createConfiguration(PID_B, null));
        pm.store(PID_C, createConfiguration(PID_C, null));

        pm.store(FA_PID_A, createConfiguration(FA_PID_A, FACTORY_PID_A));
        pm.store(FA_PID_B, createConfiguration(FA_PID_B, FACTORY_PID_A));
        pm.store(FA_PID_C, createConfiguration(FA_PID_C, FACTORY_PID_A));

        pm.store(FB_PID_A, createConfiguration(FB_PID_A, FACTORY_PID_B));
        pm.store(FB_PID_B, createConfiguration(FB_PID_B, FACTORY_PID_B));
        return pm;
    }

    @Test public void testPopulation() throws Exception
    {
        final PersistenceManager cpm = this.createAndPopulatePersistenceManager();

        assertTrue(cpm.exists(PID_A));
        assertTrue(cpm.exists(PID_B));
        assertTrue(cpm.exists(PID_C));
        assertTrue(cpm.exists(FA_PID_A));
        assertTrue(cpm.exists(FA_PID_B));
        assertTrue(cpm.exists(FA_PID_C));
        assertTrue(cpm.exists(FB_PID_A));
        assertTrue(cpm.exists(FB_PID_B));

        assertFalse(cpm.exists("foo"));
    }

    @Test
    public void testRemove() throws Exception
    {
        final PersistenceManager cpm = this.createAndPopulatePersistenceManager();

        cpm.delete(FA_PID_A);
        cpm.delete(PID_A);

        assertFalse(cpm.exists(PID_A));
        assertTrue(cpm.exists(PID_B));
        assertTrue(cpm.exists(PID_C));
        assertFalse(cpm.exists(FA_PID_A));
        assertTrue(cpm.exists(FA_PID_B));
        assertTrue(cpm.exists(FA_PID_C));
        assertTrue(cpm.exists(FB_PID_A));
        assertTrue(cpm.exists(FB_PID_B));
    }
}