/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.maven.osgicheck.impl.mddocgen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

final class SyntheticBundle implements Bundle {

    private final File servicesDirectory;

    private final File bundleDirectory;

    public SyntheticBundle(File servicesDirectory, File serviceFile) {
        this.servicesDirectory = servicesDirectory;
        bundleDirectory = serviceFile.getParentFile().getParentFile();
    }

    @Override
    public int compareTo(Bundle o) {
        // not needed
        return 0;
    }

    @Override
    public int getState() {
        // not needed
        return 0;
    }

    @Override
    public void start(int options) throws BundleException {
        // not needed

    }

    @Override
    public void start() throws BundleException {
        // not needed

    }

    @Override
    public void stop(int options) throws BundleException {
        // not needed

    }

    @Override
    public void stop() throws BundleException {
        // not needed

    }

    @Override
    public void update(InputStream input) throws BundleException {
        // not needed

    }

    @Override
    public void update() throws BundleException {
        // not needed

    }

    @Override
    public void uninstall() throws BundleException {
        // not needed

    }

    @Override
    public Dictionary<String, String> getHeaders() {
        // not needed
        return null;
    }

    @Override
    public long getBundleId() {
        // not needed
        return 0;
    }

    @Override
    public String getLocation() {
        return servicesDirectory.toURI().relativize(servicesDirectory.toURI()).getPath();
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        // not needed
        return null;
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        // not needed
        return null;
    }

    @Override
    public boolean hasPermission(Object permission) {
        // not needed
        return false;
    }

    @Override
    public URL getResource(String name) {
        // not needed
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        // not needed
        return null;
    }

    @Override
    public String getSymbolicName() {
        // not needed
        return null;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // not needed
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // not needed
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        // not needed
        return null;
    }

    @Override
    public URL getEntry(String path) {
        File entry = new File(bundleDirectory, path);
        try {
            return entry.toURI().toURL();
        } catch (MalformedURLException e) {
            // should not happen
            throw new RuntimeException("Impossible to create an URL for file " + entry);
        }
    }

    @Override
    public long getLastModified() {
        // not needed
        return 0;
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        // not needed
        return null;
    }

    @Override
    public BundleContext getBundleContext() {
        // not needed
        return null;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
            int signersType) {
        // not needed
        return null;
    }

    @Override
    public Version getVersion() {
        // not needed
        return null;
    }

    @Override
    public <A> A adapt(Class<A> type) {
        // not needed
        return null;
    }

    @Override
    public File getDataFile(String filename) {
        // not needed
        return null;
    }

}
