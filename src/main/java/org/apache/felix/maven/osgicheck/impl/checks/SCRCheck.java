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
package org.apache.felix.maven.osgicheck.impl.checks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.felix.maven.osgicheck.impl.Check;
import org.apache.felix.maven.osgicheck.impl.CheckContext;
import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentException;

/**
 * The following checks are performed
 * <ul>
 *   <li>Immediate flag
 *   <li>Unary references should be greedy
 *   <li>References ordering (Not finished yet)
 * </ul>
 */
public class SCRCheck implements Check {

    @Override
    public String getName() {
        return "scr";
    }

    @Override
    public void check(final CheckContext ctx) throws IOException, MojoExecutionException {
        ctx.getLog().info("Checking for SCR descriptor...");
        final String val = ctx.getManifest().getMainAttributes().getValue("Service-Component");
        if ( val != null ) {
            final String[] components = val.split(",");
            for(final String cmp : components) {
                final File xmlFile = new File(ctx.getRootDir(), cmp.trim().replace('/', File.separatorChar));
                if ( !xmlFile.exists() ) {
                    throw new MojoExecutionException("SCR descriptor file '" + cmp + "' not found in bundle");
                }
                final List<ComponentMetadata> mList = loadDescriptor(ctx, xmlFile);
                for(final ComponentMetadata md : mList) {
                    check(ctx, md);
                }
            }
        }
    }

    private void check(final CheckContext ctx, final ComponentMetadata md) {
        checkImmediate(ctx, md);
        checkGreedyReferences(ctx, md);
        checkMultiReferencesLast(ctx, md);
    }

    /**
     * Check the immediate flag
     * <ul>
     *   <li>If a component: it must not be declared (Error)
     *   <li>If a service: it should not be declared (Warning)
     * <ul>
     * @param ctx
     * @param md
     * @throws SecurityException
     * @throws NoSuchFieldException
     */
    private void checkImmediate(final CheckContext ctx, final ComponentMetadata md) {
        // we have to use reflection
        Boolean immediate = null;
        try {
            final Field immediateField = md.getClass().getDeclaredField("m_immediate");
            immediateField.setAccessible(true);
            immediate = (Boolean) immediateField.get(md);
        } catch ( final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if ( md.getServiceMetadata() == null ) {
            if ( immediate != null ) {
                if ( immediate.booleanValue() ) {
                    ctx.reportError("Component " + md.getName() + " must not be declared as 'immediate'. It's a component. Remove the attribute.");
                } else {
                    ctx.reportError("Component " + md.getName() + " must not be declared as not 'immediate'.It's a component. Remove the attribute.");
                }
            }
        } else {
            if ( immediate != null ) {
                if ( immediate.booleanValue() ) {
                    ctx.reportWarning("Service " + md.getName() + " should not be declared as 'immediate'.");
                } else {
                    ctx.reportWarning("Service " + md.getName() + " should not declare 'immediate' attribute but rather use the default.");
                }
            }
        }
    }

    /**
     * Unary references should be greedy
     * @param ctx
     * @param md
     */
    private void checkGreedyReferences(final CheckContext ctx, final ComponentMetadata md) {
        for(final ReferenceMetadata rmd : md.getDependencies()) {
            if ( !rmd.isMultiple() ) {
                if ( rmd.isReluctant() ) {
                    ctx.reportWarning("Component " + md.getName() + " should use greedy for reference " + rmd.getName());
                }
            }
        }
    }

    /**
     * Check that unary references are before multi cardinality ones.
     * Maybe we could improve it by checking if all static references are before all dynamic ones
     * and within each block, single before multi
     * @param ctx
     * @param md
     */
    private void checkMultiReferencesLast(final CheckContext ctx, final ComponentMetadata md) {
        boolean reachedMulti = false;
        for(final ReferenceMetadata rmd : md.getDependencies()) {
            if ( !rmd.isMultiple() ) {
                if ( reachedMulti ) {
                    ctx.reportWarning("Component " + md.getName() + " should put the following reference before the references with multiple cardinality " + rmd.getName());
                }
            } else {
                reachedMulti = true;
            }
        }
    }

    private List<ComponentMetadata> loadDescriptor(final CheckContext ctx, final File file) throws IOException, MojoExecutionException {
        try(final Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            XmlHandler handler = new XmlHandler( new Bundle() {

                @Override
                public int compareTo(Bundle o) {
                    return 0;
                }

                @Override
                public void update(InputStream input) throws BundleException {
                    // nothing to do
                }

                @Override
                public void update() throws BundleException {
                    // nothing to do
                }

                @Override
                public void uninstall() throws BundleException {
                    // nothing to do
                }

                @Override
                public void stop(int options) throws BundleException {
                    // nothing to do
                }

                @Override
                public void stop() throws BundleException {
                    // nothing to do
                }

                @Override
                public void start(int options) throws BundleException {
                    // nothing to do
                }

                @Override
                public void start() throws BundleException {
                    // nothing to do
                }

                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    return null;
                }

                @Override
                public boolean hasPermission(Object permission) {
                    return false;
                }

                @Override
                public Version getVersion() {
                    return null;
                }

                @Override
                public String getSymbolicName() {
                    return null;
                }

                @Override
                public int getState() {
                    return 0;
                }

                @Override
                public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
                    return null;
                }

                @Override
                public ServiceReference<?>[] getServicesInUse() {
                    return null;
                }

                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    return null;
                }

                @Override
                public URL getResource(String name) {
                    return null;
                }

                @Override
                public ServiceReference<?>[] getRegisteredServices() {
                    return null;
                }

                @Override
                public String getLocation() {
                    return file.getAbsolutePath();
                }

                @Override
                public long getLastModified() {
                    return 0;
                }

                @Override
                public Dictionary<String, String> getHeaders(String locale) {
                    return null;
                }

                @Override
                public Dictionary<String, String> getHeaders() {
                    return null;
                }

                @Override
                public Enumeration<String> getEntryPaths(String path) {
                    return null;
                }

                @Override
                public URL getEntry(final String path) {
                    try {
                        return new File(ctx.getRootDir(), path).toURI().toURL();
                    } catch (final MalformedURLException e) {
                        return null;
                    }
                }

                @Override
                public File getDataFile(String filename) {
                    return null;
                }

                @Override
                public long getBundleId() {
                    return 0;
                }

                @Override
                public BundleContext getBundleContext() {
                    return null;
                }

                @Override
                public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
                    return null;
                }

                @Override
                public <A> A adapt(Class<A> type) {
                    return null;
                }
            }, new Logger() {

                @Override
                public void log(int level, String pattern, Object[] arguments, ComponentMetadata metadata, Long componentId,
                        Throwable ex) {
                    // nothing to do
                }

                @Override
                public void log(int level, String message, ComponentMetadata metadata, Long componentId, Throwable ex) {
                    // nothing to do
                }

                @Override
                public boolean isLogEnabled(int level) {
                    return false;
                }
            }, false, false );
            try {
                new KXml2SAXParser( in ).parseXML(handler);

                for ( final ComponentMetadata metadata : handler.getComponentMetadataList() ) {
                    try {
                        // validate the component metadata
                        metadata.validate( null ); // logger argument is not used and removed in R7


                    } catch ( final ComponentException t ) {
                        ctx.reportError("Invalid component descriptor " + file.getAbsolutePath() + " for " + metadata.getName() + " : "
                                 + t.getMessage());
                    }
                }

                return handler.getComponentMetadataList();
            } catch ( final Exception e) {
                if ( e instanceof MojoExecutionException ) {
                    throw (MojoExecutionException)e;
                }
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }
}
