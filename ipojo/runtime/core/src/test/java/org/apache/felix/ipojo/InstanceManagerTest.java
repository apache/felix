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
package org.apache.felix.ipojo;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.lang.reflect.Member;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstanceManagerTest {

    private static final int CALLERS = 500;

    @Test
    public void testConcurrencyOfMethodId() throws InterruptedException, ConfigurationException, ClassNotFoundException {
        ExecutorService executor = Executors.newFixedThreadPool(CALLERS);
        final AtomicInteger counter = new AtomicInteger();
        final AtomicInteger error = new AtomicInteger();

        ComponentFactory factory = mock(ComponentFactory.class);
        when(factory.loadClass(anyString())).thenReturn(MyComponent.class);
        when(factory.getClassName()).thenReturn(MyComponent.class.getName());
        Bundle bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(new Hashtable<String, String>());
        BundleContext context = mock(BundleContext.class);
        when(context.getBundle()).thenReturn(bundle);
        InstanceManager manager = new InstanceManager(factory, context, new HandlerManager[0]);

        Element method1 = new Element("method", "");
        method1.addAttribute(new Attribute("name", "foo"));
        method1.addAttribute(new Attribute("arguments", "{java.lang.String}"));
        method1.addAttribute(new Attribute("names", "{name}"));

        Element method2 = new Element("method", "");
        method2.addAttribute(new Attribute("name", "bar"));
        method2.addAttribute(new Attribute("arguments", "{java.lang.String}"));
        method2.addAttribute(new Attribute("names", "{name}"));

        Element method3 = new Element("method", "");
        method3.addAttribute(new Attribute("name", "baz"));
        method3.addAttribute(new Attribute("arguments", "{java.lang.String}"));
        method3.addAttribute(new Attribute("names", "{name}"));
        final MethodMetadata metadata1 = new MethodMetadata(method1);
        final MethodInterceptor interceptor = new MethodInterceptor() {
            public void onEntry(Object pojo, Member method, Object[] args) {
                if (method != null) {
                    counter.getAndIncrement();
                } else {
                    System.out.println("No method object for " + args[0]);
                    error.incrementAndGet();
                }
            }

            public void onExit(Object pojo, Member method, Object returnedObj) {
                if (method != null) {
                    counter.getAndDecrement();
                } else {
                    System.out.println("No method object");
                    error.incrementAndGet();
                }
            }

            public void onError(Object pojo, Member method, Throwable throwable) {

            }

            public void onFinally(Object pojo, Member method) {

            }
        };
        manager.register(metadata1, interceptor);
        final MethodMetadata metadata2 = new MethodMetadata(method2);
        manager.register(metadata2, interceptor);
        final MethodMetadata metadata3 = new MethodMetadata(method3);
        manager.register(metadata3, interceptor);

        MyComponent component = new MyComponent();
        manager.start();
        manager.load();
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(CALLERS);
        for (int i = 1; i < CALLERS + 1; ++i) {
            // create and start threads
            executor.execute(new Caller(manager, component,
                    metadata1.getMethodIdentifier(), startSignal, doneSignal, i));
            executor.execute(new Caller(manager, component,
                    metadata2.getMethodIdentifier(), startSignal, doneSignal, i));
            executor.execute(new Caller(manager, component,
                    metadata3.getMethodIdentifier(), startSignal, doneSignal, i));
        }

        startSignal.countDown();      // let all threads proceed
        assertThat(doneSignal.await(1, TimeUnit.MINUTES)).isTrue();
        assertThat(error.get()).isEqualTo(0);
    }

    private class Caller implements Runnable {

        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;
        private final int id;
        private final Object component;
        private final String identifier;
        private InstanceManager manager;

        public Caller(InstanceManager manager, Object component, String identifier, CountDownLatch startSignal,
                      CountDownLatch doneSignal, int name) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
            this.id = name;
            this.manager = manager;
            this.component = component;
            this.identifier = identifier;
        }

        public void run() {
            try {
                startSignal.await();
                manager.onEntry(component, identifier, new String[] {Integer.toString(id)});
                manager.onExit(component, identifier, null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                doneSignal.countDown();
            }
        }
    }


    private class MyComponent {

        public void foo(String name) {
            System.out.println(name);
        }

        public void bar(String name) {
            System.out.println(name);
        }

        public void baz(String name) {
            System.out.println(name);
        }

    }

}