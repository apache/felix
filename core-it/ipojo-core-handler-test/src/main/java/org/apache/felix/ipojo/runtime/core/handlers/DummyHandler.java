/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.handlers;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.runtime.core.services.User;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.Dictionary;


public class DummyHandler extends PrimitiveHandler {

    public DummyHandler() {
    }

    /*------------------------------------------------------------*
     *      Handler Specific Methods                              *
     *------------------------------------------------------------*/

    @Override
    public void initializeComponentFactory(ComponentTypeDescription typeDesc, Element metadata) throws ConfigurationException {
        // Initialize
        super.initializeComponentFactory(typeDesc, metadata);
    }

    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
    }


    public void bindUser(User user) {
        // in order to test
        user.getName();
    }

    public void unBindUser(User user) {
        // in order to test
        user.getType();
    }

    public void validate() {
        // do nothing
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
