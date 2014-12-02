/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.extender.internal.processor;

import org.apache.felix.ipojo.configuration.Instance;
import org.apache.felix.ipojo.extender.internal.BundleProcessor;
import org.apache.felix.ipojo.extender.internal.Extender;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultInstanceDeclaration;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultTypeDeclaration;
import org.apache.felix.ipojo.util.InvocationResult;
import org.apache.felix.ipojo.util.Log;
import org.objectweb.asm.ClassReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static org.apache.felix.ipojo.util.Reflection.fields;
import static org.apache.felix.ipojo.util.Reflection.methods;
import static org.apache.felix.ipojo.util.StreamUtils.closeQuietly;

/**
 * Processor looking for classes annotated with @Configuration and creating the corresponding instance declaration.
 */
public class ConfigurationProcessor implements BundleProcessor {

    /**
     * The logger.
     */
    private final Log m_logger;

    /**
     * Registry storing the bundle to components and instances declared within this bundle.
     * Only instances are expected.
     */
    private final Map<Bundle, ComponentsAndInstances> m_registry = new HashMap<Bundle, ComponentsAndInstances>();

    /**
     * Set to false to disable this processor.
     * When an OSGi framework does not provide the wiring API, this processor is disabled.
     */
    private final boolean m_enabled;

    /**
     * Creates the configuration processor.
     *
     * @param logger the logger.
     */
    public ConfigurationProcessor(Log logger) {

        m_logger = logger;

        // org.osgi.framework.wiring may not be available, in this case, disable us.
        try {
            this.getClass().getClassLoader().loadClass("org.osgi.framework.wiring.BundleWiring");
        } catch (ClassNotFoundException e) {
            m_logger.log(Log.ERROR, "The org.osgi.framework.wiring.BundleWiring class is not provided by the " +
                    "framework, configuration processor disabled.");
            m_enabled = false;
            return;
        }
        // Check ok.
        m_enabled = true;
    }

    public static String getClassNameFromResource(String resource) {
        String res = resource;
        if (resource.startsWith("/")) {
            res = resource.substring(1); // Remove the first /
        }
        res = res.substring(0, res.length() - ".class".length()); // Remove the .class
        return res.replace("/", ".");
    }

    /**
     * A bundle is starting.
     *
     * @param bundle the bundle
     */
    public void activate(Bundle bundle) {
        if (! m_enabled  && Extender.getIPOJOBundleContext().getBundle().getBundleId() == bundle.getBundleId()) {
            // Fast return if the configuration tracking is disabled, or if we are iPOJO
            return;
        }

        // It's not required to process bundle not importing the configuration package.
        final String imports = bundle.getHeaders().get(Constants.IMPORT_PACKAGE);
        if (imports == null  || ! imports.contains("org.apache.felix.ipojo.configuration")) {
            // TODO Check dynamic imports to verify if the package is not imported lazily.
            return;
        }

        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring == null) {
            // Invalid state.
            m_logger.log(Log.ERROR, "The bundle " + bundle.getBundleId() + " (" + bundle.getSymbolicName() + ") " +
                    "cannot be adapted to BundleWiring, state: " + bundle.getState());
            return;
        }

