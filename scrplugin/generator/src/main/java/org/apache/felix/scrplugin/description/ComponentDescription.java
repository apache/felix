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

import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.annotations.ScannedAnnotation;

/**
 * <code>ComponentDescription</code> is a described component.
 *
 * In general all fields should be set by an annocation scanner,
 * no default values should be assumed for these fields:
 * <ul>
 * <li>name</li>
 * <li>configurationPolicy</li>
 * </ul>
 *
 * These values have the following default values:
 * <ul>
 * <li>label : null - will be handled by the scr generator</li>
 * <li>description : null - will be handled by the scr generator</li>
 * <li>isAbstract : false</li>
 * <li>isInherit : true</li>
 * <li>createDs : true</li>
 * <li>createPid : true</li>
 * <li>createMetatype : false</li>
 * <li>enabled : null</li>
 * <li>immediate : null</li>
 * <li>factory : null</li>
 * <li>isSetMetatypeFactoryPid : false</li>
 * <li>activate : null</li>
 * <li>deactivate : null</li>
 * <li>modified : null</li>
 * <li>specVersion : null</li>
 * </ul>
 *
 */
public class ComponentDescription extends AbstractDescription {

    /** The name of the component. */
    private String name;

    /** The label of the component. */
    private String label;

    /** The description of the component. */
    private String description;

    /** Configuration policy. (V1.1) */
    private ComponentConfigurationPolicy configurationPolicy;

    /** Is this an abstract description? */
    private boolean isAbstract = false;

    /** Does this inherit? */
    private boolean isInherit = true;

    /** Create ds info */
    private boolean createDs = true;

    /** Create pid */
    private boolean createPid = true;

    /** Create metatype info. */
    private boolean createMetatype = false;

    /** Is this component enabled? */
    private Boolean enabled;

    /** Is this component immediately started. */
    private Boolean immediate;

    /** The factory. */
    private String factory;

    /** The set metatype factory pid flag. */
    private boolean isSetMetatypeFactoryPid = false;

    /** Activation method. (V1.1) */
    private MethodDescription activate;

    /** Deactivation method. (V1.1) */
    private MethodDescription deactivate;

    /** Modified method. (V1.1) */
    private MethodDescription modified;

    /** The spec version. */
    private SpecVersion specVersion;

    public ComponentDescription(final ScannedAnnotation annotation) {
        super(annotation);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getImmediate() {
        return immediate;
    }

    public void setImmediate(Boolean immediate) {
        this.immediate = immediate;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public boolean isSetMetatypeFactoryPid() {
        return isSetMetatypeFactoryPid;
    }

    public void setSetMetatypeFactoryPid(boolean isSetMetatypeFactoryPid) {
        this.isSetMetatypeFactoryPid = isSetMetatypeFactoryPid;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isInherit() {
        return isInherit;
    }

    public void setInherit(boolean isInherit) {
        this.isInherit = isInherit;
    }

    public boolean isCreateDs() {
        return createDs;
    }

    public void setCreateDs(boolean createDs) {
        this.createDs = createDs;
    }

    public boolean isCreatePid() {
        return createPid;
    }

    public void setCreatePid(boolean createPid) {
        this.createPid = createPid;
    }

    public boolean isCreateMetatype() {
        return createMetatype;
    }

    public void setCreateMetatype(boolean createMetatype) {
        this.createMetatype = createMetatype;
    }

    public ComponentConfigurationPolicy getConfigurationPolicy() {
        return configurationPolicy;
    }

    public void setConfigurationPolicy(ComponentConfigurationPolicy configurationPolicy) {
        this.configurationPolicy = configurationPolicy;
    }

    public MethodDescription getActivate() {
        return activate;
    }

    public void setActivate(MethodDescription activate) {
        this.activate = activate;
    }

    public MethodDescription getDeactivate() {
        return deactivate;
    }

    public void setDeactivate(MethodDescription deactivate) {
        this.deactivate = deactivate;
    }

    public MethodDescription getModified() {
        return modified;
    }

    public void setModified(MethodDescription modified) {
        this.modified = modified;
    }

    public SpecVersion getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    @Override
    public String toString() {
        return "ComponentDescription [name=" + name + ", label=" + label
                + ", description=" + description + ", enabled=" + enabled
                + ", immediate=" + immediate + ", factory=" + factory
                + ", isSetMetatypeFactoryPid=" + isSetMetatypeFactoryPid
                + ", isAbstract=" + isAbstract + ", isInherit=" + isInherit
                + ", createDs=" + createDs + ", createPid=" + createPid
                + ", createMetatype=" + createMetatype
                + ", configurationPolicy=" + configurationPolicy
                + ", activate=" + activate + ", deactivate=" + deactivate
                + ", modified=" + modified + ", specVersion=" + specVersion
                + ", annotation=" + annotation + "]";
    }
}