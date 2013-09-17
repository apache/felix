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
package org.apache.felix.ipojo.runtime.core.test.interceptors;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.dependency.interceptors.DefaultDependencyInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceBindingInterceptor;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * A binding interceptor generating a proxy to monitor the invocations.
 */
@Component
@Provides
public class ProxyBindingInterceptor extends DefaultDependencyInterceptor implements ServiceBindingInterceptor, CheckService {

    @ServiceProperty
    private String target;

    private HashMap<ServiceReference, Object> deps = new HashMap<ServiceReference, Object>();
    private Dictionary data = new Hashtable();


    private void increment(String key) {
        if (data.get(key) == null) {
            data.put(key, 1);
        } else {
            data.put(key, (Integer) data.get(key) + 1);
        }
    }

    @Override
    public <S> S getService(DependencyModel dependency, ServiceReference<S> reference, S service) {
        S proxy =  (S) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{dependency.getSpecification()}, new Interceptor(service));
        deps.put(reference, proxy);
        increment("bound");
        return proxy;
    }

    @Override
    public <S> void ungetService(DependencyModel dependency, ServiceReference<S> reference) {
        deps.remove(reference);
        increment("unbound");
    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public Dictionary getProps() {
        return data;
    }

    private class Interceptor implements InvocationHandler {

        private final Object service;

        public Interceptor(Object service) {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            increment(method.getName());

            if (method.getName().equals("toString")) {
                return this.toString();
            }
            return method.invoke(service, args);
        }
    }
}
