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

package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.annotations.Component;

/**
 **
 * A component containing inner classes.
 */
@Component
public class ComponentWithInnerClasses{

    public String doSomething() {
        MyInnerWithANativeMethod nat = new MyInnerWithANativeMethod();
        MyInnerClass inn = new MyInnerClass();
        Runnable compute = new Runnable() {

            public void run() {
                // ...
            }
        };
        return "foo";
    }

    private String foo = "foo";

    private class MyInnerWithANativeMethod {

        public String foo() {
            return ComponentWithInnerClasses.this.foo;
        }

        public native void baz();

    }

    public static class MyStaticInnerClass {

        public static String foo() {
            return "foo";
        }

        public String bar() {
            return "bar";
        }

        public native void baz();
    }

    private class MyInnerClass {
        public String foo() {
            return ComponentWithInnerClasses.this.foo;
        }
    }


}
