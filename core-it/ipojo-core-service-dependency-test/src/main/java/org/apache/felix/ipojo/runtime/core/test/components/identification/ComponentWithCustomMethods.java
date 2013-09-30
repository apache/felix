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

package org.apache.felix.ipojo.runtime.core.test.components.identification;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.runtime.core.test.services.Call;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

import java.util.Set;

/**
 * A component reproducing FELIX-4250 - Specification deduction broken when the method does not start with the 'bind'
 * prefix (https://issues.apache.org/jira/browse/FELIX-4250).
 *
 * The add and remove methods should be attached to the same dependency (id = MyService)
 * The set and unset methods should be attached to the same dependency (id = MyOtherService)
 */
@Component
public class ComponentWithCustomMethods {

    Set<FooService> myServices;

    Set<Call> myOtherServices;

    @Bind(optional=true, aggregate=true)
    public void addMyService(FooService newService) {
        myServices.add(newService);
    }

    @Unbind
    public void removeMyService(FooService oldService) {
        myServices.remove(oldService);
    }


    @Bind(optional=true, aggregate=true)
    public void setMyOtherService(Call newService) {
        myOtherServices.add(newService);
    }

    @Unbind
    public void unsetMyOtherService(Call oldService) {
        myOtherServices.remove(oldService);
    }
}
