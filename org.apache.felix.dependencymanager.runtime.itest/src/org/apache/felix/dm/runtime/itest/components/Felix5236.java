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
package org.apache.felix.dm.runtime.itest.components;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Checks support of primitive types for @Property annotation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(provides=Felix5236.class)
@Property(name="v1", value="s")
public class Felix5236 {
    public final static String ENSURE = "Felix5236";
    
    @ServiceDependency(filter = "(name=" + ENSURE + ")")
    volatile Ensure m_ensure;
    
    @Registered
    void registered(ServiceRegistration sr) {
        ServiceReference ref = sr.getReference();
        Utils.assertEquals(m_ensure, ref, "v1", "s", 1);
    }
}
