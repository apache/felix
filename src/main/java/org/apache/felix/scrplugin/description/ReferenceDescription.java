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
package org.apache.felix.scrplugin.description;

import java.lang.reflect.Field;

import org.apache.felix.scrplugin.annotations.ScannedAnnotation;

/**
 * A <code>ReferenceDescription</code> describes a reference
 * from a component to another service.
 *
 * In general all fields should be set by an annocation scanner,
 * no default values should be assumed for these fields:
 * <ul>
 * <li>name</li>
 * <li>interfaceName</li>
 * <li>target</li>
 * <li>cardinality</li>
 * <li>policy</li>
 * <li>strategy</li>
 * <li>field</li>
 * </ul>
 *
 * These values have the following default values:
 * <ul>
 * <li>bind : null</li>
 * <li>unbind : null</li>
 * <li>updated : null</li>
 * </ul>
 */
public class ReferenceDescription extends AbstractDescription {

    private String name;
    private String interfaceName;
    private String target;
    private ReferenceCardinality cardinality;
    private ReferencePolicy policy;
    private ReferenceStrategy strategy;

    private Field field;

    private MethodDescription bind;
    private MethodDescription unbind;
    private MethodDescription updated;

    public ReferenceDescription(final ScannedAnnotation annotation) {
        super(annotation);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfacename) {
        this.interfaceName = interfacename;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public ReferenceCardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(ReferenceCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public ReferencePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(ReferencePolicy policy) {
        this.policy = policy;
    }

    public MethodDescription getBind() {
        return bind;
    }

    public void setBind(MethodDescription bind) {
        this.bind = bind;
    }

    public MethodDescription getUnbind() {
        return unbind;
    }

    public void setUnbind(MethodDescription unbind) {
        this.unbind = unbind;
    }

    public MethodDescription getUpdated() {
        return updated;
    }

    public void setUpdated(MethodDescription updated) {
        this.updated = updated;
    }

    public ReferenceStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ReferenceStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public String toString() {
        return "ReferenceDescription [name=" + name + ", interfaceName="
                + interfaceName + ", target=" + target + ", cardinality="
                + cardinality + ", policy=" + policy + ", bind=" + bind
                + ", unbind=" + unbind + ", updated=" + updated + ", strategy="
                + strategy + ", field=" + field + ", annotation=" + annotation
                + "]";
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }
}
