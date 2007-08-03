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

/**
 * <code>Reference.java</code>...
 *
 */
public class Reference {

    protected String name;
    protected String interfacename;
    protected String target;
    protected String cardinality;
    protected String policy;
    protected String bind;
    protected String unbind;

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getInterfacename() {
        return this.interfacename;
    }
    public void setInterfacename(String interfacename) {
        this.interfacename = interfacename;
    }
    public String getTarget() {
        return this.target;
    }
    public void setTarget(String target) {
        this.target = target;
    }
    public String getCardinality() {
        return this.cardinality;
    }
    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }
    public String getPolicy() {
        return this.policy;
    }
    public void setPolicy(String policy) {
        this.policy = policy;
    }
    public String getBind() {
        return this.bind;
    }
    public void setBind(String bind) {
        this.bind = bind;
    }
    public String getUnbind() {
        return this.unbind;
    }
    public void setUnbind(String unbind) {
        this.unbind = unbind;
    }


}
