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
package org.apache.felix.useradmin.mongodb;

import static org.apache.felix.useradmin.mongodb.MongoSerializerHelper.NAME;
import static org.apache.felix.useradmin.mongodb.MongoSerializerHelper.TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

/**
 * Provides a repository store that uses MongoDB for storing the role information.
 * <p>
 * This service can also be configured at runtime by using the PID {@value #PID}.<br/>
 * The configuration options recognized by this service are:
 * </p>
 * <dl>
 * <dt>"useradmin.mongodb.server"</dt>
 * <dd>A space separated string containing the MongoDB servers. The format for this string is: "<code>&lt;host1:port1&gt; &lt;host2:port2&gt;</code>". This value is mandatory;</dd>
 * <dt>"useradmin.mongodb.name"</dt>
 * <dd>A string containing the name of the database to use for this store. This value is mandatory;</dd>
 * <dt>"useradmin.mongodb.collection"</dt>
 * <dd>The name of the database collection to use for this store. This value is mandatory;</dd>
 * <dt>"useradmin.mongodb.username"</dt>
 * <dd>An optional string value representing the name of the user to authenticate against MongoDB;</dd>
 * <dt>"useradmin.mongodb.password"</dt>
 * <dd>An optional string value representing the password to authenticate against MongoDB.</dd>
 * </dl>
 * <p>
 * Alternatively, one can also supply the above mentioned configuration keys as system properties. However,
 * this implies that only a single store can be configured on a system!
 * </p>
 * <p>
 * By default, the following values are used:
 * </p>
 * <table>
 * <tr><td>"<tt>useradmin.mongodb.server</tt>"</td><td>"<tt>localhost:27017</tt>"</td></tr>
 * <tr><td>"<tt>useradmin.mongodb.name</tt>"</td><td>"<tt>ua_repo</tt>"</td></tr>
 * <tr><td>"<tt>useradmin.mongodb.collection</tt>"</td><td>"<tt>useradmin</tt>"</td></tr>
 * <tr><td>"<tt>useradmin.mongodb.username</tt>"</td><td>&lt;none&gt;</td></tr>
 * <tr><td>"<tt>useradmin.mongodb.password</tt>"</td><td>&lt;none&gt;</td></tr>
 * </table>
 * <p>
 * This class is thread-safe.
 * </p>
 */
public class MongoDBStore implements RoleProvider, RoleRepositoryStore, UserAdminListener, ManagedService {
    
    /** The PID for the managed service reference. */
    public static final String PID = "org.apache.felix.useradmin.mongodb"; 
                    
    /** 
     * A space-separated array with server definitions to access MongoDB. 
     * Format = "&lt;host1:port1&gt; &lt;host2:port2&gt;". 
     * */
    private static final String KEY_MONGODB_SERVER = "useradmin.mongodb.server";
    /** The name of the MongoDB database instance. */
    private static final String KEY_MONGODB_NAME = "useradmin.mongodb.name";
    /** The username of the MongoDB database instance. */
    private static final String KEY_MONGODB_USERNAME = "useradmin.mongodb.username";
    /** The password of the MongoDB database instance. */
    private static final String KEY_MONGODB_PASSWORD = "useradmin.mongodb.password";
    /** The name of the MongoDB collection to use. */
    private static final String KEY_MONGODB_COLLECTION_NAME = "useradmin.mongodb.collection";

    /** Default MongoDB server; first checks a system property */
    private static final String DEFAULT_MONGODB_SERVER = System.getProperty(KEY_MONGODB_SERVER, "localhost:27017");
    /** Default MongoDB name */
    private static final String DEFAULT_MONGODB_NAME = System.getProperty(KEY_MONGODB_NAME, "ua_repo");
    /** Default MongoDB collection */
    private static final String DEFAULT_MONGODB_COLLECTION = System.getProperty(KEY_MONGODB_COLLECTION_NAME, "useradmin");
    /** Default MongoDB username */
    private static final String DEFAULT_MONGODB_USERNAME = System.getProperty(KEY_MONGODB_USERNAME);
    /** Default MongoDB password */
    private static final String DEFAULT_MONGODB_PASSWORD = System.getProperty(KEY_MONGODB_PASSWORD);

    private final AtomicReference<MongoDB> m_mongoDbRef;
    private final MongoSerializerHelper m_helper;
    
    private volatile LogService m_log;

    /**
     * Creates a new {@link MongoDBStore} instance.
     */
    public MongoDBStore() {
        m_mongoDbRef = new AtomicReference<MongoDB>();
        m_helper = new MongoSerializerHelper(this);
    }

