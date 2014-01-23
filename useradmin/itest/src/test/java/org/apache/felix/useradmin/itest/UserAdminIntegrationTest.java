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

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Main integration test for the user admin service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(JUnit4TestRunner.class)
public class UserAdminIntegrationTest extends BaseIntegrationTest
{
    /**
     * Tests that stopping a filled store and starting it again will cause it to
     * properly restore its state.
     */
    @Test
    public void testFelix3735_StopRunningStoreRetainsDataOk() throws Exception
    {
        final String userName = "testUser";
        final String groupName = "testGroup";

        UserAdmin userAdmin = awaitService(UserAdmin.class.getName());
        Bundle fileStoreBundle = getFileStoreBundle();
        // Start a suitable storage service...
        fileStoreBundle.start();

        // Fill the user admin with some data...
        User testUser = (User) userAdmin.createRole(userName, Role.USER);
        testUser.getProperties().put("key", "value");

        Group testGroup = (Group) userAdmin.createRole(groupName, Role.GROUP);
        testGroup.addMember(testUser);

        // Stop the file store...
        fileStoreBundle.stop();

        // retrieve the useradmin again...
        userAdmin = awaitService(UserAdmin.class.getName());

        // Verify the user + group are gone (no store available)...
        assertNull(userAdmin.getRole(userName));
        assertNull(userAdmin.getRole(groupName));

        // Start the file store...
        fileStoreBundle.start();

        // Verify the user + group are gone (no store available)...
        User readUser = (User) userAdmin.getRole(userName);
        assertNotNull(readUser);
        assertEquals(userName, readUser.getName());
        assertEquals("value", readUser.getProperties().get("key"));

        Group readGroup = (Group) userAdmin.getRole(groupName);
        assertNotNull(readGroup);
        assertEquals(groupName, readGroup.getName());
        assertEquals(1, readGroup.getMembers().length);
        assertEquals(readUser, readGroup.getMembers()[0]);
    }

    /**
     * Tests that starting the file store <em>after</em> the user admin service
     * is started will cause it to be properly initialized.
     */
    @Test
    public void testFelix3735_StartStoreAfterUserAdminInitializesOk() throws Exception
    {
        final String userName = "anotherTestUser";
        final String groupName = "anotherTestGroup";

        UserAdmin userAdmin = awaitService(UserAdmin.class.getName());
        Bundle fileStoreBundle = getFileStoreBundle();
        // Start a suitable storage service...
        fileStoreBundle.start();

        // Fill the user admin with some data...
        User testUser = (User) userAdmin.createRole(userName, Role.USER);
        testUser.getProperties().put("key", "value");

        Group testGroup = (Group) userAdmin.createRole(groupName, Role.GROUP);
        testGroup.addMember(testUser);

        // Stop the file store...
        fileStoreBundle.stop();

        Bundle userAdminBundle = findBundle(ORG_APACHE_FELIX_USERADMIN);
        assertNotNull(userAdminBundle);
        userAdminBundle.stop();

        // Obtain user admin service again; shouldn't be available...
        userAdmin = getService(UserAdmin.class.getName());
        assertNull(userAdmin);

        userAdminBundle.start();

        // Obtain user admin service again; should be available now...
        userAdmin = awaitService(UserAdmin.class.getName());
        assertNotNull(userAdmin);

        // Verify the user + group are gone (no store available)...
        assertNull(userAdmin.getRole(userName));
        assertNull(userAdmin.getRole(groupName));

        // Start the file store...
        fileStoreBundle.start();

        // Verify the user + group are gone (no store available)...
        User readUser = (User) userAdmin.getRole(userName);
        assertNotNull(readUser);
        assertEquals(userName, readUser.getName());
        assertEquals("value", readUser.getProperties().get("key"));

        Group readGroup = (Group) userAdmin.getRole(groupName);
        assertNotNull(readGroup);
        assertEquals(groupName, readGroup.getName());
        assertEquals(1, readGroup.getMembers().length);
        assertEquals(readUser, readGroup.getMembers()[0]);
    }
}
