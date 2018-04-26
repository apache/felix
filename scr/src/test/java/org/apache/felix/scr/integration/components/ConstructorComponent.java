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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


public class ConstructorComponent
{
    private enum Mode
    {
        FIELD,
        SINGLE,
        MULTI
    }
    public @interface Config
    {
        String email() default "bar"; // property in component xml with value foo
        int port() default 443; // property in component xml with value 80
        long test() default 5; // no property in component xml, will be 0
    }

    private final BundleContext bundle;

    private final ComponentContext context;

    private final Map<String, Object> config;

    private final Config annotation;

    private final Mode mode;

    private boolean activated;

    private String activationTest;

    private volatile Object ref;

    public ConstructorComponent(BundleContext b,
            ComponentContext c,
            Map<String, Object> configMap,
            Config configAnnotation)
    {
        this.bundle = b;
        this.context = c;
        this.config = configMap;
        this.annotation = configAnnotation;
        this.mode = Mode.FIELD;
    }

    public ConstructorComponent(final ConstructorSingleReference single)
    {
        this.bundle = null;
        this.context = null;
        this.config = null;
        this.annotation = null;
        this.mode = Mode.SINGLE;
        this.ref = single;
    }

    public ConstructorComponent(final List<ConstructorMultiReference> list)
    {
        this.bundle = null;
        this.context = null;
        this.config = null;
        this.annotation = null;
        this.mode = Mode.MULTI;
        this.ref = list;
    }

    @SuppressWarnings("unused")
    private void activator()
    {
        // everything should be set here already
        switch ( mode )
        {
            case FIELD :  activationTest = checkField();
                          break;
            case SINGLE : activationTest = checkSingle();
                          break;
            case MULTI : activationTest = checkMulti();
                         break;
        }
        activated = true;
    }

    @SuppressWarnings("rawtypes")
    private String checkMulti()
    {
        if ( ref == null )
        {
            return "ref is null";
        }
        if ( !(ref instanceof List) )
        {
            return "ref is wrong type: " + ref.getClass();
        }
        if ( ((List)ref).size() != 3)
        {
            return "ref has wrong size: " + ((List)ref).size();
        }
        final List<String> names = new ArrayList<>(Arrays.asList("a", "b", "c"));
        for(final Object obj : (List)ref)
        {
            if ( !(obj instanceof ConstructorMultiReference) )
            {
                return "ref has wrong type: " + obj.getClass();
            }
            names.remove(((ConstructorMultiReference)obj).getName());
        }
        if ( !names.isEmpty() )
        {
            return "Unexpected references found. Names not found: " + names;
        }
        return null;
    }

    private String checkSingle()
    {
        if ( ref == null )
        {
            return "ref is null";
        }
        if ( !(ref instanceof ConstructorSingleReference) )
        {
            return "ref has wrong type: " + ref.getClass();
        }
        if ( !((ConstructorSingleReference)ref).getName().equals("single"))
        {
            return "ref has wrong name: " + ((ConstructorSingleReference)ref).getName().equals("single");
        }
        return null;
    }

    private String checkField()
    {
        if ( bundle == null )
        {
            return "bundle is null";
        }
        if ( context == null )
        {
            return "context is null";
        }
        if ( config == null )
        {
            return "config is null";
        }
        if ( annotation == null )
        {
            return "annotation is null";
        }
        if ( !annotation.email().equals("foo") )
        {
            return "Wrong value for annotation.email: " + annotation.email();
        }
        if ( annotation.port() != 80 )
        {
            return "Wrong value for annotation.port: " + annotation.port();
        }
        if ( annotation.test() != 0 )
        {
            return "Wrong value for annotation.test: " + annotation.test();
        }
        if ( !config.get("email").equals("foo") )
        {
            return "Wrong value for map.email: " + config.get("email");
        }
        if ( !config.get("port").equals("80") )
        {
            return "Wrong value for map.email: " + config.get("port");
        }
        if ( config.get("test") != null )
        {
            return "Wrong value for map.test: " + config.get("test");
        }
        return null;
    }

    public String test() {
        if ( !activated )
        {
            return "activate not called";
        }
        return activationTest;
    }
}
