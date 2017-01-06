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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.configurator.impl.Util;

/**
 * This object holds a sorted map of configurations
 */
public class AbstractState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final int VERSION = 1;

    private final Map<String, ConfigList> configurationsByPid = new TreeMap<>();

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
        out.writeObject(configurationsByPid);
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize fields
     */
    private void readObject(final java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version < 1 || version > VERSION ) {
            throw new ClassNotFoundException(this.getClass().getName());
        }
        Util.setField(this, "configurationsByPid", in.readObject());
    }

    public void add(final Config c) {
        ConfigList configs = this.configurationsByPid.get(c.getPid());
        if ( configs == null ) {
            configs = new ConfigList();
            this.configurationsByPid.put(c.getPid(), configs);
        }

        configs.add(c);
    }

    public Map<String, ConfigList> getConfigurations() {
        return this.configurationsByPid;
    }

    public ConfigList getConfigurations(final String pid) {
        return this.getConfigurations().get(pid);
    }

    public Collection<String> getPids() {
        return this.getConfigurations().keySet();
    }
}
