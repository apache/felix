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
package org.apache.felix.dm.shell;

/**
 * Unique identification of a component based on its name, type and bundle name.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentId implements Comparable<ComponentId> {
    private final String name;
    private final String type;
    private final String bundleName;
    
    public ComponentId(String name, String type, String bundleName) {
        super();
        this.name = name;
        this.type = type;
        this.bundleName = bundleName;
    }
    
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
    
    public String getBundleName() {
        return bundleName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundleName == null) ? 0 : bundleName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ComponentId other = (ComponentId) obj;
        if (bundleName == null) {
            if (other.bundleName != null)
                return false;
        }
        else if (!bundleName.equals(other.bundleName))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        }
        else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ComponentId [name=" + name + ", type=" + type + ", bundleName=" + bundleName + "]";
    }

    public int compareTo(ComponentId o) {
    	// TODO it is common to have compareTo use the same fields that equals does
    	// if not for a good reason, document this
        return name.compareTo(o.name);
    }
}
