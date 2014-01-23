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
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

/**
 * Main integration test for the user admin service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@RunWith(JUnit4TestRunner.class)
public class MongoDBStoreTest extends BaseIntegrationTest
{
    /**
     * Tests that fetching an empty role without properties or other roles does not cause a NPE.
     */
    @Test
    public void testFelix4399_FetchEmptyRoleOk() throws Exception
    {
        UserAdmin ua = getUserAdmin();

        String roleName = "emptyRole";

        if (canRunTest())
        {
            Role emptyRole = ua.createRole(roleName, Role.USER);
            assertNotNull("Collection not empty?!", emptyRole);

            Role readRole = ua.getRole(roleName);

            assertNotNull("Unable to read back created empty role?!", readRole);
            assertEquals("Names not equal?!", emptyRole.getName(), readRole.getName());
            assertEquals("Types not equal?!", emptyRole.getType(), readRole.getType());

            Role[] readRoles = ua.getRoles(null);

            assertNotNull("Unable to read back created empty role?!", readRoles);
            assertEquals(1, readRoles.length);
        }
    }

    /**
     * Tests that creating a new role returns the actual created role.
     */
    @Test
    public void testFelix4400_CreateRoleReturnsNonNullOk() throws Exception
    {
        UserAdmin ua = getUserAdmin();

        String roleName = "newRole";

        if (canRunTest())
        {
            Role newRole = ua.createRole(roleName, Role.USER);
            assertNotNull("Felix-4400 not resolved?!", newRole);

            assertEquals("Names not equal?!", roleName, newRole.getName());
            assertEquals("Types not equal?!", Role.USER, newRole.getType());
        }
    }

    /**
     * Tests that removing a role works correctly.
     */
    @Test
    public void testRemoveRoleOk() throws Exception
    {
        UserAdmin ua = getUserAdmin();

        String roleName = "newRole";
        Role[] readRoles;

        if (canRunTest())
        {
            Role role = ua.createRole(roleName, Role.USER);
            assertNotNull("Collection not empty?!", role);

            readRoles = ua.getRoles(null);

            assertNotNull("No roles stored?!", readRoles);
            assertEquals(1, readRoles.length);

            ua.removeRole(roleName);

            readRoles = ua.getRoles(null);

            assertNull("Still roles stored?!", readRoles);
        }
    }

    /**
     * Tests that removing a role works correctly.
     */
    @Test
    public void testUpdateRoleOk() throws Exception
    {
        UserAdmin ua = getUserAdmin();

        String roleName = "role1";
        Role[] readRoles;

        if (canRunTest())
        {
            User role = (User) ua.createRole(roleName, Role.USER);
            assertNotNull("Collection not empty?!", role);

            readRoles = ua.getRoles(null);

            assertNotNull("No roles stored?!", readRoles);
            assertEquals(1, readRoles.length);

            role.getProperties().put("key", "value");

            Thread.sleep(100); // Wait a little to ensure everything is written...

            readRoles = ua.getRoles("(key=value)");

            assertNotNull("Role not updated?!", readRoles);
            assertEquals(1, readRoles.length);
        }
    }

    /**
     * Sets up MongoDB and tries to clear the useradmin collection. When this fails, it is assumed that no MongoDB service is available.
     */
    private boolean canRunTest() throws BundleException
    {
        Bundle mongoBundle = getMongoDBBundle();
        mongoBundle.start();

        Bundle mongoStoreBundle = getMongoDBStoreBundle();
        mongoStoreBundle.start();

        // Provision an empty configuration...
        BundleContext context = mongoStoreBundle.getBundleContext();

        ServiceReference serviceRef = context.getServiceReference(ManagedService.class.getName());
        assertNotNull(serviceRef);

        ManagedService service = (ManagedService) context.getService(serviceRef);
        try
        {
            service.updated(null);

            Mongo mongo = new Mongo();
            DB db = mongo.getDB("ua_repo");
            DBCollection collection = db.getCollection("useradmin");
            // we always get a collection back, regardless if there is an actual MongoDB listening, hence we should do
            // some actual calls that cause a connection to MongoDB to be created...
            collection.remove(new BasicDBObject(), WriteConcern.SAFE);

            CommandResult lastError = db.getLastError();

            return (lastError.getException() == null && collection.getCount() == 0L);
        }
        catch (Exception e)
        {
            // Ignore; apparently, we failed to connect to MongoDB...
        }

        return false;
    }
}
