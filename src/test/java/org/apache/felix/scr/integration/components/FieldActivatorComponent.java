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
package org.apache.felix.scr.integration.components;


import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


public class FieldActivatorComponent
{
    public @interface Config {
        String email() default "bar";
        int port() default 443;
        long test() default 5;
    }

    private BundleContext bundle;

    private ComponentContext context;

    private Map<String, Object> config;

    private Config annotation;

    public String test()
    {
        if ( bundle == null ) {
            return "bundle is null";
        }
        if ( context == null ) {
            return "context is null";
        }
        if ( config == null ) {
            return "config is null";
        }
        if ( annotation == null ) {
            return "annotation is null";
        }
        if ( !annotation.email().equals("foo") ) {
            return "Wrong value for annotation.email: " + annotation.email();
        }
        if ( annotation.port() != 80 ) {
            return "Wrong value for annotation.port: " + annotation.port();
        }
        if ( annotation.test() != 0 ) {
            return "Wrong value for annotation.test: " + annotation.test();
        }
        if ( !config.get("email").equals("foo") ) {
            return "Wrong value for map.email: " + config.get("email");
        }
        if ( !config.get("port").equals("80") ) {
            return "Wrong value for map.email: " + config.get("port");
        }
        if ( config.get("test") != null ) {
            return "Wrong value for map.test: " + config.get("test");
        }
        return null;
    }

}
