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

package org.apache.felix.ipojo.runtime.core.components.inheritance.d;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.runtime.core.components.inheritance.a.IA;
import org.apache.felix.ipojo.runtime.core.components.inheritance.b.IB;

@Component
public class D {
    @Requires
    private IB[] cImpls;
    private IB cImplDesired;

    // works if I use following instead and cast to C type below
    // in for loop
    // but this creates dependency on bundle C instead of just
    // the interface bundles A & B
    // @Requires(default-implementation=C)
    // private iB[] cImpls;
    // private C cImplDesired;

    @Validate
    public void start() {
        for (IB iimpl : cImpls) {

            // works just fine
            System.out.println("iimpl : " + iimpl);
            System.out.println(iimpl.methTwo());

            // following produces 
            // invalid D instance with NoMethodFoundError
            // unless I cast to C instead of iA
            if (((IA) iimpl).methOne().equals("one")) {
                cImplDesired = iimpl;
                System.out.println(iimpl.methOne());
            }
        }
    }
}
