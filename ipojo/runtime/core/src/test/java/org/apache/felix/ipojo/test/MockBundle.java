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
package org.apache.felix.ipojo.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.osgi.framework.*;

public class MockBundle implements Bundle {

    private final ClassLoader m_classloader;

    /**
     * @param cl the Classloader to load classes and resources.
     */
    public MockBundle(ClassLoader cl) {
        m_classloader = cl;
    }

    public int getState() {
        return 0;
    }

    public void start(int i) throws BundleException {
    }

    public void start() throws BundleException {
    }

    public void stop(int i) throws BundleException {
    }

    public void stop() throws BundleException {
    }

    public void update() throws BundleException {
    }

    public void update(InputStream in) throws BundleException {
    }

    public void uninstall() throws BundleException {
    }

    public Dictionary getHeaders() {
        return null;
    }

    public long getBundleId() {
        return 1; // 0 is the system bundle
    }

    public String getLocation() {
        return null;
    }

    public ServiceReference[] getRegisteredServices() {
        return null;
    }

    public ServiceReference[] getServicesInUse() {
        return null;
    }

    public boolean hasPermission(Object permission) {
        return false;
    }

    public URL getResource(String name) {
        return m_classloader.getResource(name);
    }

    public Dictionary getHeaders(String locale) {
        return null;
    }

    public String getSymbolicName() {
        return null;
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        return m_classloader.loadClass(name);
    }

    public Enumeration getResources(String name) throws IOException {
        return m_classloader.getResources(name);
    }

    public Enumeration getEntryPaths(String path) {
        return null;
    }

    public URL getEntry(String name) {
        return null;
    }

    public long getLastModified() {
        return 0;
    }

    public Enumeration findEntries(String path, String filePattern,
            boolean recurse) {
        return null;
    }

    public BundleContext getBundleContext() {
        return null;
    }

    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int i) {
        return null;
    }

    public Version getVersion() {
        return null;
    }

    public <A> A adapt(Class<A> aClass) {
        return null;
    }

    public File getDataFile(String s) {
        return null;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p/>
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     * <p/>
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     * <p/>
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     * <p/>
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     * <p/>
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this object.
     */
    public int compareTo(Bundle o) {
        return 0;
    }
}