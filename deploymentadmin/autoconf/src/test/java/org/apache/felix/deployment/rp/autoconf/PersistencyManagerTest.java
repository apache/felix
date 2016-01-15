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

package org.apache.felix.deployment.rp.autoconf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

import junit.framework.TestCase;

/**
 * Test cases for {@link PersistencyManager}.
 */
public class PersistencyManagerTest extends TestCase
{
    private File m_tempDir;

    public void testHandleNonExistingDirectory() throws Exception
    {
        PersistencyManager pm = new PersistencyManager(new File("/does/not/exist"));
        assertNotNull(pm);

        assertEquals(0, pm.getResourceNames().size());
    }

    public void testHandleEmptyExistingDirectory() throws Exception
    {
        PersistencyManager pm = new PersistencyManager(m_tempDir);
        assertNotNull(pm);

        assertEquals(0, pm.getResourceNames().size());
    }

    public void testLoadNonExistingResource() throws Exception
    {
        PersistencyManager pm = new PersistencyManager(m_tempDir);
        assertEquals(0, pm.load("doesNotExist").size());
    }

    public void testSaveResourceWithoutFilter() throws Exception
    {
        AutoConfResource res1 = new AutoConfResource("res1", "pid1", null, "osgi-dp:locationA", false, new Hashtable<String, Object>(), null);
        AutoConfResource res2 = new AutoConfResource("res2", "pid2", null, "osgi-dp:locationB", false, new Hashtable<String, Object>(), null);

        PersistencyManager pm = new PersistencyManager(m_tempDir);
        pm.store("test1", Arrays.asList(res1, res2));

        assertEquals(2, pm.load("test1").size());
    }

    public void testSaveResourceWithFilter() throws Exception
    {
        Filter f = FrameworkUtil.createFilter("(name=test)");

        AutoConfResource res1 = new AutoConfResource("res1", "pid1", null, "osgi-dp:locationA", false, new Hashtable<String, Object>(), f);
        AutoConfResource res2 = new AutoConfResource("res2", "pid2", null, "osgi-dp:locationB", false, new Hashtable<String, Object>(), null);

        PersistencyManager pm = new PersistencyManager(m_tempDir);
        pm.store("test1", Arrays.asList(res1, res2));

        assertEquals(2, pm.load("test1").size());
    }

    @Override
    protected void setUp() throws IOException
    {
        m_tempDir = File.createTempFile("persistence", "dir");
        m_tempDir.delete();
        m_tempDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception
    {
        Utils.removeDirectoryWithContent(m_tempDir);
    }

}
