/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.plugins.ds.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.promise.Promise;

/**
 * ComponentsServlet provides a plugin for managing Service Components Runtime.
 */
class WebConsolePlugin extends SimpleWebConsolePlugin
{

    private static final long serialVersionUID = 1L;

    private static final String LABEL = "components"; //$NON-NLS-1$
    private static final String TITLE = "%components.pluginTitle"; //$NON-NLS-1$
    private static final String CATEGORY = "OSGi"; //$NON-NLS-1$
    private static final String CSS[] = { "/res/ui/bundles.css" }; // yes, it's correct! //$NON-NLS-1$
    private static final String RES = "/" + LABEL + "/res/"; //$NON-NLS-1$ //$NON-NLS-2$

    // actions
    private static final String OPERATION = "action"; //$NON-NLS-1$
    private static final String OPERATION_ENABLE = "enable"; //$NON-NLS-1$
    private static final String OPERATION_DISABLE = "disable"; //$NON-NLS-1$
    //private static final String OPERATION_CONFIGURE = "configure";

    // needed services
    static final String SCR_SERVICE = ServiceComponentRuntime.class.getName(); //$NON-NLS-1$
    private static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService"; //$NON-NLS-1$
    private static final String CONFIGURATION_ADMIN_NAME = "org.osgi.service.cm.ConfigurationAdmin"; //$NON-NLS-1$

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    WebConsolePlugin()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile("/res/plugin.html"); //$NON-NLS-1$
    }

    @Override
    public String getCategory()
    {
        return CATEGORY;
    }

    private void wait(final Promise<Void> p )
    {
        while ( !p.isDone() )
        {
            try
            {
                Thread.sleep(5);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        final String op = request.getParameter(OPERATION);
        RequestInfo reqInfo = new RequestInfo(request, true);
        if (reqInfo.component == null && reqInfo.componentRequested)
        {
            boolean found = false;
            if (OPERATION_ENABLE.equals(op))
            {
                final String name = reqInfo.name;
                for(final ComponentDescriptionDTO cd : reqInfo.disabled)
                {
                    if ( name.equals(cd.name) )
                    {
                        wait(getScrService().enableComponent(cd));
                        reqInfo = new RequestInfo(request, false);
                        found = true;
                        break;
                    }
                }
            }
            if ( !found )
            {
                response.sendError(404);
                return;
            }
        }
        else
        {
            if (!reqInfo.componentRequested)
            {
                response.sendError(500);
                return;
            }
            if (OPERATION_DISABLE.equals(op))
            {
                wait(getScrService().disableComponent(reqInfo.component.description));
                reqInfo = new RequestInfo(request, false);
            }
        }

        final PrintWriter pw = response.getWriter();
        response.setContentType("application/json"); //$NON-NLS-1$
        response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
        renderResult(pw, reqInfo, null);
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String path = request.getPathInfo();
        // don't process if this is request to load a resource
        if (!path.startsWith(RES))
        {
            final RequestInfo reqInfo = new RequestInfo(request, true);
            if (reqInfo.component == null && reqInfo.componentRequested)
            {
                response.sendError(404);
                return;
            }
            if (reqInfo.extension.equals("json")) //$NON-NLS-1$
            {
                response.setContentType("application/json"); //$NON-NLS-1$
                response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

                this.renderResult(response.getWriter(), reqInfo, reqInfo.component);

                // nothing more to do
                return;
            }
        }
        super.doGet(request, response);
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);

        StringWriter w = new StringWriter();
        PrintWriter w2 = new PrintWriter(w);
        renderResult(w2, reqInfo, reqInfo.component);

        // prepare variables
        DefaultVariableResolver vars = ((DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request));
        vars.put("__drawDetails__", reqInfo.componentRequested ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$
        vars.put("__data__", w.toString()); //$NON-NLS-1$

        response.getWriter().print(TEMPLATE);

    }

    private void renderResult(final PrintWriter pw, RequestInfo info, final ComponentConfigurationDTO component)
            throws IOException
    {
        final JSONWriter jw = new JSONWriter(pw);
        jw.object();

        jw.key("status"); //$NON-NLS-1$
        final ServiceComponentRuntime scrService = getScrService();
        if (scrService == null)
        {
            jw.value(-1);
        }
        else
        {
            jw.value(info.configurations.size());
            if ( !info.configurations.isEmpty())
            {
                // render components
                jw.key("data"); //$NON-NLS-1$
                jw.array();
                if (component != null)
                {
                    component(jw, component, true);
                }
                else
                {
                    for( final ComponentDescriptionDTO cd : info.disabled )
                    {
                        disabledComponent(jw, cd);
                    }
                    for (final ComponentConfigurationDTO cfg : info.configurations)
                    {
                        component(jw, cfg, false);
                    }
                }
                jw.endArray();
            }
        }

        jw.endObject();
    }

    void disabledComponent(final JSONWriter jw, final ComponentDescriptionDTO component)
    {
        final String name = component.name;

        jw.object();

        // component information
        jw.key("id"); //$NON-NLS-1$
        jw.value("");
        jw.key("name"); //$NON-NLS-1$
        jw.value(name);
        jw.key("state"); //$NON-NLS-1$
        jw.value("disabled"); //$NON-NLS-1$
        jw.key("stateRaw"); //$NON-NLS-1$
        jw.value(-1);

        if ( component.configurationPid != null && component.configurationPid.length > 0 )
        {
            final String pid;
            if ( component.configurationPid.length == 1 )
            {
                pid = component.configurationPid[0];
            }
            else
            {
                pid = Arrays.toString(component.configurationPid);
            }
            jw.key("pid"); //$NON-NLS-1$
            jw.value(pid);
        }

        jw.endObject();
    }

    void component(JSONWriter jw, ComponentConfigurationDTO component, boolean details)
    {
        String id = String.valueOf(component.id);
        String name = component.description.name;
        int state = component.state;

        jw.object();

        // component information
        jw.key("id"); //$NON-NLS-1$
        jw.value(id);
        jw.key("name"); //$NON-NLS-1$
        jw.value(name);
        jw.key("state"); //$NON-NLS-1$
        jw.value(ComponentConfigurationPrinter.toStateString(state));
        jw.key("stateRaw"); //$NON-NLS-1$
        jw.value(state);

        final String configurationPid = component.description.configurationPid[0];
        final String pid;
        if (component.description.configurationPid.length == 1) {
            pid = configurationPid;
        } else {
            pid = Arrays.toString(component.description.configurationPid);
        }
        jw.key("pid"); //$NON-NLS-1$
        jw.value(pid);
        if (isConfigurable(
                this.getBundleContext().getBundle(0).getBundleContext().getBundle(component.description.bundle.id),
                configurationPid))
        {
            jw.key("configurable"); //$NON-NLS-1$
            jw.value(configurationPid);
        }

        // component details
        if (details)
        {
            gatherComponentDetails(jw, component);
        }

        jw.endObject();
    }

    private void gatherComponentDetails(JSONWriter jw, ComponentConfigurationDTO component)
    {
        final Bundle bundle = this.getBundleContext().getBundle(0).getBundleContext().getBundle(component.description.bundle.id);

        jw.key("props"); //$NON-NLS-1$
        jw.array();

        keyVal(jw, "Bundle", bundle.getSymbolicName() + " ("
                + bundle.getBundleId() + ")");
        keyVal(jw, "Implementation Class", component.description.implementationClass);
        if (component.description.factory != null)
        {
            keyVal(jw, "Component Factory Name", component.description.factory);
        }
        keyVal(jw, "Default State", component.description.defaultEnabled ? "enabled" : "disabled");
        keyVal(jw, "Activation", component.description.immediate ? "immediate" : "delayed");

        try
        {
            keyVal(jw, "Configuration Policy", component.description.configurationPolicy);
        }
        catch (Throwable t)
        {
            // missing implementation of said method in the actually bound API
            // ignore this and just don't display the information
        }

        listServices(jw, component);
        if (component.description.configurationPid.length == 1) {
            keyVal(jw, "PID", component.description.configurationPid[0]);
        } else {
            keyVal(jw, "PIDs", Arrays.toString(component.description.configurationPid));
        }
        listReferences(jw, component);
        listProperties(jw, component);

        jw.endArray();
    }

    private void listServices(JSONWriter jw, ComponentConfigurationDTO component)
    {
        String[] services = component.description.serviceInterfaces;
        if (services == null)
        {
            return;
        }

        keyVal(jw, "Service Type", component.description.scope);

        jw.object();
        jw.key("key");
        jw.value("Services");
        jw.key("value");
        jw.array();
        for (int i = 0; i < services.length; i++)
        {
            jw.value(services[i]);
        }
        jw.endArray();
        jw.endObject();
    }

    private SatisfiedReferenceDTO findReference(final ComponentConfigurationDTO component, final String name)
    {
        for(final SatisfiedReferenceDTO dto : component.satisfiedReferences)
        {
            if ( dto.name.equals(name))
            {
                return dto;
            }
        }
        return null;
    }

    private void listReferences(JSONWriter jw, ComponentConfigurationDTO component)
    {
        for(final ReferenceDTO dto : component.description.references)
        {
            jw.object();
            jw.key("key");
            jw.value("Reference " + dto.name);
            jw.key("value");
            jw.array();
            final SatisfiedReferenceDTO satisfiedRef = findReference(component, dto.name);

            jw.value(satisfiedRef != null ? "Satisfied" : "Unsatisfied");
            jw.value("Service Name: " + dto.interfaceName);
            if (dto.target != null)
            {
                jw.value("Target Filter: " + dto.target);
            }
            jw.value("Cardinality: " + dto.cardinality);
            jw.value("Policy: " + dto.policy);
            jw.value("Policy Option: " + dto.policyOption);

            // list bound services
            if ( satisfiedRef != null )
            {
                for (int j = 0; j < satisfiedRef.boundServices.length; j++)
                {
                    final StringBuffer b = new StringBuffer();
                    b.append("Bound Service ID ");
                    b.append(satisfiedRef.boundServices[j].id);

                    String name = (String) satisfiedRef.boundServices[j].properties.get(ComponentConstants.COMPONENT_NAME);
                    if (name == null)
                    {
                        name = (String) satisfiedRef.boundServices[j].properties.get(Constants.SERVICE_PID);
                        if (name == null)
                        {
                            name = (String) satisfiedRef.boundServices[j].properties.get(Constants.SERVICE_DESCRIPTION);
                        }
                    }
                    if (name != null)
                    {
                        b.append(" (");
                        b.append(name);
                        b.append(")");
                    }
                    jw.value(b.toString());
                }
            }
            else
            {
                jw.value("No Services bound");
            }

            jw.endArray();
            jw.endObject();
        }
    }

    private void listProperties(JSONWriter jw, ComponentConfigurationDTO component)
    {
        Map<String, Object> props = component.properties;
        if (props != null)
        {
            jw.object();
            jw.key("key");
            jw.value("Properties");
            jw.key("value");
            jw.array();
            TreeSet<String> keys = new TreeSet<String>(props.keySet());
            for (Iterator<String> ki = keys.iterator(); ki.hasNext();)
            {
                final String key = ki.next();
                final StringBuilder b = new StringBuilder();
                b.append(key).append(" = ");

                Object prop = props.get(key);
                prop = WebConsoleUtil.toString(prop);
                b.append(prop);
                jw.value(b.toString());
            }
            jw.endArray();
            jw.endObject();
        }

    }

    private void keyVal(JSONWriter jw, String key, Object value)
    {
        if (key != null && value != null)
        {
            jw.object();
            jw.key("key"); //$NON-NLS-1$
            jw.value(key);
            jw.key("value"); //$NON-NLS-1$
            jw.value(value);
            jw.endObject();
        }
    }

    /**
     * Check if the component with the specified pid is
     * configurable
     * @param providingBundle The Bundle providing the component. This may be
     *      theoretically be <code>null</code>.
     * @param pid A non null pid
     * @return <code>true</code> if the component is configurable.
     */
    private boolean isConfigurable(final Bundle providingBundle, final String pid)
    {
        // we first check if the config admin has something for this pid
        final ConfigurationAdmin ca = this.getConfigurationAdmin();
        if (ca != null)
        {
            try
            {
                // we use listConfigurations to not create configuration
                // objects persistently without the user providing actual
                // configuration
                String filter = '(' + Constants.SERVICE_PID + '=' + pid + ')';
                Configuration[] configs = ca.listConfigurations(filter);
                if (configs != null && configs.length > 0)
                {
                    return true;
                }
            }
            catch (InvalidSyntaxException ise)
            {
                // should print message
            }
            catch (IOException ioe)
            {
                // should print message
            }
        }
        // second check is using the meta type service
        if (providingBundle != null)
        {
            final MetaTypeService mts = this.getMetaTypeService();
            if (mts != null)
            {
                final MetaTypeInformation mti = mts.getMetaTypeInformation(providingBundle);
                if (mti != null)
                {
                    try {
                        return mti.getObjectClassDefinition(pid, null) != null;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private final ConfigurationAdmin getConfigurationAdmin()
    {
        return (ConfigurationAdmin) getService(CONFIGURATION_ADMIN_NAME);
    }

    final ServiceComponentRuntime getScrService()
    {
        return (ServiceComponentRuntime) getService(SCR_SERVICE);
    }

    private final MetaTypeService getMetaTypeService()
    {
        return (MetaTypeService) getService(META_TYPE_NAME);
    }

    private final class RequestInfo
    {
        public final String extension;
        public final ComponentConfigurationDTO component;
        public final boolean componentRequested;
        public final ServiceComponentRuntime scrService;
        public final List<ComponentDescriptionDTO> descriptions = new ArrayList<ComponentDescriptionDTO>();
        public final List<ComponentConfigurationDTO> configurations = new ArrayList<ComponentConfigurationDTO>();
        public final List<ComponentDescriptionDTO> disabled = new ArrayList<ComponentDescriptionDTO>();
        public final String name;

        protected RequestInfo(final HttpServletRequest request, final boolean checkPathInfo)
        {
            this.scrService = getScrService();
            if ( scrService != null )
            {
                final Collection<ComponentDescriptionDTO> descs = scrService.getComponentDescriptionDTOs();
                for(final ComponentDescriptionDTO d : descs)
                {
                    descriptions.add(d);
                    if ( !scrService.isComponentEnabled(d) )
                    {
                        disabled.add(d);
                    }
                    configurations.addAll(scrService.getComponentConfigurationDTOs(d));
                }
                Collections.sort(configurations, Util.COMPONENT_COMPARATOR);
            }

            String info = request.getPathInfo();
            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);

            // get extension
            if (info.endsWith(".json")) //$NON-NLS-1$
            {
                extension = "json"; //$NON-NLS-1$
                info = info.substring(0, info.length() - 5);
            }
            else
            {
                extension = "html"; //$NON-NLS-1$
            }

            if (checkPathInfo && info.length() > 1 && info.startsWith("/")) //$NON-NLS-1$
            {
                this.componentRequested = true;
                info = info.substring(1);
                ComponentConfigurationDTO component = getComponentId(info);
                if (component == null)
                {
                    component = getComponentByName(info);
                }
                this.component = component;
            }
            else
            {
                this.componentRequested = false;
                this.component = null;
            }
            this.name = info;

            request.setAttribute(WebConsolePlugin.this.getClass().getName(), this);
        }

        protected ComponentConfigurationDTO getComponentId(final String componentIdPar)
        {
            try
            {
                final long componentId = Long.parseLong(componentIdPar);
                for(final ComponentConfigurationDTO cfg : this.configurations)
                {
                    if ( cfg.id == componentId )
                    {
                        return cfg;
                    }
                }
            }
            catch (NumberFormatException nfe)
            {
                // don't care
            }

            return null;
        }

        protected ComponentConfigurationDTO getComponentByName(final String names)
        {
            if (names.length() > 0)
            {
                final int slash = names.lastIndexOf('/');
                final String componentName;
                final String pid;
                if (slash > 0)
                {
                    componentName = names.substring(0, slash);
                    pid = names.substring(slash + 1);
                }
                else
                {
                    componentName = names;
                    pid = null;
                }

                Collection<ComponentConfigurationDTO> components = null;
                try
                {
                    for(final ComponentDescriptionDTO d : this.descriptions)
                    {
                        if ( d.name.equals(componentName) )
                        {
                            components = scrService.getComponentConfigurationDTOs(d);
                        }
                    }
                }
                catch (Throwable t)
                {
                    // not implemented in the used API version
                    components = null;
                }

                if (components != null)
                {
                    if (pid != null)
                    {
                        final Iterator<ComponentConfigurationDTO> i = components.iterator();
                        while ( i.hasNext() )
                        {
                            ComponentConfigurationDTO c = i.next();
                            if (pid.equals(c.properties.get(Constants.SERVICE_PID)))
                            {
                                return c;
                            }

                        }
                    }
                    else if (components.size() > 0)
                    {
                        return components.iterator().next();
                    }
                }
            }

            return null;
        }
    }

    static RequestInfo getRequestInfo(final HttpServletRequest request)
    {
        return (RequestInfo) request.getAttribute(WebConsolePlugin.class.getName());
    }
}
