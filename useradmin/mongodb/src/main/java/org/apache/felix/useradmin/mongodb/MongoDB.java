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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

/**
 * Provides a simple facade for accessing MongoDB.
 */
final class MongoDB {

    private final List<ServerAddress> m_servers;
    private final String m_dbName;
    private final String m_collectionName;
    
    private final AtomicReference<Mongo> m_mongoRef;
    
    /**
     * Creates a new {@link MongoDB} instance.
     * 
     * @param serverNames the space separated list of Mongo servers, cannot be <code>null</code> or empty;
     * @param dbName the name of the MongoDB to connect to, cannot be <code>null</code> or empty; 
     * @param collectionName the name of the collection to use, cannot be <code>null</code> or empty.
     */
    public MongoDB(String serverNames, String dbName, String collectionName) {
        if (serverNames == null || "".equals(serverNames.trim())) {
            throw new IllegalArgumentException("ServerNames cannot be null or empty!");
        }
        if (dbName == null || "".equals(dbName.trim())) {
            throw new IllegalArgumentException("DbName cannot be null or empty!");
        }
        if (collectionName == null || "".equals(collectionName.trim())) {
            throw new IllegalArgumentException("CollectionName cannot be null or empty!");
        }
        
        m_mongoRef = new AtomicReference<Mongo>();

        m_servers = parseServers(serverNames);
        m_dbName = dbName;
        m_collectionName = collectionName;
    }

    /**
     * Parses the space separated list of server names.
     * 
     * @param serverNames the server names, cannot be <code>null</code>.
     * @return a list of {@link ServerAddress}es to connect to, never <code>null</code>.
     * @throws IllegalArgumentException in case the given server names was invalid.
     */
    private static List<ServerAddress> parseServers(String serverNames) {
        String[] parts = serverNames.split("\\s+");

        List<ServerAddress> servers = new ArrayList<ServerAddress>();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                int colonPos = part.indexOf(":");
                if (colonPos > 0 && (colonPos < part.length() - 1)) {
                    String name = part.substring(0, colonPos);
                    String portStr = part.substring(colonPos + 1);
                    servers.add(new ServerAddress(name, Integer.valueOf(portStr)));
                }
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegal port number in: " + part);
            }
            catch (UnknownHostException e) {
                throw new IllegalArgumentException("Unknown host: " + part);
            }
        }

        if (servers.isEmpty()) {
            throw new IllegalArgumentException("No (valid) servers defined!");
        }

        return servers;
    }

    /**
     * Connects to the MongoDB with the supplied credentials.
     * 
     * @param userName the optional user name to use;
     * @param password the optional password to use.
     * @return <code>true</code> if the connection was succesful, <code>false</code> otherwise.
     */
    public boolean connect(String userName, String password) {
        Mongo newMongo = new Mongo(m_servers);

        Mongo oldMongo;
        do {
            oldMongo = m_mongoRef.get();
        } while (!m_mongoRef.compareAndSet(oldMongo, newMongo));
        
        DB db = newMongo.getDB(m_dbName);
        if ((userName != null) && (password != null)) {
            if (!db.authenticate(userName, password.toCharArray())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Returns the database collection to work in.
     * 
     * @return the {@link DBCollection}, never <code>null</code>.
     * @throws MongoException in case no connection to Mongo exists.
     */
    public DBCollection getCollection() {
        Mongo mongo = m_mongoRef.get();
        if (mongo == null) {
            throw new MongoException("Not connected to MongoDB!");
        }
        DB db = mongo.getDB(m_dbName);
        return db.getCollection(m_collectionName);
    }

    /**
     * Disconnects from the MongoDB.
     */
    public void disconnect() {
        Mongo mongo = m_mongoRef.get();
        if (mongo != null) {
            try {
                mongo.close();
            } finally {
                m_mongoRef.compareAndSet(mongo, null);
            }
        }
    }
}
