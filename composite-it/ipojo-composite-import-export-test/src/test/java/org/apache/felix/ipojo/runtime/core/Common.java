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

package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.composite.CompositeManager;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ow2.chameleon.testing.helpers.BaseTest;

import static junit.framework.Assert.fail;

/**
 * Bootstrap the test from this project
 */
@ExamReactorStrategy(PerMethod.class)
public class Common extends BaseTest {

    @Override
    public boolean deployiPOJOComposite() {
        return true;
    }


    public static ServiceContext getServiceContext(ComponentInstance ci) {
        if (ci instanceof CompositeManager) {
            return ((CompositeManager) ci).getServiceContext();
        } else {
            throw new RuntimeException("Cannot get the service context from a non composite instance");
        }
    }

    public void assertContains(String s, String[] arrays, String object) {
        for (String suspect : arrays) {
            if (object.equals(suspect)) {
                return;
            }
        }
        fail("Assertion failed : " + s);
    }
}
