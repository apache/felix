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
package org.apache.felix.scr.impl;

import org.apache.felix.service.command.Converter;
import org.osgi.framework.*;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * This class registers as a Converter service using ServiceFactory to avoid class-load errors when the Converter
 * API is not available (i.e. when the Gogo runtime bundle is absent).
 */
@SuppressWarnings("rawtypes")
class ComponentConverterFactory implements ServiceFactory {

    private final ComponentCommands commands;

    ComponentConverterFactory(ComponentCommands commands) {
        this.commands = commands;
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        Object converter = new Converter() {
            @Override
            public Object convert(Class<?> aClass, Object o) throws Exception {
                return commands.convert(aClass, o);
            }
            @Override
            public CharSequence format(Object o, int i, Converter converter) throws Exception {
                return commands.format(o, i);
            }
        };
        return converter;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        // no-op
    }
}
