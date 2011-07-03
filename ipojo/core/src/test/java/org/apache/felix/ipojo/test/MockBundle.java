package org.apache.felix.ipojo.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class MockBundle implements Bundle {

    private final ClassLoader m_classloader;

    /**
     * @param classloader the Classloader to load classes and resources.
     */
    public MockBundle(ClassLoader cl) {
        m_classloader = cl;
    }

    public int getState() {
        return 0;
    }

    public void start() throws BundleException {
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
}