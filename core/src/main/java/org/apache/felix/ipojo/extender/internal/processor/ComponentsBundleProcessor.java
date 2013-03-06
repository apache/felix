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

package org.apache.felix.ipojo.extender.internal.processor;

import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultInstanceDeclaration;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultTypeDeclaration;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.util.Log;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.util.*;

/**
 * Processor handling the {@link #IPOJO_HEADER} and {@link #IPOJO_HEADER_ALT}
 * header from the bundle manifest.
 */
public class ComponentsBundleProcessor implements BundleProcessor {

    /**
     * iPOJO Component Type and Instance declaration header.
     */
    public static final String IPOJO_HEADER = "iPOJO-Components";

    /**
     * iPOJO Component Type and Instance declaration header
     * (alternative).
     * This header was introduced because of BND supporting only header
     * starting with an uppercase.
     */
    public static final String IPOJO_HEADER_ALT = "IPOJO-Components";

    /**
     * The attribute used in instance configuration specifying the targeted component (i.e. factory).
     */
    public static final String COMPONENT_INSTANCE_ATTRIBUTE = "component";

    /**
     * The logger.
     */
    private final Log m_logger;

    /**
     * Registry storing the bundle to components and instances declared within this bundle.
     */
    private final Map<Bundle, ComponentsAndInstances> m_registry = new HashMap<Bundle, ComponentsAndInstances>();

    /**
     * Creates the component bundle processor.
     *
     * @param logger the logger.
     */
    public ComponentsBundleProcessor(Log logger) {
        m_logger = logger;
    }

    /**
     * A bundle is starting.
     *
     * @param bundle the bundle
     */
    public void activate(Bundle bundle) {
        Dictionary dict = bundle.getHeaders();
        // Check bundle
        String header = (String) dict.get(IPOJO_HEADER);
        // Check the alternative header
        if (header == null) {
            header = (String) dict.get(IPOJO_HEADER_ALT);
        }

        if (header != null) {
            try {
                parse(bundle, header);
            } catch (IOException e) {
                m_logger.log(Logger.ERROR, "An exception occurs during the parsing of the bundle " + bundle.getBundleId(), e);
            } catch (ParseException e) {
                m_logger.log(Logger.ERROR, "A parse exception occurs during the parsing of the bundle " + bundle.getBundleId(), e);
            }
        }

    }

    /**
     * A bundle is stopping.
     *
     * @param bundle the bundle
     */
    public void deactivate(Bundle bundle) {
        ComponentsAndInstances cai = m_registry.remove(bundle);
        if (cai != null) {
            cai.stop();
        }
    }

    /**
     * {@inheritDoc BundleProcessor#start}
     */
    public void start() {
        // Nothing to do
    }

    /**
     * {@inheritDoc BundleProcessor#stop}
     * <p/>
     * This method cleans up all created factories and instances.
     */
    public void stop() {
        // Ignored, for a simple ordered shutdown, use ReverseBundleProcessor
    }

    /**
     * Parses the internal metadata (from the manifest
     * (in the iPOJO-Components property)). This methods
     * creates factories and add instances to the instance creator.
     *
     * @param bundle     the owner bundle.
     * @param components The iPOJO Header String.
     * @throws IOException    if the manifest can not be found
     * @throws ParseException if the parsing process failed
     */
    private void parse(Bundle bundle, String components) throws IOException, ParseException {
        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.parseHeader(components);

        // Get the component type declaration
        Element[] metadata = parser.getComponentsMetadata();
        for (int i = 0; i < metadata.length; i++) {
            handleTypeDeclaration(bundle, metadata[i]);
        }

        Dictionary[] instances = parser.getInstances();
        for (int i = 0; instances != null && i < instances.length; i++) {
            handleInstanceDeclaration(bundle, instances[i]);
        }
    }

    /**
     * Extracts and builds the declaration attached to an instance.
     *
     * @param bundle   the bundle declaring the instance
     * @param instance the instance configuration (parsed from the header)
     */
    private void handleInstanceDeclaration(Bundle bundle, Dictionary instance) {

        String component = (String) instance.get(COMPONENT_INSTANCE_ATTRIBUTE);
        //String v = (String) instance.get(Factory.FACTORY_VERSION_PROPERTY); //TODO CES to GSA, why this is commented ?

        DefaultInstanceDeclaration declaration = new DefaultInstanceDeclaration(bundle.getBundleContext(),
                component, instance);
        declaration.start();

        getComponentsAndInstances(bundle).m_instances.add(declaration);

    }

    /**
     * Adds a component factory to the factory list.
     *
     * @param metadata the new component metadata.
     * @param bundle   the bundle.
     */
    private void handleTypeDeclaration(Bundle bundle, Element metadata) {

        DefaultTypeDeclaration declaration = new DefaultTypeDeclaration(bundle.getBundleContext(), metadata);
        declaration.start();

        getComponentsAndInstances(bundle).m_types.add(declaration);

    }

    /**
     * Gets the {@link ComponentsAndInstances} declared by the given bundle.
     *
     * @param bundle the bundle
     * @return the set of component and instances declared by the bundle, <code>null</code> otherwise
     */
    private ComponentsAndInstances getComponentsAndInstances(Bundle bundle) {
        ComponentsAndInstances cai = m_registry.get(bundle);
        if (cai == null) {
            cai = new ComponentsAndInstances();
            m_registry.put(bundle, cai);
        }
        return cai;
    }

    /**
     * Container storing the components and instances declared by a bundle.
     * This class is not intended to be used outside from the current processor.
     */
    private static class ComponentsAndInstances {
        List<DefaultTypeDeclaration> m_types = new ArrayList<DefaultTypeDeclaration>();
        List<DefaultInstanceDeclaration> m_instances = new ArrayList<DefaultInstanceDeclaration>();

        /**
         * Stops all declarations.
         */
        void stop() {
            for (DefaultInstanceDeclaration instance : m_instances) {
                instance.stop();
            }
            for (DefaultTypeDeclaration declaration : m_types) {
                declaration.stop();
            }
            m_instances.clear();
            m_types.clear();
        }
    }


}
