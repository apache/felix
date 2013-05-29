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

import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.apache.felix.ipojo.extender.internal.builder.ReflectiveFactoryBuilder;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultExtensionDeclaration;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Log;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.*;

/**
 * Bundle processor handling the {@link #IPOJO_EXTENSION} header.
 */
public class ExtensionBundleProcessor implements BundleProcessor {

    /**
     * iPOJO Extension declaration header.
     */
    public static final String IPOJO_EXTENSION = "IPOJO-Extension";

    /**
     * Logger.
     */
    private final Log m_logger;

    /**
     * The map storing the association between bundles and the list of extension declaration.
     */
    private Map<Bundle, List<DefaultExtensionDeclaration>> m_extensions = new HashMap<Bundle, List<DefaultExtensionDeclaration>>();

    /**
     * Creates the processor.
     *
     * @param logger the logger
     */
    public ExtensionBundleProcessor(Log logger) {
        m_logger = logger;
    }

    /**
     * A bundle is starting.
     *
     * @param bundle the bundle
     */
    public void activate(Bundle bundle) {
        Dictionary dict = bundle.getHeaders();
        // Check for abstract factory type
        String extension = (String) dict.get(IPOJO_EXTENSION);
        if (extension != null) {
            activateExtensions(bundle, extension);
        }
    }

    /**
     * A bundle is stopping.
     *
     * @param bundle the bundle
     */
    public void deactivate(Bundle bundle) {
        List<DefaultExtensionDeclaration> declarations = m_extensions.get(bundle);
        if (declarations != null) {
            for (DefaultExtensionDeclaration declaration : declarations) {
                declaration.stop();
            }
            m_extensions.remove(bundle);
        }
    }

    /**
     * iPOJO is starting.
     * Nothing to do.
     */
    public void start() {
        // Nothing to do
    }

    /**
     * iPOJO is stopping.
     * We clean up all extension in the reverse order of their installation.
     */
    public void stop() {
        // Construct a new instance to avoid ConcurrentModificationException since deactivate also change the extensions
        // list
        // Ignored, for a simple ordered shutdown, use ReverseBundleProcessor
    }

    /**
     * Parses an IPOJO-Extension manifest header and then creates
     * iPOJO extensions (factory types).
     *
     * @param bundle the bundle containing the header.
     * @param header the header to parse.
     */
    private void activateExtensions(Bundle bundle, String header) {
        String[] extensions = ParseUtils.split(header, ",");
        for (int i = 0; extensions != null && i < extensions.length; i++) {
            String[] segments = ParseUtils.split(extensions[i], ":");

            /*
             * Get the fully qualified type name.
             * type = [namespace] name
             */
            String[] nameparts = ParseUtils.split(segments[0].trim(), " \t");
            String type = nameparts.length == 1 ? nameparts[0] : nameparts[0] + ":" + nameparts[1];
            String classname = segments[1];

            Class<? extends IPojoFactory> clazz;
            try {
                clazz = bundle.loadClass(classname).asSubclass(IPojoFactory.class);
            } catch (ClassNotFoundException e) {
                String message = String.format("Cannot load class '%s' from bundle %s (%s) for extension '%s'",
                                               classname,
                                               bundle.getSymbolicName(),
                                               bundle.getVersion(),
                                               type);
                m_logger.log(Logger.ERROR, message, e);
                return;
            }

            try {
                ReflectiveFactoryBuilder builder = new ReflectiveFactoryBuilder(clazz.getConstructor(BundleContext.class, Element.class));
                DefaultExtensionDeclaration declaration = new DefaultExtensionDeclaration(bundle.getBundleContext(), builder, type);

                getBundleDeclarations(bundle).add(declaration);

                declaration.start();

                m_logger.log(Logger.DEBUG, "New factory type available: " + type);
            } catch (NoSuchMethodException e) {
                m_logger.log(Logger.ERROR,
                        String.format("Extension '%s' is missing the required (BundleContext, Element) public " +
                                "constructor", clazz.getName()));
            }
        }
    }

    /**
     * Gets the list of declaration for the given method.
     *
     * @param bundle the bundle
     * @return the list of extension declaration associated to the given bundle, <code>null</code> otherwise.
     */
    private List<DefaultExtensionDeclaration> getBundleDeclarations(Bundle bundle) {
        List<DefaultExtensionDeclaration> declarations = m_extensions.get(bundle);
        if (declarations == null) {
            declarations = new ArrayList<DefaultExtensionDeclaration>();
            m_extensions.put(bundle, declarations);
        }
        return declarations;
    }

}