    @Override
    public boolean addRole(Role role) throws IOException {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        
        try {
            DBCollection coll = getCollection();
            
            DBCursor cursor = coll.find(getTemplateObject(role));
            try {
                if (cursor.hasNext()) {
                    // Role already exists...
                    return false;
                }
            } finally {
                cursor.close();
            }
            
            // Role does not exist; insert it...
            DBObject data = m_helper.serialize(role);
            
            WriteResult result = coll.insert(data);
            
            if (result.getLastError() != null) {
                result.getLastError().throwOnError();
            }

            return true;
        }
        catch (MongoException e) {
            m_log.log(LogService.LOG_WARNING, "Add role failed!", e);
            throw new IOException("AddRole failed!", e);
        }
    }

    @Override
    public void close() throws IOException {
        MongoDB mongoDB = m_mongoDbRef.get();
        if (mongoDB != null) {
            mongoDB.disconnect();
        }
    }

    @Override
    public Role[] getAllRoles() throws IOException {
        try {
            List<Role> roles = new ArrayList<Role>();
            
            DBCollection coll = getCollection();

            DBCursor cursor = coll.find();
            try {
                while (cursor.hasNext()) {
                    roles.add(m_helper.deserialize(cursor.next()));
                }
            } finally {
                cursor.close();
            }

            return roles.toArray(new Role[roles.size()]);
        }
        catch (MongoException e) {
            m_log.log(LogService.LOG_WARNING, "Get all roles failed!", e);
            throw new IOException("GetAllRoles failed!", e);
        }
    }

