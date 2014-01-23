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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
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
 * <dt>server</dt>
 * <dd>A space separated string containing the MongoDB servers. The format for this string is: "<code>&lt;host1:port1&gt; &lt;host2:port2&gt;</code>". This value is optional;</dd>
 * <dt>dbname</dt>
 * <dd>A string value containing the name of the database to use for this store. This value is optional;</dd>
 * <dt>collection</dt>
 * <dd>The name of the database collection to use for this store. This value is optional;</dd>
 * <dt>username</dt>
 * <dd>A string value representing the name of the user to authenticate against MongoDB. This value is optional;</dd>
 * <dt>password</dt>
 * <dd>A string value representing the password to authenticate against MongoDB. This value is optional.</dd>
 * </dl>
 * <p>
 * Alternatively, one can also supply the above mentioned configuration keys prefixed with 
 * "<tt>org.apache.felix.useradmin.mongodb.</tt>" as system properties (e.g.: 
 * <tt>-Dorg.apache.felix.useradmin.mongodb.server=my.mongo.server:27017</tt>). However, this 
 * implies that only a single store can be configured on a system (which could be a sensible
 * default for some situations)!
 * </p>
 * <p>
 * By default, the following values are used:
 * </p>
 * <table>
 * <tr><td><tt>server</tt></td><td>"<tt>localhost:27017</tt>"</td></tr>
 * <tr><td><tt>dbname</tt></td><td>"<tt>ua_repo</tt>"</td></tr>
 * <tr><td><tt>collection</tt></td><td>"<tt>useradmin</tt>"</td></tr>
 * <tr><td><tt>username</tt></td><td>&lt;none&gt;</td></tr>
 * <tr><td><tt>password</tt></td><td>&lt;none&gt;</td></tr>
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
    private static final String KEY_MONGODB_SERVER = "server";
    /** The name of the MongoDB database instance. */
    private static final String KEY_MONGODB_DBNAME = "dbname";
    /** The username of the MongoDB database instance. */
    private static final String KEY_MONGODB_USERNAME = "username";
    /** The password of the MongoDB database instance. */
    private static final String KEY_MONGODB_PASSWORD = "password";
    /** The name of the MongoDB collection to use. */
    private static final String KEY_MONGODB_COLLECTION_NAME = "collection";

    private static final String PREFIX = PID.concat(".");
    /** Default MongoDB server; first checks a system property */
    private static final String DEFAULT_MONGODB_SERVER = System.getProperty(PREFIX.concat(KEY_MONGODB_SERVER), "localhost:27017");
    /** Default MongoDB name */
    private static final String DEFAULT_MONGODB_DBNAME = System.getProperty(PREFIX.concat(KEY_MONGODB_DBNAME), "ua_repo");
    /** Default MongoDB collection */
    private static final String DEFAULT_MONGODB_COLLECTION = System.getProperty(PREFIX.concat(KEY_MONGODB_COLLECTION_NAME), "useradmin");
    /** Default MongoDB username */
    private static final String DEFAULT_MONGODB_USERNAME = System.getProperty(PREFIX.concat(KEY_MONGODB_USERNAME));
    /** Default MongoDB password */
    private static final String DEFAULT_MONGODB_PASSWORD = System.getProperty(PREFIX.concat(KEY_MONGODB_PASSWORD));

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
    public Role addRole(String roleName, int type) throws MongoException {
        if (roleName == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        
        DBCollection coll = getCollection();

        Role role = getRole(roleName);
        if (role != null) {
            return null;
        }

        // Role does not exist; insert it...
        DBObject data = m_helper.serialize(roleName, type);

        WriteResult result = coll.insert(data);
        
        if (result.getLastError() != null) {
            result.getLastError().throwOnError();
        }

        // FELIX-4400: ensure we return the correct role...
        return getRole(roleName);
    }

    /**
     * Closes this store and disconnects from the MongoDB backend.
     */
    public void close() {
        MongoDB mongoDB = m_mongoDbRef.get();
        if (mongoDB != null) {
            mongoDB.disconnect();
        }
        m_mongoDbRef.set(null);
    }

    @Override
    public Role[] getRoles(String filterValue) throws InvalidSyntaxException, MongoException {
        List<Role> roles = new ArrayList<Role>();

        Filter filter = null;
        if (filterValue != null) {
            filter = FrameworkUtil.createFilter(filterValue);
        }

        DBCollection coll = getCollection();

        DBCursor cursor = coll.find();
        try {
            while (cursor.hasNext()) {
                // Hmm, there might be a more clever way of doing this...
                Role role = m_helper.deserialize(cursor.next());
                if ((filter == null) || filter.match(role.getProperties())) {
                    roles.add(role);
                }
            }
        } finally {
            cursor.close();
        }

        return roles.toArray(new Role[roles.size()]);
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
    public Role getRoleByName(String name) throws MongoException {
        return getRole(name);
    }

    @Override
    public Role removeRole(String roleName) throws MongoException {
        DBCollection coll = getCollection();
        
        Role role = getRole(roleName);
        if (role == null) {
            return null;
        }

        WriteResult result = coll.remove(getTemplateObject(role));

        if (result.getLastError() != null) {
            result.getLastError().throwOnError();
        }

        return role;
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
        // Defaults to "ua_repo"
        String newDbName = getProperty(properties, KEY_MONGODB_DBNAME, DEFAULT_MONGODB_DBNAME);
        // Defaults to "localhost:27017"
        String newServers = getProperty(properties, KEY_MONGODB_SERVER, DEFAULT_MONGODB_SERVER);
        // Defaults to "useradmin"
        String newCollectionName = getProperty(properties, KEY_MONGODB_COLLECTION_NAME, DEFAULT_MONGODB_COLLECTION);
        // Defaults to null
        String newUsername = getProperty(properties, KEY_MONGODB_USERNAME, DEFAULT_MONGODB_USERNAME);
        // Defaults to null. FELIX-3774; use correct property name...
        String newPassword = getProperty(properties, KEY_MONGODB_PASSWORD, DEFAULT_MONGODB_PASSWORD); 

        MongoDB newMongoDb = new MongoDB(newServers, newDbName, newCollectionName);

        MongoDB oldMongoDb;
        do {
            oldMongoDb = m_mongoDbRef.get();
        }
        while (!m_mongoDbRef.compareAndSet(oldMongoDb, newMongoDb));

        try {
            // FELIX-3775: oldMongoDb can be null when supplying the configuration for the first time...
            if (oldMongoDb != null) {
                oldMongoDb.disconnect();
            }
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
     * Returns the value for the given key from the given properties.
     * 
     * @param properties the properties to get the value from, may be <code>null</code>;
     * @param key the key to retrieve the value for, cannot be <code>null</code>;
     * @param defaultValue the default value to use in case no value is present in the given dictionary, the value is not a string, or the dictionary itself was <code>null</code>.
     * @return the value, can be <code>null</code> in case the given key lead to a null value, or a null value was supplied as default value.
     */
    private String getProperty(Dictionary properties, String key, String defaultValue) {
        String result = defaultValue;
        if (properties != null) {
            Object value = properties.get(key);
            if (value != null && (value instanceof String)) {
                result = (String) value;
            }
        }
        return result;
    }
    
    /**
     * Creates a template object for the given role.
     * 
     * @param role the role to create a template object for, cannot be <code>null</code>.
     * @return a template object for MongoDB, never <code>null</code>.
     */
    private DBObject getTemplateObject(Role role) {
        BasicDBObject query = new BasicDBObject();
        query.put(NAME, role.getName());
        query.put(TYPE, role.getType());
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
