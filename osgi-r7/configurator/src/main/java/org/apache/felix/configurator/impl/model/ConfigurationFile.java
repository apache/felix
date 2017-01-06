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

import java.net.URL;
import java.util.List;

/**
 * This object holds all configurations from a single file.
 * This is only an intermediate object.
 */
public class ConfigurationFile implements Comparable<ConfigurationFile> {

    private final URL url;

    private final List<Config> configurations;

    public ConfigurationFile(final URL url, final List<Config> configs) {
        this.url = url;
        this.configurations = configs;
    }

    @Override
    public int compareTo(final ConfigurationFile o) {
        return url.getPath().compareTo(o.url.getPath());
    }

    @Override
    public String toString() {
        return "ConfigurationFile [url=" + url + ", configurations=" + configurations + "]";
    }

    public List<Config> getConfigurations() {
        return this.configurations;
    }
}
