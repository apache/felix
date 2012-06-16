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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.annotations.ScannedAnnotation;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;

/**
 * <code>Component</code> is a described component.
 *
 */
public class Component extends AbstractObject {

    /** The name of the component. */
    protected String name;

    /** Is this component enabled? */
    protected Boolean enabled;

    /** Is this component immediately started. */
    protected Boolean immediate;

    /** The factory. */
    protected String factory;

    /** All properties. */
    protected List<Property> properties = new ArrayList<Property>();

    /** The corresponding service. */
    protected Service service;

    /** The references. */
    protected List<Reference> references = new ArrayList<Reference>();

    /** Is this an abstract description? */
    protected boolean isAbstract;

    /** Is this a descriptor to be ignored ? */
    protected boolean isDs;

    /** Configuration policy. (V1.1) */
    protected ComponentConfigurationPolicy configurationPolicy;

    /** Activation method. (V1.1) */
    protected String activate;

    /** Deactivation method. (V1.1) */
    protected String deactivate;

    /** Modified method. (V1.1) */
    protected String modified;

    /** The spec version. */
    protected SpecVersion specVersion;

    /** The class description. */
    private final ClassDescription classDescription;

    /** Configuration PID (V1.2) */
    private String configurationPid;

    /**
     * Constructor from java source.
     */
    public Component(final ClassDescription cDesc, final ScannedAnnotation annotation, final String sourceLocation) {
        super(annotation, sourceLocation);
        this.classDescription = cDesc;
    }

    public ClassDescription getClassDescription() {
        return this.classDescription;
    }

    /**
     * Get the spec version.
     */
    public SpecVersion getSpecVersion() {
        return this.specVersion;
    }

    /**
     * Set the spec version.
     */
    public void setSpecVersion(final SpecVersion value) {
        // only set a higher version, never "downgrade"
        if (this.specVersion == null || this.specVersion.ordinal() < value.ordinal()) {
            this.specVersion = value;
        }
    }

    /**
     * @return All properties of this component.
     */
    public List<Property> getProperties() {
        return this.properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public void addProperty(Property property) {
        this.properties.add(property);
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

    public Service getService() {
        return this.service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List<Reference> getReferences() {
        return this.references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public void addReference(Reference ref) {
        this.references.add(ref);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isDs() {
        return isDs;
    }

    public void setDs(boolean isDs) {
        this.isDs = isDs;
    }

    /**
     * Get the name of the activate method (or null for default)
     */
    public String getActivate() {
        return this.activate;
    }

    /**
     * Set the name of the deactivate method (or null for default)
     */
    public void setDeactivate(final String value) {
        this.deactivate = value;
    }

    /**
     * Get the name of the deactivate method (or null for default)
     */
    public String getDeactivate() {
        return this.deactivate;
    }

    /**
     * Set the name of the activate method (or null for default)
     */
    public void setActivate(final String value) {
        this.activate = value;
    }

    /**
     * Set the name of the modified method (or null for default)
     */
    public void setModified(final String value) {
        this.modified = value;
    }

    /**
     * Get the name of the modified method (or null for default)
     */
    public String getModified() {
        return this.modified;
    }

    /**
     * Return the configuration policy.
     */
    public ComponentConfigurationPolicy getConfigurationPolicy() {
        return this.configurationPolicy;
    }

    /**
     * Set the configuration policy.
     */
    public void setConfigurationPolicy(final ComponentConfigurationPolicy value) {
        this.configurationPolicy = value;
    }

    @Override
    public String toString() {
        return "Component [name=" + name + ", enabled=" + enabled + ", immediate=" + immediate + ", factory=" + factory
                        + ", properties=" + properties + ", service=" + service + ", references=" + references + ", isAbstract="
                        + isAbstract + ", isDs=" + isDs + ", configurationPolicy=" + configurationPolicy + ", activate="
                        + activate + ", deactivate=" + deactivate + ", modified=" + modified + ", specVersion=" + specVersion
                        + ", classDescription=" + classDescription + ", configurationPid=" + configurationPid + "]";
    }

    public String getConfigurationPid() {
        return configurationPid;
    }

    public void setConfigurationPid(String configurationPid) {
        this.configurationPid = configurationPid;
    }
}