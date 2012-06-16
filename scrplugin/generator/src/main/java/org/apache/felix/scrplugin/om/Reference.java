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
package org.apache.felix.scrplugin.om;

import java.lang.reflect.Field;

import org.apache.felix.scrplugin.annotations.ScannedAnnotation;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferencePolicy;
import org.apache.felix.scrplugin.description.ReferencePolicyOption;
import org.apache.felix.scrplugin.description.ReferenceStrategy;

/**
 * <code>Reference.java</code>...
 *
 */
public class Reference extends AbstractObject {

    protected String name;
    protected String interfacename;
    protected String target;
    protected ReferenceCardinality cardinality;
    protected ReferencePolicy policy;
    protected ReferencePolicyOption policyOption;
    protected String bind;
    protected String unbind;
    protected String updated;

    /** @since 1.0.9 */
    protected ReferenceStrategy strategy;

    private Field field;

    /**
     * Constructor from java source.
     */
    public Reference(final ScannedAnnotation annotation, final String sourceLocation) {
        super(annotation, sourceLocation);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Field getField() {
        return this.field;
    }

    public void setField(Field field) {
        this.field = field;
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

    public ReferenceCardinality getCardinality() {
        return this.cardinality;
    }

    public void setCardinality(ReferenceCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public ReferencePolicy getPolicy() {
        return this.policy;
    }

    public void setPolicy(ReferencePolicy policy) {
        this.policy = policy;
    }

    public ReferencePolicyOption getPolicyOption() {
        return this.policyOption;
    }

    public void setPolicyOption(ReferencePolicyOption policyOption) {
        this.policyOption = policyOption;
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

    public String getUpdated() {
        return this.updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    /** @since 1.0.9 */
    public ReferenceStrategy getStrategy() {
        return strategy;
    }

    /** @since 1.0.9 */
    public void setStrategy(ReferenceStrategy strategy) {
        this.strategy = strategy;
    }

    /** @since 1.0.9 */
    public boolean isLookupStrategy() {
        return this.getStrategy() == ReferenceStrategy.LOOKUP;
    }
}
