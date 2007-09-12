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
package org.apache.sling.console.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.sling.console.web.internal.Util;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * The <code>Sling Manager</code> TODO
 *
 * @scr.component immediate="true" label="%manager.name"
 *                description="%manager.description"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Sling Management Console"
 * @scr.reference name="operation"
 *                interface="org.apache.sling.console.web.Action"
 *                cardinality="0..n" policy="dynamic"
 * @scr.reference name="render" interface="org.apache.sling.console.web.Render"
 *                cardinality="0..n" policy="dynamic"
 */
public class SlingManager extends GenericServlet {

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /**
     * @scr.property value="/sling"
     */
    private static final String PROP_MANAGER_ROOT = "manager.root";

    /**
     * @scr.property value="Sling Management Console"
     */
    private static final String PROP_REALM = "realm";

    /**
     * @scr.property value="admin"
     */
    private static final String PROP_USER_NAME = "username";

    /**
     * @scr.property value="admin"
     */
    private static final String PROP_PASSWORD = "password";

    private ComponentContext componentContext;

    /**
     * @scr.reference
     */
    private HttpService httpService;

    /**
     * @scr.reference
     */
    private LogService logService;

    private Map<String, Action> operations = new HashMap<String, Action>();

    private SortedMap<String, Render> renders = new TreeMap<String, Render>();

    private Render defaultRender;

    private String webManagerRoot;

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // check whether we are not at .../{webManagerRoot}
        if (request.getRequestURI().endsWith(this.webManagerRoot)) {
            response.sendRedirect(request.getRequestURI() + "/"
                + this.defaultRender.getName());
            return;
        }

        // handle the request action, terminate if done
        if (this.handleAction(request, response)) {
            return;
        }

        // otherwise we render the response
        Render render = this.getRender(request);
        if (render == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String current = render.getName();
        boolean disabled = false; // should take action==shutdown into
        // account:
        // Boolean.valueOf(request.getParameter("disabled")).booleanValue();

        PrintWriter pw = Util.startHtml(response, render.getLabel());
        Util.navigation(pw, this.renders.values(), current, disabled);

        render.render(request, response);

        Util.endHhtml(pw);
    }

    protected boolean handleAction(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        // check action
        String actionName = this.getParameter(req, Util.PARAM_ACTION);
        if (actionName != null) {
            Action action = this.operations.get(actionName);
            if (action != null) {
                boolean redirect = true;
                try {
                    redirect = action.performAction(req, resp);
                } catch (IOException ioe) {
                    this.log(ioe.getMessage(), ioe);
                } catch (ServletException se) {
                    this.log(se.getMessage(), se.getRootCause());
                }
                if (redirect) {
                    String uri = req.getRequestURI();
                    // Object pars =
                    // req.getAttribute(Action.ATTR_REDIRECT_PARAMETERS);
                    // if (pars instanceof String) {
                    // uri += "?" + pars;
                    // }
                    resp.sendRedirect(uri);
                }
                return true;
            }
        }

        return false;
    }

    protected Render getRender(HttpServletRequest request) {

        String page = request.getRequestURI();

        // remove trailing slashes
        while (page.endsWith("/")) {
            page = page.substring(0, page.length() - 1);
        }

        // take last part of the name
        int lastSlash = page.lastIndexOf('/');
        if (lastSlash >= 0) {
            page = page.substring(lastSlash + 1);
        }

        Render render = this.renders.get(page);
        return (render == null) ? this.defaultRender : render;
    }

    private String getParameter(HttpServletRequest request, String name) {
        // just get the parameter if not a multipart/form-data POST
        if (!ServletFileUpload.isMultipartContent(new ServletRequestContext(
            request))) {
            return request.getParameter(name);
        }

        // check, whether we alread have the parameters
        Map<String, FileItem[]> params = (Map<String, FileItem[]>) request.getAttribute(Util.ATTR_FILEUPLOAD);
        if (params == null) {
            // parameters not read yet, read now
            // Create a factory for disk-based file items
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(256000);

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(-1);

            // Parse the request
            params = new HashMap<String, FileItem[]>();
            try {
                List /* FileItem */items = upload.parseRequest(request);
                for (Iterator ii = items.iterator(); ii.hasNext();) {
                    FileItem fi = (FileItem) ii.next();
                    FileItem[] current = params.get(fi.getFieldName());
                    if (current == null) {
                        current = new FileItem[] { fi };
                    } else {
                        FileItem[] newCurrent = new FileItem[current.length + 1];
                        System.arraycopy(current, 0, newCurrent, 0,
                            current.length);
                        newCurrent[current.length] = fi;
                        current = newCurrent;
                    }
                    params.put(fi.getFieldName(), current);
                }
            } catch (FileUploadException fue) {
                // TODO: log
            }
            request.setAttribute(Util.ATTR_FILEUPLOAD, params);
        }

        FileItem[] param = params.get(name);
        if (param != null) {
            for (int i = 0; i < param.length; i++) {
                if (param[i].isFormField()) {
                    return param[i].getString();
                }
            }
        }

        // no valid string parameter, fail
        return null;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        this.componentContext = context;

        Dictionary config = this.componentContext.getProperties();

        // get authentication details
        String realm = this.getProperty(config, PROP_REALM,
            "Sling Management Console");
        String userId = this.getProperty(config, PROP_USER_NAME, null);
        String password = this.getProperty(config, PROP_PASSWORD, null);

        // get the web manager root path
        this.webManagerRoot = this.getProperty(config, PROP_MANAGER_ROOT, "/sling");
        if (!this.webManagerRoot.startsWith("/")) {
            this.webManagerRoot = "/" + this.webManagerRoot;
        }

        // register the servlet and resources
        try {
            HttpContext httpContext = new SlingHttpContext(this.httpService, realm,
                userId, password);

            // rest of sling
            this.httpService.registerServlet(this.webManagerRoot, this, config,
                httpContext);
            this.httpService.registerResources(this.webManagerRoot + "/res", "/res",
                httpContext);

        } catch (Exception e) {
            this.logService.log(LogService.LOG_ERROR, "Problem setting up", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        this.httpService.unregister(this.webManagerRoot + "/res");
        this.httpService.unregister(this.webManagerRoot);

        this.componentContext = null;

        // simply remove all operations, we should not be used anymore
        this.defaultRender = null;
        this.operations.clear();
        this.renders.clear();
    }

    protected void bindOperation(Action operation) {
        this.operations.put(operation.getName(), operation);
    }

    protected void unbindOperation(Action operation) {
        this.operations.remove(operation.getName());
    }

    protected void bindRender(Render render) {
        this.renders.put(render.getName(), render);

        if (this.defaultRender == null) {
            this.defaultRender = render;
        }
    }

    protected void unbindRender(Render render) {
        this.renders.remove(render.getName());

        if (this.defaultRender == render) {
            if (this.renders.isEmpty()) {
                this.defaultRender = null;
            } else {
                this.defaultRender = this.renders.values().iterator().next();
            }
        }
    }

    /**
     * Returns the named property from the configuration. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    private String getProperty(Dictionary config, String name, String def) {
        Object value = config.get(name);
        if (value instanceof String) {
            return (String) value;
        }

        if (value == null) {
            return def;
        }

        return String.valueOf(value);
    }
}
