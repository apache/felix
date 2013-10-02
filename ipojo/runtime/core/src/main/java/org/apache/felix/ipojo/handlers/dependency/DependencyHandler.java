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

package org.apache.felix.ipojo.handlers.dependency;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import java.util.*;

/**
 * The dependency handler manages a list of service dependencies.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandler extends PrimitiveHandler implements DependencyStateListener {

    /**
     * Proxy settings property.
     */
    public static final String PROXY_SETTINGS_PROPERTY = "ipojo.proxy";
    /**
     * Proxy type property.
     */
    public static final String PROXY_TYPE_PROPERTY = "ipojo.proxy.type";
    /**
     * Proxy type value: smart.
     */
    public static final String SMART_PROXY = "smart";
    /**
     * Proxy type value: dynamic-proxy.
     */
    public static final String DYNAMIC_PROXY = "dynamic-proxy";
    /**
     * Proxy settings value: enabled.
     */
    public static final String PROXY_ENABLED = "enabled";
    /**
     * Proxy settings value: disabled.
     */
    public static final String PROXY_DISABLED = "disabled";
    /**
     * List of dependencies of the component.
     */
    private final List<Dependency> m_dependencies = new ArrayList<Dependency>();
    /**
     * Is the handler started.
     */
    private boolean m_started;
    /**
     * The handler description.
     */
    private DependencyHandlerDescription m_description;
    /**
     * The instance configuration context source, updated once reconfiguration.
     */
    private InstanceConfigurationSource m_instanceConfigurationSource;

    /**
     * Builds a description of this dependency to help the user to identify it. IT's not related to the Dependency
     * Description, it's just a string containing dependency information to spot it easily in the code.
     *
     * @param dep the dependency
     * @return the identifier containing (if defined) the id, the specification, the field and the callback.
     * @since 1.10.1
     */
    public static String getDependencyIdentifier(Dependency dep) {
        StringBuilder identifier = new StringBuilder("{");
        if (dep.getId() != null) {
            identifier.append("id=").append(dep.getId());
        }
        if (dep.getField() != null) {
            if (identifier.length() > 1) {
                identifier.append(", ");
            }
            identifier.append("field=").append(dep.getField());
        }
        if (dep.getCallbacks() != null && dep.getCallbacks().length > 0) {
            if (identifier.length() > 1) {
                identifier.append(", ");
            }
            identifier.append("method=").append(dep.getCallbacks()[0].getMethodName());
        }
        if (dep.getSpecification() != null) {
            if (identifier.length() > 1) {
                identifier.append(", ");
            }
            identifier.append("specification=").append(dep.getSpecification().getName());
        }
        identifier.append("}");
        return identifier.toString();
    }

    /**
     * Get the list of managed dependency.
     *
     * @return the dependency list
     */
    public Dependency[] getDependencies() {
        return m_dependencies.toArray(new Dependency[m_dependencies.size()]);
    }

    /**
     * Validate method. This method is invoked by an AbstractServiceDependency when this dependency becomes RESOLVED.
     *
     * @param dep : the dependency becoming RESOLVED.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#validate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void validate(DependencyModel dep) {
        checkContext();
    }

    /**
     * Invalidate method. This method is invoked by an AbstractServiceDependency when this dependency becomes UNRESOLVED or BROKEN.
     *
     * @param dep : the dependency becoming UNRESOLVED or BROKEN.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#invalidate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void invalidate(DependencyModel dep) {
        setValidity(false);
    }

    /**
     * Check the validity of the dependencies.
     */
    protected void checkContext() {
        if (!m_started) {
            return;
        }
        synchronized (m_dependencies) {
            // Store the initial state
            boolean initialState = getValidity();

            boolean valid = true;
            for (Dependency dep : m_dependencies) {
                if (dep.getState() != Dependency.RESOLVED) {
                    valid = false;
                    break;
                }
            }

            // Check the component dependencies
            if (valid) {
                // The dependencies are valid
                if (!initialState) {
                    // There is a state change
                    setValidity(true);
                }
                // Else do nothing, the component state stay VALID
            } else {
                // The dependencies are not valid
                if (initialState) {
                    // There is a state change
                    setValidity(false);
                }
                // Else do nothing, the component state stay UNRESOLVED
            }

        }
    }

    /**
     * Configure the handler.
     *
     * @param componentMetadata : the component type metadata
     * @param configuration     : the instance configuration
     * @throws ConfigurationException : one dependency metadata is not correct.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        PojoMetadata manipulation = getFactory().getPojoMetadata();
        boolean atLeastOneField = false;

        // Create the dependency according to the component metadata
        Element[] deps = componentMetadata.getElements("Requires");

        // Get instance filters.
        Dictionary filtersConfiguration = getRequiresFilters(configuration.get("requires.filters"));
        Dictionary fromConfiguration = (Dictionary) configuration.get("requires.from");

        for (int i = 0; deps != null && i < deps.length; i++) {
            // Create the dependency metadata
            final Element dependencyElement = deps[i];

            String field = dependencyElement.getAttribute("field");
            String serviceSpecification = getServiceSpecificationAttribute(dependencyElement);
            String opt = dependencyElement.getAttribute("optional");
            boolean optional = opt != null && opt.equalsIgnoreCase("true");
            String defaultImpl = dependencyElement.getAttribute("default-implementation");
            String exception = dependencyElement.getAttribute("exception");
            String to = dependencyElement.getAttribute("timeout");
            int timeout = 0;
            if (to != null) {
                timeout = Integer.parseInt(to);
            }

            String agg = dependencyElement.getAttribute("aggregate");
            boolean aggregate = agg != null && agg.equalsIgnoreCase("true");

            String identity = dependencyElement.getAttribute("id");

            String nul = dependencyElement.getAttribute("nullable");
            boolean nullable = nul == null || nul.equalsIgnoreCase("true");

            boolean isProxy = isProxy(dependencyElement);

            BundleContext context = getFacetedBundleContext(dependencyElement);

            String filter = computeFilter(dependencyElement, filtersConfiguration, fromConfiguration, aggregate, identity);
            Filter fil = createAndCheckFilter(filter);

            Class spec = null;
            if (serviceSpecification != null) {
                spec = DependencyMetadataHelper.loadSpecification(serviceSpecification, getInstanceManager().getContext());
            }

            int policy = DependencyMetadataHelper.getPolicy(dependencyElement);
            Comparator cmp = DependencyMetadataHelper.getComparator(dependencyElement, getInstanceManager().getGlobalContext());

            Dependency dep = new Dependency(this, field, spec, fil, optional, aggregate, nullable, isProxy, identity,
                    context, policy, cmp, defaultImpl, exception);
            dep.setTimeout(timeout);

            // Look for dependency callback :
            addCallbacksToDependency(dependencyElement, dep);

            // Add the constructor parameter if needed
            String paramIndex = dependencyElement.getAttribute("constructor-parameter");
            if (paramIndex != null) {
                int index = Integer.parseInt(paramIndex);
                dep.addConstructorInjection(index);
            }

            // Check the dependency, throws an exception on error.
            DependencyConfigurationChecker.ensure(dep, dependencyElement, manipulation);
            m_dependencies.add(dep);
            if (dep.getField() != null) {
                getInstanceManager().register(manipulation.getField(dep.getField()), dep);
                atLeastOneField = true;
            }

        }

        if (atLeastOneField) { // Does register only if we have fields
            MethodMetadata[] methods = manipulation.getMethods();
            for (MethodMetadata method : methods) {
                for (Dependency dep : m_dependencies) {
                    getInstanceManager().register(method, dep);
                }
            }

            // Also track the inner class methods
            for (String inner : manipulation.getInnerClasses()) {
                MethodMetadata[] meths = manipulation.getMethodsFromInnerClass(inner);
                if (meths != null) {
                    for (MethodMetadata method : meths) {
                        for (Dependency dep : m_dependencies) {
                            getInstanceManager().register(method, inner, dep);
                        }
                    }
                }
            }
        }

        m_description = new DependencyHandlerDescription(this, getDependencies()); // Initialize the description.

        manageContextSources(configuration);
    }

    /**
     * Add internal context source to all dependencies.
     *
     * @param configuration the instance configuration to creates the instance configuration source
     */
    private void manageContextSources(Dictionary<String, Object> configuration) {
        m_instanceConfigurationSource = new InstanceConfigurationSource(configuration);
        SystemPropertiesSource systemPropertiesSource = new SystemPropertiesSource();

        for (Dependency dependency : m_dependencies) {
            if (dependency.getFilter() != null) {
                dependency.getContextSourceManager().addContextSource(m_instanceConfigurationSource);
                dependency.getContextSourceManager().addContextSource(systemPropertiesSource);

                for (Handler handler : getInstanceManager().getRegisteredHandlers()) {
                    if (handler instanceof ContextSource) {
                        dependency.getContextSourceManager().addContextSource((ContextSource) handler);
                    }
                }
            }
        }
    }

    private String computeFilter(Element dependencyElement, Dictionary filtersConfiguration, Dictionary fromConfiguration, boolean aggregate, String identity) {
        String filter = dependencyElement.getAttribute("filter");
        // Get instance filter if available
        if (filtersConfiguration != null && identity != null && filtersConfiguration.get(identity) != null) {
            filter = (String) filtersConfiguration.get(identity);
        }

        // Compute the 'from' attribute
        filter = updateFilterIfFromIsEnabled(fromConfiguration, dependencyElement, filter, aggregate, identity);
        return filter;
    }

    private String updateFilterIfFromIsEnabled(Dictionary fromConfiguration, Element dependencyElement, String filter, boolean aggregate, String identity) {
        String from = dependencyElement.getAttribute("from");
        if (fromConfiguration != null && identity != null && fromConfiguration.get(identity) != null) {
            from = (String) fromConfiguration.get(identity);
        }
        if (from != null) {
            String fromFilter = "(|(instance.name=" + from + ")(service.pid=" + from + "))";
            if (aggregate) {
                warn("The 'from' attribute is incompatible with aggregate requirements: only one provider will match : " + fromFilter);
            }
            if (filter != null) {
                filter = "(&" + fromFilter + filter + ")"; // Append the two filters
            } else {
                filter = fromFilter;
            }
        }
        return filter;
    }

    private boolean isProxy(Element dependencyElement) {
        boolean isProxy = true;
        String setting = getProxySetting();

        if (setting == null || PROXY_ENABLED.equals(setting)) { // If not set => Enabled
            isProxy = true;
        } else if (PROXY_DISABLED.equals(setting)) {
            isProxy = false;
        }

        String proxy = dependencyElement.getAttribute("proxy");
        // If proxy == null, use default value
        if (proxy != null) {
            if (proxy.equals("false")) {
                isProxy = false;
            } else if (proxy.equals("true")) {
                if (!isProxy) { // The configuration overrides the system setting
                    warn("The configuration of a service dependency overrides the proxy mode");
                }
                isProxy = true;
            }
        }
        return isProxy;
    }

    private String getProxySetting() {
        // Detect proxy default value.
        String setting = getInstanceManager().getContext().getProperty(PROXY_SETTINGS_PROPERTY);

        // Felix also includes system properties in the bundle context property, however it is not the case of the
        // other frameworks, so if it's null we should call System.getProperty.

        if (setting == null) {
            setting = System.getProperty(PROXY_SETTINGS_PROPERTY);
        }
        return setting;
    }

    private void addCallbacksToDependency(Element dependencyElement, Dependency dep) throws ConfigurationException {
        Element[] cbs = dependencyElement.getElements("Callback");
        for (int j = 0; cbs != null && j < cbs.length; j++) {
            if (!cbs[j].containsAttribute("method") || !cbs[j].containsAttribute("type")) {
                throw new ConfigurationException("Requirement Callback : a dependency callback must contain a method " +
                        "and a type (bind or unbind) attribute");
            }
            String method = cbs[j].getAttribute("method");
            String type = cbs[j].getAttribute("type");

            int methodType = DependencyCallback.UNBIND;
            if ("bind".equalsIgnoreCase(type)) {
                methodType = DependencyCallback.BIND;
            } else if ("modified".equalsIgnoreCase(type)) {
                methodType = DependencyCallback.MODIFIED;
            }

            dep.addDependencyCallback(createDependencyHandler(dep, method, methodType));
        }
    }

    protected DependencyCallback createDependencyHandler(final Dependency dep, final String method, final int type) {
        return new DependencyCallback(dep, method, type);
    }

    private Filter createAndCheckFilter(String filter) throws ConfigurationException {
        Filter fil = null;
        if (filter != null) {
            try {
                fil = getInstanceManager().getContext().createFilter(filter);
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException("A requirement filter is invalid : " + filter, e);
            }
        }
        return fil;
    }

    private BundleContext getFacetedBundleContext(Element dep) {
        String scope = dep.getAttribute("scope");
        BundleContext context = getInstanceManager().getContext(); // Get the default bundle context.
        if (scope != null) {
            // If we are not in a composite, the policy is set to global.
            if (scope.equalsIgnoreCase("global") || ((((IPojoContext) getInstanceManager().getContext()).getServiceContext()) == null)) {
                context =
                        new PolicyServiceContext(getInstanceManager().getGlobalContext(), getInstanceManager().getLocalServiceContext(),
                                PolicyServiceContext.GLOBAL);
            } else if (scope.equalsIgnoreCase("composite")) {
                context =
                        new PolicyServiceContext(getInstanceManager().getGlobalContext(), getInstanceManager().getLocalServiceContext(),
                                PolicyServiceContext.LOCAL);
            } else if (scope.equalsIgnoreCase("composite+global")) {
                context =
                        new PolicyServiceContext(getInstanceManager().getGlobalContext(), getInstanceManager().getLocalServiceContext(),
                                PolicyServiceContext.LOCAL_AND_GLOBAL);
            }
        }
        return context;
    }

    private String getServiceSpecificationAttribute(Element dep) {
        String serviceSpecification = dep.getAttribute("interface");
        // the 'interface' attribute is deprecated
        if (serviceSpecification != null) {
            warn("The 'interface' attribute is deprecated, use the 'specification' attribute instead");
        } else {
            serviceSpecification = dep.getAttribute("specification");
        }
        return serviceSpecification;
    }

    /**
     * Gets the requires filter configuration from the given object.
     * The given object must come from the instance configuration.
     * This method was made to fix FELIX-2688. It supports filter configuration using
     * an array:
     * <code>{"myFirstDep", "(property1=value1)", "mySecondDep", "(property2=value2)"});</code>
     *
     * @param requiresFiltersValue the value contained in the instance
     *                             configuration.
     * @return the dictionary. If the object in already a dictionary, just returns it,
     *         if it's an array, builds the dictionary.
     * @throws ConfigurationException the dictionary cannot be built
     */
    private Dictionary getRequiresFilters(Object requiresFiltersValue)
            throws ConfigurationException {
        if (requiresFiltersValue != null
                && requiresFiltersValue.getClass().isArray()) {
            String[] filtersArray = (String[]) requiresFiltersValue;
            if (filtersArray.length % 2 != 0) {
                throw new ConfigurationException(
                        "A requirement filter is invalid : "
                                + requiresFiltersValue);
            }
            Dictionary<String, Object> requiresFilters = new Hashtable<String, Object>();
            for (int i = 0; i < filtersArray.length; i += 2) {
                requiresFilters.put(filtersArray[i], filtersArray[i + 1]);
            }
            return requiresFilters;
        }

        return (Dictionary) requiresFiltersValue;
    }

    /**
     * Handler start method.
     *
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // Start the dependencies
        for (Dependency dep : m_dependencies) {
            dep.start();
        }
        // Check the state
        m_started = true;
        setValidity(false);
        checkContext();
    }

    /**
     * Handler stop method.
     *
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        m_started = false;
        for (Dependency dep : m_dependencies) {
            dep.stop();
        }
    }

    /**
     * Handler createInstance method. This method is override to allow delayed callback invocation.
     *
     * @param instance : the created object
     * @see org.apache.felix.ipojo.PrimitiveHandler#onCreation(Object)
     */
    public void onCreation(Object instance) {
        for (Dependency dep : m_dependencies) {
            dep.onObjectCreation(instance);
        }
    }

    /**
     * Get the dependency handler description.
     *
     * @return the dependency handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }

    /**
     * The instance is reconfigured.
     *
     * @param configuration the new instance configuration.
     */
    @Override
    public void reconfigure(Dictionary configuration) {
        m_instanceConfigurationSource.reconfigure(configuration);
    }

    @Override
    public void stateChanged(int state) {
        if (state == ComponentInstance.DISPOSED) {
            // Cleanup all dependencies
            for (Dependency dep : m_dependencies) {
                dep.cleanup();
            }
        }
        super.stateChanged(state);
    }
}
