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
package org.apache.felix.sandbox.scrplugin.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>Component.java</code>...
 *
 */
public class Component {

    protected String name;

    protected Boolean enabled;

    protected Boolean immediate;

    protected String factory;
    protected Implementation implementation;

    protected List properties = new ArrayList();

    protected Service service;

    protected List references = new ArrayList();

    public List getProperties() {
        return this.properties;
    }

    public void setProperties(List properties) {
        this.properties = properties;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactory() {
        return this.factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isImmediate() {
        return this.immediate;
    }

    public void setImmediate(Boolean immediate) {
        this.immediate = immediate;
    }

    public Implementation getImplementation() {
        return this.implementation;
    }

    public void setImplementation(Implementation implementation) {
        this.implementation = implementation;
    }

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List getReferences() {
        return this.references;
    }

    public void setReferences(List references) {
        this.references = references;
    }

}
