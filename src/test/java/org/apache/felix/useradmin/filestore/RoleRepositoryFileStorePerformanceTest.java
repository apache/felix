/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.filestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.useradmin.RoleFactory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Test case for {@link RoleRepositorySerializer}. 
 */
public class RoleRepositoryFileStorePerformanceTest extends TestCase {

    private static final int USER_COUNT = 25000;
    private static final int GROUP_COUNT = 500;

    private Role m_anyone;
    private Group[] m_groups;
    private User[] m_users;
    
    private Map m_repository;
    private RoleRepositoryFileStore m_store;

    /**
     * Executes the performance test.
     */
    public void testPerformanceOk() throws Exception {
        allocateMemory();

        writeRepositoryPerformanceTest();

        releaseMemory();
        
        readRepositoryPerformanceTest();
    }

    /**
     * Does a very simple performance test for a large number of users spread over several groups.
     */
    protected void readRepositoryPerformanceTest() throws Exception {
        long r_st = System.nanoTime();
        Map result = m_store.retrieve();
        long r_time = System.nanoTime() - r_st;

        assertNotNull(result);
        assertEquals(GROUP_COUNT + USER_COUNT + 1, result.size());

        System.out.println("Read time : " + (r_time / 1.0e9) + "s.");
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();

        m_store = new RoleRepositoryFileStore(new File(System.getProperty("java.io.tmpdir")), false /* disable background writes */);

        m_repository = new HashMap(USER_COUNT + GROUP_COUNT + 1);
        m_anyone = RoleFactory.createRole(Role.USER_ANYONE);

        addToRepository(m_anyone);
    }
    
    /**
     * Does a very simple performance test for writing a large number of users spread over several groups.
     */
    protected void writeRepositoryPerformanceTest() throws Exception {
        long w_st = System.nanoTime();
        m_store.store(m_repository);
        long w_time = System.nanoTime() - w_st;

        System.out.println("Write time: " + (w_time / 1.0e9) + "s.");
    }

    private void addToRepository(Role role) {
        m_repository.put(role.getName(), role);
    }

    /**
     * 
     */
    private void allocateMemory() {
        m_groups = new Group[GROUP_COUNT];
        for (int i = 0; i < m_groups.length; i++) {
            m_groups[i] = createGroup(i+1);
            m_groups[i].addRequiredMember(m_anyone);

            addToRepository(m_groups[i]);
        }

        m_users = new User[USER_COUNT];
        for (int i = 0; i < m_users.length; i++) {
            m_users[i] = createUser(i+1);
            
            int groupIdx = (i % m_groups.length);
            m_groups[groupIdx].addMember(m_users[i]);
            
            addToRepository(m_users[i]);
        }
    }

    private Group createGroup(int idx) {
        String name = "Group" + idx;
        
        Group result = RoleFactory.createGroup(name);

        setCredentials(result);
        setProperties(result);
        
        return result;
    }

    private User createUser(int idx) {
        String name = "User" + idx;
        
        User result = RoleFactory.createUser(name);

        setCredentials(result);
        setProperties(result);
        
        return result;
    }

    /**
     * 
     */
    private void releaseMemory() {
        m_groups = null;
        m_users = null;

        System.gc();
        System.gc();
        System.gc();
    }

    private void setCredentials(User user) {
        user.getCredentials().put("name", user.getName());
        user.getCredentials().put("password", "secret");
        user.getCredentials().put("certificate", new byte[] { (byte) 0x55, (byte) 0xAA } );
    }
    
    private void setProperties(Role role) {
        role.getProperties().put("key1", "value1");
        role.getProperties().put("key2", "hello world".getBytes());
    }
}
