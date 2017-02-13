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
package org.apache.felix.webconsole.plugins.deppack.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.utils.json.JSONWriter;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.util.tracker.ServiceTracker;

/**
 * DepPackServlet provides a plugin for managing deployment admin packages.
 */
class WebConsolePlugin extends SimpleWebConsolePlugin
{

    private static final String LABEL = "deppack"; //$NON-NLS-1$
    private static final String TITLE = "%deppack.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = { "/" + LABEL + "/res/plugin.css" }; //$NON-NLS-1$ //$NON-NLS-2$
    private static final String CATEGORY = "OSGi"; //$NON-NLS-1$

    //
    private static final String ACTION_DEPLOY = "deploydp"; //$NON-NLS-1$
    private static final String ACTION_UNINSTALL = "uninstalldp"; //$NON-NLS-1$
    private static final String PARAMETER_PCK_FILE = "pckfile"; //$NON-NLS-1$

    // templates
    private final String TEMPLATE;

    private final ServiceTracker adminTracker;

    /** Default constructor */
    WebConsolePlugin(ServiceTracker adminTracker)
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile("/res/plugin.html"); //$NON-NLS-1$
        this.adminTracker = adminTracker;
    }

    public String getCategory()
    {
        return CATEGORY;
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        // get the uploaded data
        final String action = WebConsoleUtil.getParameter(req, Util.PARAM_ACTION);
        if (ACTION_DEPLOY.equals(action))
        {
            Map params = (Map) req.getAttribute(AbstractWebConsolePlugin.ATTR_FILEUPLOAD);
            if (params != null)
            {
                final FileItem pck = getFileItem(params, PARAMETER_PCK_FILE, false);
                final DeploymentAdmin admin = (DeploymentAdmin) adminTracker.getService();
                if (admin != null)
                {
                    try
                    {
                        admin.installDeploymentPackage(pck.getInputStream());

                        final String uri = req.getRequestURI();
                        resp.sendRedirect(uri);
                        return;
                    }
                    catch ( /*Deployment*/Exception e)
                    {
                        throw new ServletException("Unable to deploy package.", e);
                    }
                }
            }
            throw new ServletException("Upload file or deployment admin missing.");
        }
        else if (ACTION_UNINSTALL.equals(action))
        {
            final String pckId = req.getPathInfo().substring(
                req.getPathInfo().lastIndexOf('/') + 1);
            if (pckId != null && pckId.length() > 0)
            {
                final DeploymentAdmin admin = (DeploymentAdmin) adminTracker.getService();
                if (admin != null)
                {
                    try
                    {
                        final DeploymentPackage pck = admin.getDeploymentPackage(pckId);
                        if (pck != null)
                        {
                            pck.uninstall();
                        }
                    }
                    catch ( /*Deployment*/Exception e)
                    {
                        throw new ServletException("Unable to undeploy package.", e);
                    }
                }

            }

            final PrintWriter pw = resp.getWriter();
            pw.println("{ \"reload\":true }");
            return;
        }
        throw new ServletException("Unknown action: " + action);
    }

    private static final FileItem getFileItem(Map params, String name, boolean isFormField)
    {
        FileItem[] items = (FileItem[]) params.get(name);
        if (items != null)
        {
            for (int i = 0; i < items.length; i++)
            {
                if (items[i].isFormField() == isFormField)
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

        final DeploymentAdmin admin = (DeploymentAdmin) adminTracker.getService();

        StringWriter w = new StringWriter();
        PrintWriter w2 = new PrintWriter(w);
        JSONWriter jw = new JSONWriter(w2);
        jw.object();
        if (null == admin)
        {
            jw.key("error"); //$NON-NLS-1$
            jw.value(true);
        }
        else
        {
            final DeploymentPackage[] packages = admin.listDeploymentPackages();
            jw.key("data"); //$NON-NLS-1$

            jw.array();
            for (int i = 0; i < packages.length; i++)
            {
                packageInfoJson(jw, packages[i]);
            }
            jw.endArray();

        }
        jw.endObject();


        // prepare variables
        DefaultVariableResolver vars = ((DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request));
        vars.put("__data__", w.toString()); //$NON-NLS-1$

        response.getWriter().print(TEMPLATE);
    }

    private static final void packageInfoJson(JSONWriter jw, DeploymentPackage pack)
        throws IOException
    {
        jw.object();
        jw.key("id"); //$NON-NLS-1$
        jw.value(pack.getName());
        jw.key("name"); //$NON-NLS-1$
        jw.value(pack.getName());
        jw.key("state"); //$NON-NLS-1$
        jw.value(pack.getVersion());

        jw.key("actions"); //$NON-NLS-1$
        jw.array();

        jw.object();
        jw.key("enabled"); //$NON-NLS-1$
        jw.value(true);
        jw.key("name"); //$NON-NLS-1$
        jw.value("Uninstall");
        jw.key("link"); //$NON-NLS-1$
        jw.value(ACTION_UNINSTALL);
        jw.endObject();

        jw.endArray();

        jw.key("props"); //$NON-NLS-1$
        jw.array();
        jw.object();
        jw.key("key");
        jw.value("Package Name");
        jw.key("value");
        jw.value(pack.getName());
        jw.endObject();

        jw.object();
        jw.key("key");
        jw.value("Version");
        jw.key("value");
        jw.value(pack.getVersion());
        jw.endObject();

        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < pack.getBundleInfos().length; i++)
        {
            buffer.append(pack.getBundleInfos()[i].getSymbolicName());
            buffer.append(" - "); //$NON-NLS-1$
            buffer.append(pack.getBundleInfos()[i].getVersion());
            buffer.append("<br/>"); //$NON-NLS-1$
        }
        jw.object();
        jw.key("key");
        jw.value("Bundles");
        jw.key("value");
        jw.value(buffer.toString());
        jw.endObject();

        jw.endArray();

        jw.endObject();
    }

}
