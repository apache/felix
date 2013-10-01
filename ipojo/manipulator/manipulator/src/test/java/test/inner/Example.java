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

/**
 * Created with IntelliJ IDEA.
 * User: clement
 * Date: 01/10/13
 * Time: 14:57
 * To change this template use File | Settings | File Templates.
 */
public class Example {

    private String myString;

    org.apache.felix.ipojo.InstanceManager __IM;
    boolean __M1___run;
    boolean __MFoo___run;

    public void doSomething() {
        Runnable runnable = new Runnable() {
            public void run() {
                if (! __M1___run) {
                    __run();
                } else {
                    try {
                        __IM.onEntry(Example.this, "__M1___run", new Object[0]);
                        __run();
                        __IM.onExit(Example.this, "__M1___run", new Object[0]);
                    } catch (Throwable e) {
                        __IM.onError(Example.this, "__M1___run", e);
                    }
                }
            }

            private void __run() {
                System.out.println(myString);
            }
        };
        runnable.run();
    }

    private class Foo {
        public void run() {
            if (! __MFoo___run) {
                __run();
            } else {
                try {
                    __IM.onEntry(Example.this, "__MFoo___run", new Object[0]);
                    __run();
                    __IM.onExit(Example.this, "__MFoo___run", new Object[0]);
                } catch (Throwable e) {
                    __IM.onError(Example.this, "__MFoo___run", e);
                }
            }
        }

        private void __run() {
            System.out.println(myString);
        }
    }

}
