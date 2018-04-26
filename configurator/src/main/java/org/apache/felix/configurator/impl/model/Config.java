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
import java.io.IOException;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.List;

public class Config implements Serializable, Comparable<Config> {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final int VERSION = 1;

    /** The configuration pid */
    private final String pid;

    /** The configuration ranking */
    private final int ranking;

    /** The bundle id. */
    private final long bundleId;

    /** The configuration policy. */
    private final ConfigPolicy policy;

    /** The configuration properties. */
    private final Dictionary<String, Object> properties;

    /** The index within the list of configurations if several. */
    private volatile int index = 0;

    /** The configuration state. */
    private volatile ConfigState state = ConfigState.INSTALL;

    private volatile List<File> files;

    public Config(final String pid,
            final Dictionary<String, Object> properties,
            final long bundleId,
            final int ranking,
            final ConfigPolicy policy) {
        this.pid = pid;
        this.ranking = ranking;
        this.bundleId = bundleId;
        this.properties = properties;
        this.policy = policy;
    }

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
        out.writeObject(pid);
        out.writeObject(properties);
        out.writeObject(policy.name());
        out.writeLong(bundleId);
        out.writeInt(ranking);
        out.writeInt(index);
        out.writeObject(state.name());
        out.writeObject(files);
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
        ReflectionUtil.setField(this, "pid", in.readObject());
        ReflectionUtil.setField(this, "properties", in.readObject());
        ReflectionUtil.setField(this, "policy", ConfigPolicy.valueOf((String)in.readObject()));
        ReflectionUtil.setField(this, "bundleId", in.readLong());
        ReflectionUtil.setField(this, "ranking", in.readInt());
        this.index = in.readInt();
        this.state = ConfigState.valueOf((String)in.readObject());
        this.files = (List<File>) in.readObject();
    }

    /**
     * Get the PID
     * @return The pid.
     */
    public String getPid() {
        return this.pid;
    }

    /**
     * The configuration ranking
     * @return The configuration ranking
     */
    public int getRanking() {
        return this.ranking;
    }

    /**
     * The bundle id
     * @return The bundle id
     */
    public long getBundleId() {
        return this.bundleId;
    }

    /**
     * The index of the configuration. This value is only
     * relevant if there are several configurations for the
     * same pid with same ranking and bundle id.
     * @return The index within the configuration set
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Set the index
     */
    public void setIndex(final int value) {
        this.index = value;
    }

    /**
     * Get the configuration state
     * @return The state
     */
    public ConfigState getState() {
        return this.state;
    }

    /**
     * Set the configuration state
     * @param value The new state
     */
    public void setState(final ConfigState value) {
        this.state = value;
    }

    /**
     * Get the policy
     * @return The policy
     */
    public ConfigPolicy getPolicy() {
        return this.policy;
    }

    /**
     * Get all properties
     * @return The configuration properties
     */
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    public void setFiles(final List<File> f) {
        this.files = f;
    }

    public List<File> getFiles() {
        return this.files;
    }

    @Override
    public int compareTo(final Config o) {
        // sort by ranking, highest first
        // if same ranking, sort by bundle id, lowest first
        // if same bundle id, sort by index
        if ( this.getRanking() > o.getRanking() ) {
            return -1;
        } else if ( this.getRanking() == o.getRanking() ) {
            if ( this.getBundleId() < o.getBundleId() ) {
                return -1;
            } else if ( this.getBundleId() == o.getBundleId() ) {
                return this.getIndex() - o.getIndex();
            }
        }
        return 1;
    }

    @Override
    public String toString() {
        return "Config [pid=" + pid
                + ", ranking=" + ranking
                + ", bundleId=" + bundleId
                + ", index=" + index
                + ", properties=" + properties
                + ", policy=" + policy
                + ", state=" + state + "]";
    }
}
