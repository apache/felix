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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


public class FieldActivatorComponent
{
    public @interface Config {
        String email() default "bar"; // property in component xml with value foo
        int port() default 443; // property in component xml with value 80
        long test() default 5; // no property in component xml, will be 0
    }

    private BundleContext bundle;

    private ComponentContext context;

    private Map<String, Object> config;

    private Config annotation;

    private boolean activated;

    private String activationTest;

    private final List<String> notSetBeforeActivate = new ArrayList<>();

    private final List<String> setBeforeActivate = new ArrayList<>();

    public FieldActivatorComponent()
    {
        notSetBeforeActivate.add("bundle");
        notSetBeforeActivate.add("context");
        notSetBeforeActivate.add("config");
        notSetBeforeActivate.add("annotation");
    }

    @SuppressWarnings("unused")
    private void activator()
    {
        // everything should be set here already
        check();
        activated = true;
    }

    private void addError(final String msg)
    {
        if ( activationTest == null )
        {
            activationTest = msg;
        }
        else
        {
            activationTest = activationTest + ", " + msg;
        }
    }

    private void check()
    {
        if ( bundle != null )
        {
            notSetBeforeActivate.remove("bundle");
            setBeforeActivate.add("bundle");
        }
        if ( context != null )
        {
            notSetBeforeActivate.remove("context");
            setBeforeActivate.add("context");
        }
        if ( config != null )
        {
            notSetBeforeActivate.remove("config");
            setBeforeActivate.add("config");
            if ( !config.get("email").equals("foo") )
            {
                addError("Wrong value for map.email: " + config.get("email"));
            }
            if ( !config.get("port").equals("80") )
            {
                addError("Wrong value for map.email: " + config.get("port"));
            }
            if ( config.get("test") != null )
            {
                addError("Wrong value for map.test: " + config.get("test"));
            }
        }
        if ( annotation != null )
        {
            notSetBeforeActivate.remove("annotation");
            setBeforeActivate.add("annotation");
            if ( !annotation.email().equals("foo") )
            {
                addError("Wrong value for annotation.email: " + annotation.email());
            }
            if ( annotation.port() != 80 )
            {
                addError("Wrong value for annotation.port: " + annotation.port());
            }
            if ( annotation.test() != 0 )
            {
                addError("Wrong value for annotation.test: " + annotation.test());
            }
        }
    }

    public boolean isActivateCalled()
    {
        return activated;
    }

    public List<String> setBeforeActivate()
    {
        return setBeforeActivate;
    }

    public List<String> notSetBeforeActivate()
    {
        return notSetBeforeActivate;
    }

    public String additionalError()
    {
        return activationTest;
    }
}
