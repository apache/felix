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

package org.apache.felix.useradmin.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(JUnit4TestRunner.class)
public class FileStoreInitializationTest extends BaseIntegrationTest
{

    /**
     * Tests that initialization and closing of the repository store is
     * performed correctly.
     */
    @Test
    public void testStoreIsInitializedAndClosedProperlyOk() throws Exception
    {
        UserAdmin ua = getUserAdmin();
        // Start the file store bundle...
        Bundle fileStoreBundle = getFileStoreBundle();
        fileStoreBundle.start();

        // Create two roles...
        User user = (User) ua.createRole("user1", Role.USER);
        assertNotNull(user);

        Group group = (Group) ua.createRole("group1", Role.GROUP);
        assertNotNull(group);

        group.addMember(user);
        group.addRequiredMember(ua.getRole(Role.USER_ANYONE));

        // Stop the file store; should persist the two roles...
        fileStoreBundle.stop();

        Thread.sleep(100); // Wait a little until the bundle is really stopped...

        // Retrieve the roles again; should both yield null due to the store not being available...
        user = (User) ua.getRole("user1");
        assertNull(user);

        group = (Group) ua.getRole("group1");
        assertNull(group);

        // This will not succeed: no backend to store the user in...
        assertNull(ua.createRole("user2", Role.USER));

        fileStoreBundle.start();

        awaitService(ORG_APACHE_FELIX_USERADMIN_FILESTORE);

        // Retrieve the roles again; should both yield valid values...
        user = (User) ua.getRole("user1");
        assertNotNull(user);

        group = (Group) ua.getRole("group1");
        assertNotNull(group);

        Role[] members = group.getMembers();
        assertNotNull(members);
        assertEquals(1, members.length);
        assertEquals("user1", members[0].getName());

        members = group.getRequiredMembers();
        assertNotNull(members);
        assertEquals(1, members.length);
        assertEquals(Role.USER_ANYONE, members[0].getName());

        user = (User) ua.getRole("user2");
        assertNull(user);
    }
}
