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
package org.apache.felix.ipojo.runtime.core.components.annotations;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.Map;

/**
 * A component receiving both bundle context.
 */
@Component
@Provides
public class ComponentWithThreeConstructorParams implements CheckService {

    private final BundleContext bc;
    private BundleContext instance;

    private BundleContext component;

    public ComponentWithThreeConstructorParams(BundleContext bc, @Context(Context.Source.INSTANCE) BundleContext
            instance,
                                               @Context BundleContext component) {
        this.instance = instance;
        this.component = component;
        this.bc = bc;
    }

    @Override
    public boolean check() {
        return instance != null  && component != null;
    }

    @Override
    public Map map() {
        Map<String, BundleContext> map = new HashMap<String, BundleContext>();
        if (instance != null) {
            map.put("instance", instance);
        }
        if (component != null) {
            map.put("component", component);
        }
        if (bc != null) {
            map.put("bc", bc);
        }
        return map;
    }
}
