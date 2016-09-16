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
package org.apache.felix.configurator.impl.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.configurator.impl.Util;
import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.osgi.framework.BundleContext;

public class State extends AbstractState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final int VERSION = 1;

    private static final String FILE_NAME = "state.ser";

    private final Map<Long, Long> bundlesLastModified = new HashMap<Long, Long>();

    private final Set<String> environments = new HashSet<>();

    private volatile Set<String> initialHashes;

    volatile transient boolean envsChanged = true;

    /**
     * Serialize the object
     * - write version id
     * - serialize fields
     * @param out Object output stream
     * @throws IOException
     */
    private void writeObject(final java.io.ObjectOutputStream out)
    throws IOException {
        out.writeInt(VERSION);
        out.writeObject(bundlesLastModified);
        out.writeObject(environments);
        out.writeObject(initialHashes);
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize fields
     */
    @SuppressWarnings("unchecked")
    private void readObject(final java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version < 1 || version > VERSION ) {
            throw new ClassNotFoundException(this.getClass().getName());
        }
        Util.setField(this, "bundlesLastModified", in.readObject());
        Util.setField(this, "environments", in.readObject());
        initialHashes = (Set<String>) in.readObject();
    }

    public static State createOrReadState(final BundleContext bc) {
        final File f = bc.getDataFile(FILE_NAME);
        if ( f == null || !f.exists() ) {
            return new State();
        }
        try ( final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f)) ) {

            return (State) ois.readObject();
        } catch ( final ClassNotFoundException | IOException e ) {
            SystemLogger.error("Unable to read persisted state from " + f, e);
            return new State();
        }
    }

    public static void writeState(final BundleContext bc, final State state) {
        final File f = bc.getDataFile(FILE_NAME);
        if ( f == null ) {
            // do nothing, no file system support
            return;
        }
        try ( final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f)) ) {
            oos.writeObject(state);
        } catch ( final IOException e) {
            SystemLogger.error("Unable to persist state to " + f, e);
        }
    }

    public Long getLastModified(final long bundleId) {
        return this.bundlesLastModified.get(bundleId);
    }

    public void setLastModified(final long bundleId, final long lastModified) {
        this.bundlesLastModified.put(bundleId, lastModified);
    }

    public void removeLastModified(final long bundleId) {
        this.bundlesLastModified.remove(bundleId);
    }

    public Set<Long> getKnownBundleIds() {
        return this.bundlesLastModified.keySet();
    }

    public Set<String> getEnvironments() {
        return this.environments;
    }

    public void changeEnvironments(final Set<String> envs) {
        this.envsChanged = this.environments.equals(envs);
        this.environments.clear();
        this.environments.addAll(envs);
    }

    public boolean environmentsChanged() {
        return this.envsChanged;
    }

    public Set<String> getInitialHashes() {
        return this.initialHashes;
    }

    public void setInitialHashes(final Set<String> value) {
        this.initialHashes = value;
    }

    /**
     * Add all configurations for a pid
     * @param pid The pid
     * @param configs The list of configurations
     */
    public void addAll(final String pid, final ConfigList configs) {
        if ( configs != null ) {
            ConfigList list = this.getConfigurations().get(pid);
            if ( list == null ) {
                list = new ConfigList();
                this.getConfigurations().put(pid, list);
            }

            list.addAll(configs);
        }
    }

    /**
     * Mark all configurations from that bundle as changed to reprocess them
     * @param bundleId The bundle id
     */
    public void checkEnvironments(final long bundleId) {
        for(final String pid : this.getPids()) {
            final ConfigList configList = this.getConfigurations(pid);
            for(final Config cfg : configList) {
                if ( cfg.getBundleId() == bundleId ) {
                    configList.setHasChanges(true);
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "State [bundlesLastModified=" + bundlesLastModified + ", environments=" + environments
                + ", initialHashes=" + initialHashes + "]";
    }
}
