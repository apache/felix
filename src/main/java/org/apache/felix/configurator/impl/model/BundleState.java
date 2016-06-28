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

import java.io.Serializable;
import java.util.List;

/**
 * This object holds all configurations provided by a single bundle
 * when the configurations are read.
 * Later on it just holds the last modified information. The configurations
 * are merged into the {@code State} object.
 */
public class BundleState extends AbstractState implements Serializable {

    private static final long serialVersionUID = 1L;

    public void addFiles(final List<ConfigurationFile> allFiles) {
        for(final ConfigurationFile f : allFiles) {
            for(final Config c : f.getConfigurations()) {
                // set index
                final ConfigList list = this.getConfigurations(c.getPid());
                if ( list != null ) {
                    c.setIndex(list.size());
                }
                this.add(c);
            }
        }
    }
}
