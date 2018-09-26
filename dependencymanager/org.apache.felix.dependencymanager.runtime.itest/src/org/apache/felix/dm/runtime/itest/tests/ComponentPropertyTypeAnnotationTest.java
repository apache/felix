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
package org.apache.felix.dm.runtime.itest.tests;

import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.runtime.itest.components.ComponentDMPropertyTypeAnnotation;
import org.apache.felix.dm.runtime.itest.components.ComponentDMPropertyTypeArrayAnnotation;
import org.apache.felix.dm.runtime.itest.components.ComponentDMSingleValuedPropertyTypeAnnotation;
import org.apache.felix.dm.runtime.itest.components.ComponentJaxrsResourceAnnotation;
import org.apache.felix.dm.runtime.itest.components.ComponentPropertyTypeWithDictionaryPassedInUpdateCallback;
import org.apache.felix.dm.runtime.itest.components.FactoryPidWithPropertyTypeAnnotation;
import org.apache.felix.dm.runtime.itest.components.JaxrsComponentPropertyTypeAnnotation;
import org.apache.felix.dm.runtime.itest.components.MultipleReconfigurableComponentPropertyType;
import org.apache.felix.dm.runtime.itest.components.ReconfigurableComponentPropertyTypeWithOptionalConfigAnnotation;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Ensure that a Provider can be injected into a Consumer, using simple DM annotations.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class ComponentPropertyTypeAnnotationTest extends TestBase {
    
    public void testJaxRsPropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, JaxrsComponentPropertyTypeAnnotation.JaxRsConsumer.ENSURE);
        e.waitForStep(2, 5000);
        sr1.unregister();
        e.ensure();
    }
    
    public void testCustomPropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, ComponentDMPropertyTypeAnnotation.ENSURE);
        e.waitForStep(3, 5000);
        sr1.unregister();
        e.ensure();
    }
    
    public void testCustomPropertyTypesArray() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, ComponentDMPropertyTypeArrayAnnotation.ENSURE);
        e.waitForStep(4, 5000);
        sr1.unregister();
        e.ensure();
    }

    public void testSingleValuedPropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, ComponentDMSingleValuedPropertyTypeAnnotation.ENSURE);
        e.waitForStep(3, 5000);
        sr1.unregister();
        e.ensure();
    }
    
    public void testJaxrsPropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, ComponentJaxrsResourceAnnotation.ENSURE);
        e.waitForStep(2, 5000);
        sr1.unregister();
        e.ensure();
    }

    public void testCustomReconfigurablePropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, ReconfigurableComponentPropertyTypeWithOptionalConfigAnnotation.ENSURE);
        e.waitForStep(3, 5000);
        sr1.unregister();
        e.ensure();
    }
    
    public void testDictionaryPassedInUpdatedCallback() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, ComponentPropertyTypeWithDictionaryPassedInUpdateCallback.ENSURE);
        e.waitForStep(3, 5000);
        sr1.unregister();
        e.ensure();
    }
    
    public void testMultipleReconfigurablePropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, MultipleReconfigurableComponentPropertyType.ENSURE);
        e.waitForStep(2, 5000);
        sr1.unregister();
        e.ensure();
    }
    
    public void testFactoryPidWithPropertyTypes() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr1 = register(e, FactoryPidWithPropertyTypeAnnotation.ENSURE);
        e.waitForStep(3, 5000);
        sr1.unregister();
        e.ensure();
    }
    
}
