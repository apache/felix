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
<<<<<<< HEAD
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
=======
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

<<<<<<< HEAD
import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
=======
import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.util.promise.Promise;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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

<<<<<<< HEAD
    // needed services
    static final String SCR_SERVICE = "org.apache.felix.scr.ScrService"; //$NON-NLS-1$
    private static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService"; //$NON-NLS-1$
    private static final String CONFIGURATION_ADMIN_NAME = "org.osgi.service.cm.ConfigurationAdmin"; //$NON-NLS-1$

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    WebConsolePlugin()
    {
        super(LABEL, TITLE, CSS);

=======
    // templates
    private final String TEMPLATE;

    private volatile ConfigurationSupport optionalSupport;

    private final ServiceComponentRuntime runtime;

    /** Default constructor */
    WebConsolePlugin(final ServiceComponentRuntime service)
    {
        super(LABEL, TITLE, CSS);

        this.runtime = service;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        // load templates
        TEMPLATE = readTemplateFile("/res/plugin.html"); //$NON-NLS-1$
    }

<<<<<<< HEAD
=======

    @Override
    public void deactivate() {
        if ( this.optionalSupport != null )
        {
            this.optionalSupport.close();
            this.optionalSupport = null;
        }
        super.deactivate();
    }


    @Override
    public void activate(final BundleContext bundleContext)
    {
        super.activate(bundleContext);
        this.optionalSupport = new ConfigurationSupport(bundleContext);
    }


    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public String getCategory()
    {
        return CATEGORY;
    }

<<<<<<< HEAD
    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        final RequestInfo reqInfo = new RequestInfo(request);
        if (reqInfo.component == null && reqInfo.componentRequested)
        {
            response.sendError(404);
            return;
        }
        if (!reqInfo.componentRequested)
=======
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
        if (reqInfo.componentRequested)
        {
            boolean found = false;
            if ( reqInfo.component != null )
            {
                if (OPERATION_ENABLE.equals(op))
                {
                    wait(this.runtime.enableComponent(reqInfo.component.description));
                    reqInfo = new RequestInfo(request, false);
                    found = true;
                }
                else if ( OPERATION_DISABLE.equals(op) )
                {
                    wait(this.runtime.disableComponent(reqInfo.component.description));
                    found = true;
                }
            }
            if ( !found )
            {
                response.sendError(404);
                return;
            }
        }
        else
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
            response.sendError(500);
            return;
        }
<<<<<<< HEAD
        String op = request.getParameter(OPERATION);
        if (OPERATION_ENABLE.equals(op))
        {
            reqInfo.component.enable();
        }
        else if (OPERATION_DISABLE.equals(op))
        {
            reqInfo.component.disable();
        }
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        final PrintWriter pw = response.getWriter();
        response.setContentType("application/json"); //$NON-NLS-1$
        response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
<<<<<<< HEAD
        renderResult(pw, null);
=======
        renderResult(pw, reqInfo, null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
<<<<<<< HEAD
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
=======
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        String path = request.getPathInfo();
        // don't process if this is request to load a resource
        if (!path.startsWith(RES))
        {
<<<<<<< HEAD
            final RequestInfo reqInfo = new RequestInfo(request);
=======
            final RequestInfo reqInfo = new RequestInfo(request, true);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            if (reqInfo.component == null && reqInfo.componentRequested)
            {
                response.sendError(404);
                return;
            }
            if (reqInfo.extension.equals("json")) //$NON-NLS-1$
            {
                response.setContentType("application/json"); //$NON-NLS-1$
                response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$

<<<<<<< HEAD
                this.renderResult(response.getWriter(), reqInfo.component);
=======
                this.renderResult(response.getWriter(), reqInfo, reqInfo.component);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

                // nothing more to do
                return;
            }
        }
        super.doGet(request, response);
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
<<<<<<< HEAD
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
        throws IOException
=======
    @SuppressWarnings("unchecked")
    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
            throws IOException
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);

        StringWriter w = new StringWriter();
        PrintWriter w2 = new PrintWriter(w);