        // Only lookup for local classes, parent classes will be analyzed on demand.
        Collection<String> resources = wiring.listResources("/", "*.class",
                BundleWiring.FINDENTRIES_RECURSE + BundleWiring.LISTRESOURCES_LOCAL);
        if (resources == null) {
            m_logger.log(Log.ERROR, "The bundle " + bundle.getBundleId() + " (" + bundle.getSymbolicName() + ") " +
                    " does not have any classes to be analyzed");
            return;
        }
        m_logger.log(Log.DEBUG, resources.size() + " classes found");
        handleResources(bundle, resources, wiring.getClassLoader());
    }

    /**
     * A bundle is stopping.
     *
     * @param bundle the bundle
     */
    public void deactivate(Bundle bundle) {
        if (! m_enabled) { return; }

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

    private void handleResources(Bundle bundle, Collection<String> resources, ClassLoader classLoader) {
        for (String resource : resources) {
            handleResource(bundle, resource, classLoader);
        }
    }

    private void handleResource(Bundle bundle, String resource, ClassLoader classLoader) {
        URL url = classLoader.getResource(resource);
        if (url == null) {
            m_logger.log(Log.ERROR, "The resource " + resource + " cannot be loaded by " + bundle.getBundleId() + " " +
                    "(" + bundle.getSymbolicName() + ")");
            return;
        }

        try {
            if (hasConfigurationAnnotation(bundle, url, classLoader)) {
                instantiateAndDeclareInstances(bundle, resource, classLoader);
            }
        } catch (IOException e) {
            m_logger.log(Log.ERROR, "The resource " + resource + " cannot be loaded by " + bundle.getBundleId() + " " +
                    "(" + bundle.getSymbolicName() + ")", e);
        }

    }

    private void instantiateAndDeclareInstances(Bundle bundle, String resource, ClassLoader classLoader) {
        String classname = getClassNameFromResource(resource);
        List<Instance> instances = new ArrayList<Instance>();
        try {
            Class clazz = classLoader.loadClass(classname);
            Object configuration = clazz.newInstance();

            // Collect fields
            Map<Field, Instance> fields =
                    fields().ofType(Instance.class).in(configuration).map();
            for (Map.Entry<Field, Instance> entry : fields.entrySet()) {
                Instance instance = entry.getValue();
                instance.nameIfUnnamed(entry.getKey().getName())
                        .with("instance.bundle.context").setto(bundle.getBundleContext());
                instances.add(instance);
            }

            // Collect methods with Bundle Context as argument
            Map<Method, InvocationResult<Instance>> methods =
                    methods().in(configuration).ofReturnType(Instance.class).withParameter(BundleContext.class)
                            .map(bundle.getBundleContext());

            // Collect methods without arguments
            methods.putAll(methods().in(configuration).ofReturnType(Instance.class).map());

            for (Map.Entry<Method, InvocationResult<Instance>> entry : methods.entrySet()) {
                Instance instance = entry.getValue().get();
                if (instance == null) {
                    m_logger.log(Log.ERROR, "The Instance declaration creation failed because the method " + entry
                            .getKey().getName() + " of class " + entry.getKey().getDeclaringClass() + " threw an " +
                            "exception", entry.getValue().error());
                } else {
                    instance
                            .nameIfUnnamed(entry.getKey().getName())
                            .with("instance.bundle.context").setto(bundle.getBundleContext());
                    instances.add(instance);
                }
            }

        } catch (ClassNotFoundException e) {
            m_logger.log(Log.ERROR, "Cannot load class " + classname + " despite it was considered as a " +
                    "configuration", e);
            return;
        } catch (InstantiationException e) {
            m_logger.log(Log.ERROR, "Cannot instantiate class " + classname + " despite it was considered as a " +
                    "configuration", e);
            return;
        } catch (IllegalAccessException e) {
            m_logger.log(Log.ERROR, "Cannot instantiate class " + classname + " despite it was considered as a " +
                    "configuration", e);
            return;
        }

        m_logger.log(Log.WARNING, instances.size() + " instances found in class " + classname);
        //Build and enqueue declaration
        for (Instance instance : instances) {
            DefaultInstanceDeclaration declaration = new DefaultInstanceDeclaration(bundle.getBundleContext(),
                    instance.factory(), instance.configuration());
            declaration.start();
            getComponentsAndInstances(bundle).m_instances.add(declaration);
        }
    }

    private boolean hasConfigurationAnnotation(Bundle bundle, URL url, ClassLoader classLoader) throws IOException {
        InputStream is = url.openStream();

        try {
            // Exclude inner classes and classes containing $
            if (url.toExternalForm().contains("$")) {
                return false;
            }

            ClassReader reader = new ClassReader(is);
            ConfigurationAnnotationScanner scanner = new ConfigurationAnnotationScanner();
            reader.accept(scanner, 0);

            // Only class with the @Configuration are considered, parent classes are not analyzed.
            // Indeed, we have to detect when the parent must be considered independently,
            // or when only the daughter needs too (to avoid creating the instances twice).

            return scanner.isConfiguration();

            // The following code would traverse the whole class hierarchy.
//            if (scanner.isConfiguration()) {
//                return true;
//            } else if (scanner.getParent() != null) {
//                URL parentUrl = classLoader.getResource("/" + scanner.getParent().replace(".", "/") + ".class");
//                if (parentUrl == null) {
//                    m_logger.log(Log.DEBUG, "Cannot load the parent class " + scanner.getParent() + " - stopping " +
//                            "scan");
//                    return false;
//                }
//                return hasConfigurationAnnotation(bundle, parentUrl, classLoader);
//            }
        } finally {
            closeQuietly(is);
        }
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
