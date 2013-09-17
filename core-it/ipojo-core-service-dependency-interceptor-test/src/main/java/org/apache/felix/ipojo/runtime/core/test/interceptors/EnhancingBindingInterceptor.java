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
import org.apache.felix.ipojo.runtime.core.test.services.Enhanced;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A binding interceptor enhancing the service object.
 */
@Component(immediate = true)
@Provides
public class EnhancingBindingInterceptor extends DefaultDependencyInterceptor implements ServiceBindingInterceptor {

    @ServiceProperty
    private String target;

    private HashMap<ServiceReference, Object> deps = new HashMap<ServiceReference, Object>();

    @Override
    public <S> S getService(DependencyModel dependency, ServiceReference<S> reference, S service) {
        S proxy =  (S) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{dependency.getSpecification(), Enhanced.class}, new Interceptor(service));
        deps.put(reference, proxy);
        return proxy;
    }

    @Override
    public <S> void ungetService(DependencyModel dependency, ServiceReference<S> reference) {
        deps.remove(reference);
    }

    private class Interceptor implements InvocationHandler {

        private final Object service;

        public Interceptor(Object service) {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("enhance")) {
                return "yo!";
            }
            return method.invoke(service, args);
        }
    }
}