<<<<<<< HEAD
        renderResult(w2, reqInfo.component);
=======
        renderResult(w2, reqInfo, reqInfo.component);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // prepare variables
        DefaultVariableResolver vars = ((DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request));
        vars.put("__drawDetails__", reqInfo.componentRequested ? Boolean.TRUE : Boolean.FALSE); //$NON-NLS-1$
        vars.put("__data__", w.toString()); //$NON-NLS-1$

        response.getWriter().print(TEMPLATE);

    }

<<<<<<< HEAD
    private void renderResult(final PrintWriter pw, final Component component)
        throws IOException
    {
        final JSONWriter jw = new JSONWriter(pw);
        try
        {
            jw.object();

            final ScrService scrService = getScrService();
            if (scrService == null)
            {
                jw.key("status"); //$NON-NLS-1$
                jw.value(-1);
            }
            else
            {
                final Component[] components = scrService.getComponents();

                if (components == null || components.length == 0)
                {
                    jw.key("status"); //$NON-NLS-1$
                    jw.value(0);
                }
                else
                {
                    // order components by name
                    sortComponents(components);

                    final StringBuffer buffer = new StringBuffer();
                    buffer.append(components.length);
                    buffer.append(" component"); //$NON-NLS-1$
                    if (components.length != 1)
                    {
                        buffer.append('s');
                    }
                    buffer.append(" installed."); //$NON-NLS-1$
                    jw.key("status"); //$NON-NLS-1$
                    jw.value(components.length);

                    // render components
                    jw.key("data"); //$NON-NLS-1$
                    jw.array();
                    if (component != null)
                    {
                        component(jw, component, true);
                    }
                    else
                    {
                        for (int i = 0; i < components.length; i++)
                        {
                            component(jw, components[i], false);
                        }
                    }
                    jw.endArray();
                }
            }

            jw.endObject();
        }
        catch (JSONException je)
        {
            throw new IOException(je.toString());
        }
    }

    private void sortComponents(Component[] components)
    {
        Arrays.sort(components, Util.COMPONENT_COMPARATOR);
    }

    private void component(JSONWriter jw, Component component, boolean details)
        throws JSONException
    {
        String id = String.valueOf(component.getId());
        String name = component.getName();
        int state = component.getState();
=======
    private void renderResult(final PrintWriter pw, RequestInfo info, final ComponentConfigurationDTO component)
            throws IOException
    {
        final JSONWriter jw = new JSONWriter(pw);
        jw.object();

        jw.key("status"); //$NON-NLS-1$
        jw.value(info.configurations.size());
        if ( !info.configurations.isEmpty() )
        {
            // render components
            jw.key("data"); //$NON-NLS-1$
            jw.array();
            if (component != null)
            {
                if ( component.state == -1 )
                {
                    component(jw, component.description, null, true);
                }
                else
                {
                    component(jw, component.description, component, true);
                }
            }
            else
            {
                for( final ComponentDescriptionDTO cd : info.disabled )
                {
                    component(jw, cd, null, false);
                }
                for (final ComponentConfigurationDTO cfg : info.configurations)
                {
                    component(jw, cfg.description, cfg, false);
                }
            }
            jw.endArray();
        }

        jw.endObject();
    }

    void writePid(final JSONWriter jw, final ComponentDescriptionDTO desc) throws IOException
    {
        final String configurationPid = desc.configurationPid[0];
        final String pid;
        if (desc.configurationPid.length == 1) {
            pid = configurationPid;
        } else {
            pid = Arrays.toString(desc.configurationPid);
        }
        jw.key("pid"); //$NON-NLS-1$
        jw.value(pid);
        if (this.optionalSupport.isConfigurable(
                this.getBundleContext().getBundle(0).getBundleContext().getBundle(desc.bundle.id),
                configurationPid))
        {
            jw.key("configurable"); //$NON-NLS-1$
            jw.value(configurationPid);
        }
    }

    void component(JSONWriter jw,
            final ComponentDescriptionDTO desc,
            final ComponentConfigurationDTO config, boolean details) throws IOException
    {
        String id = config == null ? "" : String.valueOf(config.id);
        String name = desc.name;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        jw.object();

        // component information
        jw.key("id"); //$NON-NLS-1$
        jw.value(id);
<<<<<<< HEAD
        jw.key("name"); //$NON-NLS-1$
        jw.value(name);
        jw.key("state"); //$NON-NLS-1$
        jw.value(ComponentConfigurationPrinter.toStateString(state));
        jw.key("stateRaw"); //$NON-NLS-1$
        jw.value(state);

        final Dictionary props = component.getProperties();

        final String pid = (String) (props != null ? props.get(Constants.SERVICE_PID)
            : null);
        if (pid != null)
        {
            jw.key("pid"); //$NON-NLS-1$
            jw.value(pid);
            if (isConfigurable(component.getBundle(), pid))
            {
                jw.key("configurable"); //$NON-NLS-1$
                jw.value(pid);
            }
        }

        // component details
        if (details)
        {
            gatherComponentDetails(jw, component);
=======
        jw.key("bundleId"); //$NON-NLS-1$
        jw.value(desc.bundle.id);
        jw.key("name"); //$NON-NLS-1$
        jw.value(name);
        jw.key("state"); //$NON-NLS-1$
        if ( config != null )
        {
            jw.value(ComponentConfigurationPrinter.toStateString(config.state));
            jw.key("stateRaw"); //$NON-NLS-1$
            jw.value(config.state);
        }
        else
        {
            if ( desc.defaultEnabled && "require".equals(desc.configurationPolicy))
            {
                jw.value("no config");
            }
            else
            {
                jw.value("disabled"); //$NON-NLS-1$
            }
            jw.key("stateRaw"); //$NON-NLS-1$
            jw.value(-1);
        }

        writePid(jw, desc);

        // component details
        if (details)
        {
            gatherComponentDetails(jw, desc, config);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        jw.endObject();
    }

<<<<<<< HEAD
    private void gatherComponentDetails(JSONWriter jw, Component component)
        throws JSONException
    {
        jw.key("props"); //$NON-NLS-1$
        jw.array();

        keyVal(jw, "Bundle", component.getBundle().getSymbolicName() + " ("
            + component.getBundle().getBundleId() + ")");
        keyVal(jw, "Implementation Class", component.getClassName());
        if (component.getFactory() != null)
        {
            keyVal(jw, "Component Factory Name", component.getFactory());
        }
        keyVal(jw, "Default State", component.isDefaultEnabled() ? "enabled" : "disabled");
        keyVal(jw, "Activation", component.isImmediate() ? "immediate" : "delayed");

        try
        {
            keyVal(jw, "Configuration Policy", component.getConfigurationPolicy());
        }
        catch (Throwable t)
        {
            // missing implementation of said method in the actually bound API
            // ignore this and just don't display the information
        }

        listServices(jw, component);
        listReferences(jw, component);
        listProperties(jw, component);
=======
    private void gatherComponentDetails(JSONWriter jw,
            ComponentDescriptionDTO desc,
            ComponentConfigurationDTO component) throws IOException
    {
        final Bundle bundle = this.getBundleContext().getBundle(0).getBundleContext().getBundle(desc.bundle.id);

        jw.key("props"); //$NON-NLS-1$
        jw.array();

        keyVal(jw, "Bundle", bundle.getSymbolicName() + " ("
                + bundle.getBundleId() + ")");
        keyVal(jw, "Implementation Class", desc.implementationClass);
        if (desc.factory != null)
        {
            keyVal(jw, "Component Factory Name", desc.factory);
        }
        keyVal(jw, "Default State", desc.defaultEnabled ? "enabled" : "disabled");
        keyVal(jw, "Activation", desc.immediate ? "immediate" : "delayed");

        keyVal(jw, "Configuration Policy", desc.configurationPolicy);

        if ( component != null && component.state == ComponentConfigurationDTO.FAILED_ACTIVATION && component.failure != null ) {
            keyVal(jw, "failure", component.failure);
        }
        if ( component != null && component.service != null ) {
            keyVal(jw, "serviceId", component.service.id);
        }
        listServices(jw, desc);
        if (desc.configurationPid.length == 1) {
            keyVal(jw, "PID", desc.configurationPid[0]);
        } else {
            keyVal(jw, "PIDs", Arrays.toString(desc.configurationPid));
        }
        listReferences(jw, desc, component);
        listProperties(jw, desc, component);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        jw.endArray();
    }

<<<<<<< HEAD
    private void listServices(JSONWriter jw, Component component)
    {
        String[] services = component.getServices();
=======
    private void listServices(JSONWriter jw, ComponentDescriptionDTO desc) throws IOException
    {
        String[] services = desc.serviceInterfaces;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if (services == null)
        {
            return;
        }

<<<<<<< HEAD
        keyVal(jw, "Service Type", component.isServiceFactory() ? "service factory"
            : "service");

        JSONArray buf = new JSONArray();
        for (int i = 0; i < services.length; i++)
        {
            buf.put(services[i]);
        }

        keyVal(jw, "Services", buf);
    }

    private void listReferences(JSONWriter jw, Component component)
    {
        Reference[] refs = component.getReferences();
        if (refs != null)
        {
            for (int i = 0; i < refs.length; i++)
            {
                JSONArray buf = new JSONArray();
                buf.put(refs[i].isSatisfied() ? "Satisfied" : "Unsatisfied");
                buf.put("Service Name: " + refs[i].getServiceName());
                if (refs[i].getTarget() != null)
                {
                    buf.put("Target Filter: " + refs[i].getTarget());
                }
                buf.put("Multiple: " + (refs[i].isMultiple() ? "multiple" : "single"));
                buf.put("Optional: " + (refs[i].isOptional() ? "optional" : "mandatory"));
                buf.put("Policy: " + (refs[i].isStatic() ? "static" : "dynamic"));

                // list bound services
                ServiceReference[] boundRefs = refs[i].getServiceReferences();
                if (boundRefs != null && boundRefs.length > 0)
                {
                    for (int j = 0; j < boundRefs.length; j++)
                    {
                        final StringBuffer b = new StringBuffer();
                        b.append("Bound Service ID ");
                        b.append(boundRefs[j].getProperty(Constants.SERVICE_ID));

                        String name = (String) boundRefs[j].getProperty(ComponentConstants.COMPONENT_NAME);
                        if (name == null)
                        {
                            name = (String) boundRefs[j].getProperty(Constants.SERVICE_PID);
                            if (name == null)
                            {
                                name = (String) boundRefs[j].getProperty(Constants.SERVICE_DESCRIPTION);
                            }
                        }
                        if (name != null)
                        {
                            b.append(" (");
                            b.append(name);
                            b.append(")");
                        }
                        buf.put(b.toString());
                    }
                }
                else
                {
                    buf.put("No Services bound");
                }

                keyVal(jw, "Reference " + refs[i].getName(), buf.toString());
            }
        }
    }

    private void listProperties(JSONWriter jw, Component component)
    {
        Dictionary props = component.getProperties();
        if (props != null)
        {
            JSONArray buf = new JSONArray();
            TreeSet keys = new TreeSet(Util.list(props.keys()));
            for (Iterator ki = keys.iterator(); ki.hasNext();)
            {
                final String key = (String) ki.next();
                final StringBuffer b = new StringBuffer();
=======
        if ( desc.scope != null ) {
            keyVal(jw, "Service Type", desc.scope);
        }
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

    private void listReferences(JSONWriter jw, ComponentDescriptionDTO desc, ComponentConfigurationDTO config) throws IOException
    {
        for(final ReferenceDTO dto : desc.references)
        {
            jw.object();
            jw.key("key");
            jw.value("Reference " + dto.name);
            jw.key("value");
            jw.array();
            final SatisfiedReferenceDTO satisfiedRef;
            if ( config != null )
            {
                satisfiedRef = findReference(config, dto.name);

                jw.value(satisfiedRef != null ? "Satisfied" : "Unsatisfied");
            }
            else
            {
                satisfiedRef = null;
            }
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
            else if ( config != null )
            {
                jw.value("No Services bound");
            }

            jw.endArray();
            jw.endObject();
        }
    }

    private void listProperties(JSONWriter jw, ComponentDescriptionDTO desc, ComponentConfigurationDTO component) throws IOException
    {
        Map<String, Object> props = component != null ? component.properties : desc.properties;
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                b.append(key).append(" = ");

                Object prop = props.get(key);
                prop = WebConsoleUtil.toString(prop);
                b.append(prop);
<<<<<<< HEAD
                buf.put(b.toString());
            }

            keyVal(jw, "Properties", buf);
        }

    }

    private void keyVal(JSONWriter jw, String key, Object value)
    {
        try
        {
            WebConsoleUtil.keyVal(jw, key, value);
        }
        catch (JSONException je)
        {
            // don't care
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

    final ScrService getScrService()
    {
        return (ScrService) getService(SCR_SERVICE);
    }

    private final MetaTypeService getMetaTypeService()
    {
        return (MetaTypeService) getService(META_TYPE_NAME);
    }
=======
                jw.value(b.toString());
            }
            jw.endArray();
            jw.endObject();
        }
        if ( component == null && desc.factoryProperties != null ) {
            jw.object();
            jw.key("key");
            jw.value("FactoryProperties");
            jw.key("value");
            jw.array();
            TreeSet<String> keys = new TreeSet<String>(desc.factoryProperties.keySet());
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

    private void keyVal(JSONWriter jw, String key, Object value) throws IOException
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


>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private final class RequestInfo
    {
        public final String extension;
<<<<<<< HEAD
        public final Component component;
        public final boolean componentRequested;

        protected RequestInfo(final HttpServletRequest request)
=======
        public final ComponentConfigurationDTO component;
        public final boolean componentRequested;
        public final List<ComponentDescriptionDTO> descriptions = new ArrayList<ComponentDescriptionDTO>();
        public final List<ComponentConfigurationDTO> configurations = new ArrayList<ComponentConfigurationDTO>();
        public final List<ComponentDescriptionDTO> disabled = new ArrayList<ComponentDescriptionDTO>();

        protected RequestInfo(final HttpServletRequest request, final boolean checkPathInfo)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        {
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

<<<<<<< HEAD
            if (info.length() > 1 && info.startsWith("/")) //$NON-NLS-1$
            {
                this.componentRequested = true;
                info = info.substring(1);
                Component component = getComponentId(info);
=======
            this.descriptions.addAll(runtime.getComponentDescriptionDTOs());
            if (checkPathInfo && info.length() > 1 && info.startsWith("/")) //$NON-NLS-1$
            {
                this.componentRequested = true;
                info = info.substring(1);
                ComponentConfigurationDTO component = getComponentId(info);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                if (component == null)
                {
                    component = getComponentByName(info);
                }
                this.component = component;
<<<<<<< HEAD
=======
                if ( this.component != null )
                {
                    this.configurations.add(this.component);
                }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
            else
            {
                this.componentRequested = false;
                this.component = null;
<<<<<<< HEAD
=======

                for(final ComponentDescriptionDTO d : this.descriptions)
                {
                    if ( !runtime.isComponentEnabled(d) )
                    {
                        disabled.add(d);
                    }
                    else
                    {
                        final Collection<ComponentConfigurationDTO> configs = runtime.getComponentConfigurationDTOs(d);
                        if ( configs.isEmpty() )
                        {
                            disabled.add(d);
                        }
                        else
                        {
                            configurations.addAll(configs);
                        }
                    }
                }
                Collections.sort(configurations, Util.COMPONENT_COMPARATOR);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            request.setAttribute(WebConsolePlugin.this.getClass().getName(), this);
        }

<<<<<<< HEAD
        protected Component getComponentId(final String componentIdPar)
        {
            final ScrService scrService = getScrService();
            if (scrService != null)
            {
                try
                {
                    final long componentId = Long.parseLong(componentIdPar);
                    return scrService.getComponent(componentId);
                }
                catch (NumberFormatException nfe)
                {
                    // don't care
                }
            }
=======
        protected ComponentConfigurationDTO getComponentId(final String componentIdPar)
        {
            try
            {
                final long componentId = Long.parseLong(componentIdPar);
                for(final ComponentDescriptionDTO desc : this.descriptions)
                {
                    for(final ComponentConfigurationDTO cfg : runtime.getComponentConfigurationDTOs(desc))
                    {
                        if ( cfg.id == componentId )
                        {
                            return cfg;
                        }
                    }
                }
            }
            catch (NumberFormatException nfe)
            {
                // don't care
            }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

            return null;
        }

<<<<<<< HEAD
        protected Component getComponentByName(final String names)
        {
            if (names.length() > 0)
            {
                final ScrService scrService = getScrService();
                if (scrService != null)
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

                    Component[] components;
                    try
                    {
                        components = scrService.getComponents(componentName);
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
                            for (int i = 0; i < components.length; i++)
                            {
                                Component component = components[i];
                                if (pid.equals(component.getProperties().get(
                                    Constants.SERVICE_PID)))
                                {
                                    return component;
                                }
                            }
                        }
                        else if (components.length > 0)
                        {
                            return components[0];
=======
        protected ComponentConfigurationDTO getComponentByName(final String names)
        {
            if (names.length() > 0)
            {
                final int slash = names.lastIndexOf('/');
                final String componentName;
                final String pid;
                long bundleId = -1;
                if (slash > 0)
                {
                    pid = names.substring(slash + 1);
                    final String firstPart = names.substring(0, slash);
                    final int bundleIndex = firstPart.indexOf('/');
                    if ( bundleIndex == -1 )
                    {
                        componentName = firstPart;
                    }
                    else
                    {
                        componentName = firstPart.substring(bundleIndex + 1);
                        try
                        {
                            bundleId = Long.valueOf(firstPart.substring(0, bundleIndex));
                        }
                        catch ( final NumberFormatException nfe)
                        {
                            // wrong format
                            return null;
                        }
                    }
                }
                else
                {
                    componentName = names;
                    pid = null;
                }

                Collection<ComponentConfigurationDTO> components = null;
                for(final ComponentDescriptionDTO d : this.descriptions)
                {
                    if ( d.name.equals(componentName) && (bundleId == -1 || d.bundle.id == bundleId))
                    {
                        components = runtime.getComponentConfigurationDTOs(d);
                        if ( components.isEmpty() )
                        {
                            final ComponentConfigurationDTO cfg = new ComponentConfigurationDTO();
                            cfg.description = d;
                            cfg.state = -1;
                            return cfg;
                        }
                        else
                        {
                            if (pid != null)
                            {
                                final Iterator<ComponentConfigurationDTO> i = components.iterator();
                                while ( i.hasNext() )
                                {
                                    ComponentConfigurationDTO c = i.next();
                                    if ( pid.equals(c.description.configurationPid[0]))
                                    {
                                        return c;
                                    }

                                }
                            }
                            else if (components.size() > 0)
                            {
                                return components.iterator().next();
                            }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        }
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
