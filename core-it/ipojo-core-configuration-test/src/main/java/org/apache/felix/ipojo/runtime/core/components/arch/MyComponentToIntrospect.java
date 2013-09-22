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

package org.apache.felix.ipojo.runtime.core.components.arch;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.services.CheckService;

import java.util.Properties;

/**
 * A component with properties with/without default values to check that the architecture contains all the required
 * information.
 *
 * <ul>
 *     <li>p1 : the property has no value, the field has one</li>
 *     <li>p2 : the property has no value, it receives one in the constructor</li>
 *     <li>p3 : the property has a default value</li>
 *     <li>p4 : the property has a default value, overridden by the field value</li>
 *     <li>p5 : the property has a default value, overridden by the instance configuration</li>
 *     <li>p6 : the property has no value, the field is not initialized (not initialized means no value)</li>
 *     <li>p62 : the property has no value, the field is initialized to null </li>
 *     <li>p7 : the property has no value, it receives one in the constructor, but stay unused</li>
 *     <li>p8 : the property has no value, it does not receive one.</li>
 *     <li>p9 : the property has no value, it receives one in a method.</li>
 * </ul>
 */
@Component(propagation = true)
@Provides
public class MyComponentToIntrospect implements CheckService {

    @Property(name="p2")
    private final String p2;

    @Property
    private String p1 = "v1";

    @Property(name="p3", value = "v3")
    private String p3;

    @Property(value = "v4")
    private String p4 = "v42";

    @Property(value = "v5")
    private String p5 = "v52";

    @Property
    private String p6;

    @Property
    private String p62 = null;

    public MyComponentToIntrospect(@Property(name="p7") String v7) {
        p2 = "v2";
    }


    @Property(name="p8")
    public void setP8(String v8) {

    }

    @Property(name="p9")
    public void setP9(String v9) {

    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public Properties getProps() {
        return null;
    }
}
