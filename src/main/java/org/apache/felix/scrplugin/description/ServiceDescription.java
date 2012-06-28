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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scrplugin.annotations.ScannedAnnotation;

/**
 * If a component is a service, the {@link ClassDescription} should
 * contain a <code>ServiceDescription</code>.
 *
 * The service description defines whether this is a service factory
 * and which interfaces this service implements.
 */
public class ServiceDescription extends AbstractDescription {

    /** Flag for service factory. */
    private boolean isServiceFactory = false;

    /** The list of implemented interfaces. */
    protected final Set<String> interfaces = new LinkedHashSet<String>();

    public ServiceDescription(final ScannedAnnotation annotation) {
        super(annotation);
    }

    public boolean isServiceFactory() {
        return this.isServiceFactory;
    }

    public void setServiceFactory(boolean flag) {
        this.isServiceFactory = flag;
    }

    public Set<String> getInterfaces() {
        return this.interfaces;
    }

    /**
     * Add an interface to the list of interfaces.
     * @param interf The interface.
     */
    public void addInterface(final String interf) {
        this.interfaces.add(interf);
    }

    @Override
    public String toString() {
        return "ServiceDescription [isServiceFactory=" + isServiceFactory
                + ", interfaces=" + interfaces + ", annotation=" + annotation
                + "]";
    }

    @Override
    public AbstractDescription clone() {
        final ServiceDescription cd = new ServiceDescription(this.annotation);
        cd.setServiceFactory(this.isServiceFactory);
        for(final String i : this.getInterfaces()) {
            cd.addInterface(i);
        }

        return cd;
    }
}
