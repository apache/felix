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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import org.apache.felix.useradmin.RoleFactory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Provides a serializer for a role repository.
 */
final class RoleRepositorySerializer {
    
    private static final int VALUE_TYPE_STRING = 1;
    private static final int VALUE_TYPE_BARRAY = 2; 

    /**
     * Deserializes a given input stream.
     * 
     * @param is the input stream to deserialize, cannot be <code>null</code>.
     * @return a {@link Map} representing the role repository. It provides a 
     *         mapping between the name of the role as key and the associated
     *         role as value.
     * @throws IOException in case of I/O problems;
     * @throws IllegalArgumentException in case the given stream was <code>null</code>.
     */
    public Map deserialize(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null!");
        }
        return readRepository(new DataInputStream(is));
    }

    /**
     * Serializes a given map to the given output stream.
     * 
     * @param roleRepository the repository to serialize, cannot be <code>null</code>;
     * @param os the output stream to serialize to, cannot be  <code>null</code>.
     * @throws IOException in case of I/O problems;
     * @throws IllegalArgumentException in case the given parameter was <code>null</code>.
     */
    public void serialize(Map roleRepository, OutputStream os) throws IOException {
        if (roleRepository == null) {
            throw new IllegalArgumentException("Map cannot be null!");
        }
        if (os == null) {
            throw new IllegalArgumentException("OutputStream cannot be null!");
        }
        writeRepository(roleRepository, new DataOutputStream(os));
    }
    
    /**
     * Adds all groups, based on the given stub groups.
     * 
     * @param repository the repository to add the groups to, cannot be <code>null</code>;
     * @param stubGroups the list with stub groups to replace, cannot be <code>null</code>.
     * @throws IOException in case a referenced role was not found in the repository.
     */
    private void addGroups(Map repository, List stubGroups) throws IOException {
        // First create "empty" groups in the repository; we'll fill them in later on...
        Iterator sgIter = stubGroups.iterator();
        while (sgIter.hasNext()) {
            StubGroupImpl stubGroup = (StubGroupImpl) sgIter.next();

            Group group = (Group) RoleFactory.createRole(Role.GROUP, stubGroup.getName());
            copyDictionary(stubGroup.getProperties(), group.getProperties());
            copyDictionary(stubGroup.getCredentials(), group.getCredentials());

            repository.put(group.getName(), group);
        }
        
        int origSize = stubGroups.size();
        while (!stubGroups.isEmpty()) {
            List copy = new ArrayList(stubGroups);
            
            int size = copy.size();
            for (int i = 0; i < size; i++) {
                StubGroupImpl stubGroup = (StubGroupImpl) copy.get(i);

                Group group = (Group) repository.get(stubGroup.getName());
                if (group != null) {
                    resolveGroupMembers(stubGroup, group, repository);
                    stubGroups.remove(stubGroup);
                }
            }

            // In case we didn't resolve any groups; we should fail...
            if (origSize == stubGroups.size()) {
                throw new IOException("Failed to resolve groups: " + stubGroups);
            }

            origSize = stubGroups.size();
        }
    }
    
    /**
     * Converts a given {@link Dictionary} implementation to a {@link Map} implementation.
     * 
     * @param dictionary the dictionary to convert, cannot be <code>null</code>.
     * @return a {@link Map} instance with all the same key-value pairs as the given dictionary, never <code>null</code>.
     */
    private Map convertToMap(Dictionary dictionary) {
        Map result = new HashMap();
        if (dictionary instanceof Map) {
            result.putAll((Map) dictionary);
        } else {
            Enumeration keyEnum = dictionary.keys();
            while (keyEnum.hasMoreElements()) {
                Object key = keyEnum.nextElement();
                result.put(key, dictionary.get(key));
            }
        }
        return result;
    }
    
    /**
     * Copies the contents of a given dictionary to a given other dictionary.
     * 
     * @param source the dictionary to copy from;
     * @param dest the dictionary to copy to.
     */
    private void copyDictionary(Dictionary source, Dictionary dest) {
        Enumeration keys = source.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = source.get(key);
            dest.put(key, value);
        }
    }

    /**
     * Returns the role with the given name from the given repository.
     * 
     * @param repository the repository to obtain the roles from, cannot be <code>null</code>;
     * @param name the name of the role to retrieve, cannot be <code>null</code>.
     * @return a role matching the given name, or <code>null</code> if no such role exists.
     */
    private Role getRoleFromRepository(Map repository, String name) {
        Role role;
        if (Role.USER_ANYONE.equals(name)) {
            role = RoleFactory.createRole(Role.USER_ANYONE);
        } else {
            role = (Role) repository.get(name);
        }
        return role;
    }
    
    /**
     * Reads and fills a given dictionary.
     * 
     * @param dict the dictionary to read & fill, cannot be <code>null</code>;
     * @param dis the input stream to read the data from, cannot be <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private void readDictionary(Dictionary dict, DataInputStream dis) throws IOException {
        // Read the number of entries...
        int count = dis.readInt();
        while (count-- > 0) {
            // Read the name of the key...
            String key = dis.readUTF();
            // Read the type of the value...
            int type = dis.read();
            // Read the value & add the actual entry...
            if (VALUE_TYPE_BARRAY == type) {
                int length = dis.readInt();
                byte[] value = new byte[length];
                if (dis.read(value, 0, length) != length) {
                    throw new IOException("Invalid repository; failed to correctly read dictionary!");
                }
                dict.put(key, value);
            } else if (VALUE_TYPE_STRING == type) {
                dict.put(key, dis.readUTF());
            }
        }
    }

    /**
     * Reads a (stub) group from the given input stream.
     * 
     * @param dis the input stream to read the data from, cannot be <code>null</code>.
     * @return the read (stub) group, never <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private StubGroupImpl readGroup(DataInputStream dis) throws IOException {
        StubGroupImpl group = new StubGroupImpl(dis.readUTF());
        
        readDictionary(group.getProperties(), dis);
        readDictionary(group.getCredentials(), dis);
        
        // Read the number of basic members...
        int count = dis.readInt();
        while (count-- > 0) {
            group.addMember(dis.readUTF());
        }
        
        // Read the number of required members...
        count = dis.readInt();
        while (count-- > 0) {
            group.addRequiredMember(dis.readUTF());
        }
        
        return group;
    }

    /**
     * Reads the entire repository from the given input stream.
     * 
     * @param dis the input stream to read the data from, cannot be <code>null</code>.
     * @return the repository {@link Map}, never <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private Map readRepository(DataInputStream dis) throws IOException {
        Map repository = new HashMap();
        
        int entryCount = dis.readInt();
        
        List stubGroups = new ArrayList();
        
        // Keep reading until no more types can be read...
        while (entryCount-- > 0) {
            int type = dis.readInt();
            
            Role role = null;
            if (Role.GROUP == type) {
                stubGroups.add(readGroup(dis));
            } else if (Role.USER == type) {
                role = readUser(dis);
            } else {
                role = readRole(dis);
            }
            
            if (role != null) {
                repository.put(role.getName(), role);
            }
        }
        
        // Post processing stage: replace all stub groups with real group implementations...
        addGroups(repository, stubGroups);
        
        return repository;
    }
    
    /**
     * Reads a role from the given input stream.
     * 
     * @param dis the input stream to read the data from, cannot be <code>null</code>.
     * @return the read role, never <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private Role readRole(DataInputStream dis) throws IOException {
        Role role = RoleFactory.createRole(Role.ROLE, dis.readUTF());
        
        readDictionary(role.getProperties(), dis);
        
        return role;
    }

    /**
     * Reads a user from the given input stream.
     * 
     * @param dis the input stream to read the data from, cannot be <code>null</code>.
     * @return the read user, never <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private User readUser(DataInputStream dis) throws IOException {
        User user = (User) RoleFactory.createRole(Role.USER, dis.readUTF());
        
        readDictionary(user.getProperties(), dis);
        readDictionary(user.getCredentials(), dis);
        
        return user;
    }

    /**
     * Resolves all basic and required group members for a given group, based on the names from the given stub group.
     * 
     * @param stubGroup the stub group to convert, cannot be <code>null</code>;
     * @param repository the repository to take the roles from, cannot be <code>null</code>.
     * @return a concrete {@link Group} instance with all members resolved, or <code>null</code> if not all members could be resolved.
     * @throws IOException in case a referenced role was not found in the repository.
     */
    private void resolveGroupMembers(StubGroupImpl stubGroup, Group group, Map repository) throws IOException {
        List names = stubGroup.getMemberNames();
        int size = names.size();

        for (int i = 0; i < size; i++) {
            String name = (String) names.get(i);
            Role role = getRoleFromRepository(repository, name);
            if (role == null) {
                throw new IOException("Unable to find referenced basic member: " + name);
            }
            group.addMember(role);
        }
        
        names = stubGroup.getRequiredMemberNames();
        size = names.size();
        
        for (int i = 0; i < size; i++) {
            String name = (String) names.get(i);
            Role role = getRoleFromRepository(repository, name);
            if (role == null) {
                throw new IOException("Unable to find referenced required member: " + name);
            }
            group.addRequiredMember(role);
        }
    }

    /**
     * Writes a given dictionary to the given output stream.
     * 
     * @param dict the dictionary to write, cannot be <code>null</code>;
     * @param dos the output stream to write the data to, cannot be <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private void writeDictionary(Dictionary dict, DataOutputStream dos) throws IOException {
        Map properties = convertToMap(dict);
        
        Set entries = properties.entrySet();
        int size = entries.size();

        // Write the number of entries...
        dos.writeInt(size);
        
        Iterator entriesIter = entries.iterator();
        while (entriesIter.hasNext()) {
            Map.Entry entry = (Entry) entriesIter.next();
            
            dos.writeUTF((String) entry.getKey());
            
            Object value = entry.getValue();
            if (value instanceof String) {
                dos.write(VALUE_TYPE_STRING);
                dos.writeUTF((String) value);
            } else if (value instanceof byte[]) {
                dos.write(VALUE_TYPE_BARRAY);
                dos.writeInt(((byte[]) value).length);
                dos.write((byte[]) value);
            }
        }
    }

    /**
     * Writes a given group to the given output stream.
     * 
     * @param group the group to write, cannot be <code>null</code>.
     * @param dos the output stream to write the data to, cannot be <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private void writeGroup(Group group, DataOutputStream dos) throws IOException {
        dos.writeUTF(group.getName());

        writeDictionary(group.getProperties(), dos);
        writeDictionary(group.getCredentials(), dos);
        
        Role[] m = group.getMembers();

        if (m == null) {
            dos.writeInt(0);
        } else {
            // Write the number of basic members...
            dos.writeInt(m.length);
            // Write the names of the basic members...
            for (int i = 0; i < m.length; i++) {
                dos.writeUTF(m[i].getName());
            }
        }

        m = group.getRequiredMembers();
        
        if (m == null) {
            dos.writeInt(0);
        } else {
            // Write the number of required members...
            dos.writeInt(m.length);
            // Write the names of the required members...
            for (int i = 0; i < m.length; i++) {
                dos.writeUTF(m[i].getName());
            }
        }
    }
    
    /**
     * Writes the given repository to the given output stream.
     * 
     * @param repository the repository to write, cannot be <code>null</code>;
     * @param dos the output stream to write the data to, cannot be <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private void writeRepository(Map repository, DataOutputStream dos) throws IOException {
        Collection values = repository.values();
        Iterator valuesIter = values.iterator();
        
        // Write the total number of entries in our repository first...
        dos.writeInt(values.size());
        
        while (valuesIter.hasNext()) {
            Role role = (Role) valuesIter.next();
            
            int type = role.getType();
            
            dos.writeInt(type);
            
            if (Role.GROUP == type) {
                writeGroup((Group) role, dos);
            } else if (Role.USER == type) {
                writeUser((User) role, dos);
            } else {
                writeRole(role, dos);
            }
        }
    }
    
    /**
     * Writes a given role to the given output stream.
     * 
     * @param role the role to write, cannot be <code>null</code>.
     * @param dos the output stream to write the data to, cannot be <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private void writeRole(Role role, DataOutputStream dos) throws IOException {
        dos.writeUTF(role.getName());

        writeDictionary(role.getProperties(), dos);
    }
    
    /**
     * Writes a given user to the given output stream.
     * 
     * @param user the user to write, cannot be <code>null</code>.
     * @param dos the output stream to write the data to, cannot be <code>null</code>.
     * @throws IOException in case of I/O problems.
     */
    private void writeUser(User user, DataOutputStream dos) throws IOException {
        dos.writeUTF(user.getName());

        writeDictionary(user.getProperties(), dos);
        writeDictionary(user.getCredentials(), dos);
    }
}
