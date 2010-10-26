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
package org.apache.felix.utils.properties;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class MockBundleContext implements BundleContext {
    private Properties properties = new Properties();

    public void setProperty(String name, String value) {
        this.properties.setProperty(name, value);
    }
    public String getProperty(String name) {
        String value = this.properties.getProperty(name);
        if (value == null) {
            value = System.getProperty(name);
        }
        return value;
    }

    public Bundle getBundle() {
        throw new UnsupportedOperationException();
    }

    public Bundle installBundle(String s) throws BundleException {
        throw new UnsupportedOperationException();
    }

    public Bundle installBundle(String s, InputStream stream) throws BundleException {
        throw new UnsupportedOperationException();
    }

    public Bundle getBundle(long l) {
        throw new UnsupportedOperationException();
    }

    public Bundle[] getBundles() {
        throw new UnsupportedOperationException();
    }

    public void addServiceListener(ServiceListener listener, String s) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    public void addServiceListener(ServiceListener listener) {
        throw new UnsupportedOperationException();
    }

    public void removeServiceListener(ServiceListener listener) {
        throw new UnsupportedOperationException();
    }

    public void addBundleListener(BundleListener listener) {
        throw new UnsupportedOperationException();
    }

    public void removeBundleListener(BundleListener listener) {
        throw new UnsupportedOperationException();
    }

    public void addFrameworkListener(FrameworkListener listener) {
        throw new UnsupportedOperationException();
    }

    public void removeFrameworkListener(FrameworkListener listener) {
        throw new UnsupportedOperationException();
    }

    public ServiceRegistration registerService(String[] strings, Object o, Dictionary dictionary) {
        throw new UnsupportedOperationException();
    }

    public ServiceRegistration registerService(String s, Object o, Dictionary dictionary) {
        throw new UnsupportedOperationException();
    }

    public ServiceReference[] getServiceReferences(String s, String s1) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    public ServiceReference[] getAllServiceReferences(String s, String s1) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    public ServiceReference getServiceReference(String s) {
        throw new UnsupportedOperationException();
    }

    public Object getService(ServiceReference reference) {
        throw new UnsupportedOperationException();
    }

    public boolean ungetService(ServiceReference reference) {
        throw new UnsupportedOperationException();
    }

    public File getDataFile(String s) {
        throw new UnsupportedOperationException();
    }

    public Filter createFilter(String s) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }
}
