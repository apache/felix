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
package org.apache.felix.ipojo.handlers.context;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.ConstructorInjector;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Log;
import org.osgi.framework.BundleContext;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

/**
 * A handler injecting the bundle context in the implementation code.
 *
 * @since 1.11.2
 */
public class BundleContextHandler extends PrimitiveHandler {

    private List<BundleCallback> m_methods = new ArrayList<BundleCallback>();

    private BundleContext getComponentBundleContext() {
        return getFactory().getBundleContext();
    }

    private BundleContext getInstanceBundleContext() {
        return getInstanceManager().getInstanceContext();
    }

    /**
     * Configures the handler.
     * This method collects all `context` element.
     *
     * @param metadata      the metadata of the component
     * @param configuration the instance configuration
     * @throws org.apache.felix.ipojo.ConfigurationException if the metadata are not correct.
     */
    @Override
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        Element[] contexts = metadata.getElements("context");
        for (Element element : contexts) {
            BundleContext bc = getBundleContextForConfiguration(element);

            if (element.containsAttribute("constructor-parameter")) {
                String idx = element.getAttribute("constructor-parameter");
                int index = Integer.parseInt(idx);
                final BundleContext injected = bc;
                getLogger().log(Log.DEBUG, "Registering bundle context injection for index " + index + " for instance" +
                        " " + getInstanceManager().getInstanceName());
                getInstanceManager().register(index, new ConstructorInjector() {

                    public Object getConstructorParameter(int index) {
                        return injected;
                    }

                    public Class getConstructorParameterType(int index) {
                        return BundleContext.class;
                    }
                });
            } else if (element.containsAttribute("field")) {
                String field = element.getAttribute("field");
                final BundleContext injected = bc;
                FieldMetadata fm = getFactory().getPojoMetadata().getField(field);
                if (fm == null) {
                    throw new ConfigurationException("Cannot inject the bundle context in the field " + field + " - " +
                            "reason: the field does not exist in " + getInstanceManager().getClassName());
                }
                if (!BundleContext.class.getName().equals(fm.getFieldType())) {
                    throw new ConfigurationException("Cannot inject the bundle context in the field " + field + " - " +
                            "reason: the field " + field + " from " + getInstanceManager().getClassName() + " is not " +
                            "from the BundleContext type");
                }
                getInstanceManager().register(fm, new FieldInterceptor() {
                    public void onSet(Object pojo, String fieldName, Object value) {
                        // Do nothing.
                    }

                    public Object onGet(Object pojo, String fieldName, Object value) {
                        return injected;
                    }
                });
            } else if (element.containsAttribute("method")) {
                String method = element.getAttribute("method");
                MethodMetadata mm = getFactory().getPojoMetadata().getMethod(method,
                        new String[]{BundleContext.class.getName()});
                if (mm == null) {
                    getLogger().log(Log.WARNING, "Cannot find the method " + method + " in the class " +
                            getInstanceManager().getClassName() + ", super classes lookup will be attempted");
                }
                Callback callback = new Callback(method, new Class[]{BundleContext.class}, false,
                        getInstanceManager());
                m_methods.add(new BundleCallback(callback, bc));
            }


        }
    }

    private BundleContext getBundleContextForConfiguration(Element element) throws ConfigurationException {
        String type = element.getAttribute("value");
        if (type == null) {
            // XML case.
            type = element.getAttribute("context");
        }
        BundleContext context;
        if ("INSTANCE".equalsIgnoreCase(type)) {
            context = getInstanceBundleContext();
        } else if (type == null || "COMPONENT".equalsIgnoreCase(type)) {
            context = getComponentBundleContext();
        } else {
            throw new ConfigurationException("Not supported bundle context source : " + type);
        }
        return context;
    }

    /**
     * Stops the handler
     * This method stops the management.
     */
    @Override
    public void stop() {
        // Nothing to do.
    }

    /**
     * Starts the handler
     * This method starts the management.
     */
    @Override
    public void start() {
        // Nothing to do.
    }

    /**
     * Callback method called when an instance of the component is created, but
     * before someone can use it.
     * Injects the bundle context in all declared bundle callbacks.
     *
     * @param instance the created instance
     */
    @Override
    public void onCreation(Object instance) {
        for (BundleCallback callback : m_methods) {
            try {
                callback.invoke(instance);
            } catch (Throwable e) {
                error("Cannot inject the bundle context in the method " + callback.callback.getMethod() + " - reason:" +
                        " " + e.getMessage(), e);
            }
        }
    }

    private class BundleCallback {
        private final Callback callback;
        private final BundleContext context;

        public BundleCallback(Callback callback, BundleContext injected) {
            this.callback = callback;
            this.context = injected;
        }

        public void invoke(Object target) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            if (context != null) {
                callback.call(target, new Object[]{context});
            }
        }
    }
}
