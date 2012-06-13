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
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scrplugin.SCRDescriptorException;

/**
 * <code>Service</code>
 * 
 * contains all service information of a component.
 */
public class Service {

    /** Flag for the service factory. */
    private boolean isServiceFactory;

    /** The list of implemented interfaces. */
    private final List<Interface> interfaces = new ArrayList<Interface>();

    /**
     * Is this a service factory?
     */
    public boolean isServiceFactory() {
        return this.isServiceFactory;
    }

    /**
     * Set the service factory flag.
     */
    public void setServiceFactory(final boolean flag) {
        this.isServiceFactory = flag;
    }

    /**
     * Return the list of interfaces.
     */
    public List<Interface> getInterfaces() {
        return this.interfaces;
    }

    /**
     * Search for an implemented interface.
     * 
     * @param name
     *            The name of the interface.
     * @return The interface if it is implemented by this service or null.
     */
    public Interface findInterface(final String name) {
        final Iterator<Interface> i = this.getInterfaces().iterator();
        while (i.hasNext()) {
            final Interface current = i.next();
            if (current.getInterfaceName().equals(name)) {
                return current;
            }
        }
        return null;
    }

    /**
     * Add an interface to the list of interfaces.
     * 
     * @param interf
     *            The interface.
     */
    public void addInterface(final Interface interf) {
        // add interface only once
        if (this.findInterface(interf.getInterfaceName()) == null) {
            this.interfaces.add(interf);
        }
    }

    /**
     * Validate the service.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(final Context context) throws SCRDescriptorException {
        for (final Interface interf : this.getInterfaces()) {
            interf.validate(context);
        }
    }
}
