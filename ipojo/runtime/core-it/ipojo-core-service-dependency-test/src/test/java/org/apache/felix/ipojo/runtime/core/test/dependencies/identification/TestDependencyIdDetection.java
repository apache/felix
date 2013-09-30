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

package org.apache.felix.ipojo.runtime.core.test.dependencies.identification;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyCallback;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TestDependencyIdDetection extends Common {
    String factory = "org.apache.felix.ipojo.runtime.core.test.components.identification.ComponentWithCustomMethods";

    @Test
    public void testSetAndUnSetMethods() {
        ComponentInstance instance = ipojoHelper.createComponentInstance(factory);
        DependencyHandlerDescription dh = (DependencyHandlerDescription) instance.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:requires");

        DependencyDescription dd = findDependencyById(dh, "MyOtherService");
        assertThat(dd).isNotNull();

        DependencyCallback[] callbacks = dd.getDependency().getDependencyCallbacks();
        assertThat(callbacks).isNotNull();

        DependencyCallback bind = getBindCallback(callbacks);
        DependencyCallback unbind = getUnbindCallback(callbacks);

        assertThat(bind).isNotNull();
        assertThat(unbind).isNotNull();

        assertThat(bind.getMethodName()).isEqualTo("setMyOtherService");
        assertThat(unbind.getMethodName()).isEqualTo("unsetMyOtherService");
    }

    @Test
    public void testAddAndRemoveMethods() {
        ComponentInstance instance = ipojoHelper.createComponentInstance(factory);
        DependencyHandlerDescription dh = (DependencyHandlerDescription) instance.getInstanceDescription()
                .getHandlerDescription("org.apache.felix.ipojo:requires");

        DependencyDescription dd = findDependencyById(dh, "MyService");
        assertThat(dd).isNotNull();

        DependencyCallback[] callbacks = dd.getDependency().getDependencyCallbacks();
        assertThat(callbacks).isNotNull();

        DependencyCallback bind = getBindCallback(callbacks);
        DependencyCallback unbind = getUnbindCallback(callbacks);

        assertThat(bind).isNotNull();
        assertThat(unbind).isNotNull();

        assertThat(bind.getMethodName()).isEqualTo("addMyService");
        assertThat(unbind.getMethodName()).isEqualTo("removeMyService");
    }

    private DependencyCallback getBindCallback(DependencyCallback[] callbacks) {
        for (DependencyCallback callback : callbacks) {
            if (callback.getMethodType() == DependencyCallback.BIND) {
                return callback;
            }
        }
        return null;
    }

    private DependencyCallback getUnbindCallback(DependencyCallback[] callbacks) {
        for (DependencyCallback callback : callbacks) {
            if (callback.getMethodType() == DependencyCallback.UNBIND) {
                return callback;
            }
        }
        return null;
    }

    private DependencyDescription findDependencyById(DependencyHandlerDescription dh, String id) {
        for (DependencyDescription dd : dh.getDependencies()) {
            if (dd.getId().equals(id)) {
                return dd;
            }
        }
        return null;
    }


}