    @Override
    public Role getRole(String name) {
        DBCollection coll = getCollection();

        DBCursor cursor = coll.find(getTemplateObject(name));
        try {
            if (cursor.hasNext()) {
                return m_helper.deserialize(cursor.next());
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    @Override
    public Role getRoleByName(String name) throws IOException {
        try {
            return getRole(name);
        }
        catch (MongoException e) {
            m_log.log(LogService.LOG_WARNING, "Get role by name failed!", e);
            throw new IOException("GetRoleByName failed!", e);
        }
    }
    
    @Override
    public void initialize() throws IOException {
        // Check whether we need to connect to MongoDB, or that this is
        // already done by the #updated method...
        MongoDB oldMongoDB = m_mongoDbRef.get();
        if (oldMongoDB == null) {
            MongoDB mongoDB = new MongoDB(DEFAULT_MONGODB_SERVER, DEFAULT_MONGODB_NAME, DEFAULT_MONGODB_COLLECTION);
            
            do {
                oldMongoDB = m_mongoDbRef.get();
            } 
            while (!m_mongoDbRef.compareAndSet(oldMongoDB, mongoDB));
            
            try {
                connectToDB(mongoDB, DEFAULT_MONGODB_USERNAME, DEFAULT_MONGODB_PASSWORD);
            }
            catch (MongoException e) {
                m_log.log(LogService.LOG_WARNING, "Initialization failed!", e);
                throw new IOException("Initialization failed!", e);
            }
        }
    }

    @Override
    public boolean removeRole(Role role) throws IOException {
        try {
            DBCollection coll = getCollection();

            WriteResult result = coll.remove(getTemplateObject(role));

            if (result.getLastError() != null) {
                result.getLastError().throwOnError();
            }

            return true;
        }
        catch (MongoException e) {
            m_log.log(LogService.LOG_WARNING, "Remove role failed!", e);
            throw new IOException("RemoveRole failed!", e);
        }
    }
    
    @Override
    public void roleChanged(UserAdminEvent event) {
        if (UserAdminEvent.ROLE_CHANGED == event.getType()) {
            // Only the changes are interesting, as the creation and 
            // removal are already caught by #addRole and #removeRole.... 
            Role changedRole = event.getRole();

            try {
                DBCollection coll = getCollection();

                DBObject query = getTemplateObject(changedRole);
                DBObject update = m_helper.serializeUpdate(changedRole);

                WriteResult result = coll.update(query, update, false /* upsert */, false /* multi */);

                if (result.getLastError() != null) {
                    result.getLastError().throwOnError();
                }
            }
            catch (MongoException e) {
                m_log.log(LogService.LOG_WARNING, "Failed to update changed role: " + changedRole.getName(), e);
            }
        }
    }
    
    /**
     * @param log the log-service to set, cannot be <code>null</code>.
     */
    public void setLogService(LogService log) {
        m_log = log;
    }
    
    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        String newServers = DEFAULT_MONGODB_SERVER;
        String newDbName = DEFAULT_MONGODB_NAME;
        String newCollectionName = DEFAULT_MONGODB_COLLECTION;
        String newUsername = DEFAULT_MONGODB_USERNAME;
        String newPassword = DEFAULT_MONGODB_PASSWORD;
        
        if (properties != null) {
            // Use values supplied...
            newServers = getMandatoryProperty(properties, KEY_MONGODB_SERVER);
            newDbName = getMandatoryProperty(properties, KEY_MONGODB_NAME);
            newCollectionName = getMandatoryProperty(properties, KEY_MONGODB_COLLECTION_NAME);
            
            newUsername = getProperty(properties, KEY_MONGODB_USERNAME);
            newPassword = getProperty(properties, DEFAULT_MONGODB_PASSWORD);
        }

        MongoDB newMongoDb = new MongoDB(newServers, newDbName, newCollectionName);

        MongoDB oldMongoDb;
        do {
            oldMongoDb = m_mongoDbRef.get();
        }
        while (!m_mongoDbRef.compareAndSet(oldMongoDb, newMongoDb));

        try {
            oldMongoDb.disconnect();
        }
        catch (MongoException e) {
            m_log.log(LogService.LOG_WARNING, "Failed to disconnect from (old) MongoDB!", e);
        }

        try {
            connectToDB(newMongoDb, newUsername, newPassword);
        }
        catch (MongoException e) {
            m_log.log(LogService.LOG_WARNING, "Failed to connect to (new) MongoDB!", e);
            throw new ConfigurationException(DEFAULT_MONGODB_USERNAME, "Failed to connect!", e);
        }
    }

    /**
     * Creates a connection to MongoDB using the given credentials.
     * 
     * @param mongoDB the {@link MongoDB} facade to connect to;
     * @param userName the (optional) user name to use;
     * @param password the (optional) password to use.
     * @throws MongoException in case the connection or authentication failed.
     */
    private void connectToDB(MongoDB mongoDB, String userName, String password) throws MongoException {
        if (!mongoDB.connect(userName, password)) {
            throw new MongoException("Failed to connect to MongoDB! Authentication failed!");
        }
        
        DBCollection collection = mongoDB.getCollection();
        if (collection == null) {
            throw new MongoException("Failed to connect to MongoDB! No collection returned!");
        }

        collection.ensureIndex(new BasicDBObject(NAME, 1).append("unique", true));
    }
    
    /**
     * Returns the current database collection.
     * 
     * @return the database collection to work with, cannot be <code>null</code>.
     * @throws MongoException in case no connection to MongoDB exists.
     */
    private DBCollection getCollection() {
        MongoDB mongoDB = m_mongoDbRef.get();
        if (mongoDB == null) {
            throw new MongoException("No connection to MongoDB?!");
        }
        return mongoDB.getCollection();
    }

    /**
     * Returns the mandatory value for the given key.
     * 
     * @param properties the properties to get the mandatory value from;
     * @param key the key of the value to retrieve;
     * @return the value, never <code>null</code>.
     * @throws ConfigurationException in case the given key had no value.
     */
    private String getMandatoryProperty(Dictionary properties, String key) throws ConfigurationException {
        String result = getProperty(properties, key);
        if (result == null || "".equals(result.trim())) {
            throw new ConfigurationException(key, "cannot be null or empty!");
        }
        return result;
    }
    
    /**
     * Returns the value for the given key.
     * 
     * @param properties the properties to get the value from;
     * @param key the key of the value to retrieve;
     * @return the value, can be <code>null</code> in case no such key is present.
     * @throws ConfigurationException in case the given key had no value.
     */
    private String getProperty(Dictionary properties, String key) throws ConfigurationException {
        Object result = properties.get(key);
        if (result == null || !(result instanceof String)) {
            return null;
        }
        return (String) result;
    }
    
    /**
     * Creates a template object for the given role.
     * 
     * @param role the role to create a template object for, cannot be <code>null</code>.
     * @return a template object for MongoDB, never <code>null</code>.
     */
    private DBObject getTemplateObject(Role role) {
        BasicDBObject query = new BasicDBObject();
        query.put(TYPE, role.getType());
        query.put(NAME, role.getName());
        return query;
    }
    
    /**
     * Creates a template object for the given (role)name.
     * 
     * @param name the name of the role to create a template object for, cannot be <code>null</code>.
     * @return a template object for MongoDB, never <code>null</code>.
     */
    private DBObject getTemplateObject(String name) {
        BasicDBObject query = new BasicDBObject();
        query.put(NAME, name);
        return query;
    }
}
