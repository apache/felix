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

package test.inner;

import org.apache.felix.ipojo.annotations.Component;

import java.util.concurrent.Callable;

/**
 * A component containing inner classes.
 */
@Component
public class ComponentWithInnerClasses{

    public String doSomething() {
        MyInnerWithANativeMethod nat = new MyInnerWithANativeMethod();
        MyInnerClass inn = new MyInnerClass();
        Computation compute = new Computation() {

            public String compute(final String s) {
                return "foo";
            }
        };
        return nat.foo() + MyStaticInnerClass.foo() + inn.foo() + compute.compute("");
    }

    private void doSomethingPrivately() {

    }

    private boolean flag;

    boolean getFlag() {
        return flag;
    }

    private String test = "";

    private String foo = "foo";

    public static final Callable<Integer> callable = new Callable<Integer>() {
        public Integer call() {
            return 1;
        }
    };

    public static int call() throws Exception {
        return callable.call();
    }

    private class MyInnerWithANativeMethod {

        public String foo() {
            return ComponentWithInnerClasses.this.foo;
        }

        public void bar() {
            if (! getFlag()) {
                test.charAt(0);
            }
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
