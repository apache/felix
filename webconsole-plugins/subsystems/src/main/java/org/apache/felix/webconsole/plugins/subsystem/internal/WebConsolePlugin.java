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
package org.apache.felix.webconsole.plugins.subsystem.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

public class WebConsolePlugin extends SimpleWebConsolePlugin
{
    private static final long serialVersionUID = 4329827842860201817L;
    private static final String UNABLE_TO_FIND_TARGET_SUBSYSTEM = "Unable to find target subsystem";

    private static final String LABEL = "subsystems";
    private static final String TITLE = "Subsystems";
    private static final String CATEGORY = "OSGi";
    private static final String CSS[] = { "/res/ui/bundles.css" }; // yes, it's correct!
    private static final String RES = "/" + LABEL + "/res/";

    private final BundleContext bundleContext;
    private final String template;

    public WebConsolePlugin(BundleContext bc)
    {
        super(LABEL, TITLE, CSS);
        bundleContext = bc;

        template = readTemplateFile("/res/plugin.html");
    }

    @Override
    public String getCategory()
    {
        return CATEGORY;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String path = request.getPathInfo();
        // don't process if this is request to load a resource
        if (!path.startsWith(RES))
        {
            RequestInfo reqInfo = new RequestInfo(request);
            if (reqInfo.extension.equals("json"))
            {
                renderResult(response, reqInfo);

                // nothing more to do
                return;
            }
        }
        super.doGet(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String action = WebConsoleUtil.getParameter(req, "action");
        if ("install".equals(action))
        {
            installSubsystem(req);

            if (req.getRequestURI().endsWith("/install")) {
                // just send 200/OK, no content
                resp.setContentLength( 0 );
            } else {
                // redirect to URL
                resp.sendRedirect(req.getRequestURI());
            }

            return;
        }
        else
        {
            boolean success = false;
            RequestInfo reqInfo = new RequestInfo(req);
            if ("start".equals(action))
            {
                startSubsystem(reqInfo.id);
                success = true;
            }
            else if ("stop".equals(action))
            {
                stopSubsystem(reqInfo.id);
                success = true;
            }
            else if ("uninstall".equals(action))
            {
                uninstallSubsystem(reqInfo.id);
                success = true;
            }
            else
            {
                super.doPost(req, resp);
            }

            if (success)
            {
                renderResult(resp, reqInfo);
            }
        }
    }

    private void installSubsystem(HttpServletRequest req) throws IOException
    {
        @SuppressWarnings("rawtypes")
        Map params = (Map) req.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );

        final boolean start = getParameter(params, "subsystemstart") != null;
        FileItem[] subsystemItems = getFileItems(params, "subsystemfile");

        for (final FileItem subsystemItem : subsystemItems)
        {
            File tmpFile = null;
            try
            {
                // copy the data to a file for better processing
                tmpFile = File.createTempFile("installSubsystem", ".tmp");
                subsystemItem.write(tmpFile);
            }
            catch (Exception e)
            {
                log(LogService.LOG_ERROR, "Problem accessing uploaded subsystem file: " + subsystemItem.getName(), e);

                // remove the temporary file
                if (tmpFile != null)
                {
                    tmpFile.delete();
                    tmpFile = null;
                }
            }

            if (tmpFile != null)
            {
                final File file = tmpFile;
                // TODO support install in other subsystems than the root one
                // TODO currently this means that when installing more than one subsystem they
                // will be installed concurrently. Not sure if this is the best idea.
                // However the client UI does not support selecting more than one file, so
                // from a practical point of view this is currently not an issue.
                asyncSubsystemOperation(0, new SubsystemOperation()
                {
                    @Override
                    public void exec(Subsystem ss)
                    {
                        try
                        {
                            InputStream is = new FileInputStream(file);
                            try
                            {
                                Subsystem nss = ss.install("inputstream:" + subsystemItem.getName(), is);
                                if (start)
                                    nss.start();
                            }
                            finally
                            {
                                is.close();
                                file.delete();
                            }
                        }
                        catch (IOException e)
                        {
                            log(LogService.LOG_ERROR, "Problem installing subsystem", e);
                        }
                    }
                });
            }
        }
    }

    private void startSubsystem(long id) throws IOException
    {
        asyncSubsystemOperation(id, new SubsystemOperation()
        {
            // Lamba, where art thou. So close yet so far away...
            @Override
            public void exec(Subsystem ss)
            {
                ss.start();
            }
        });
    }

    private void stopSubsystem(long id) throws IOException
    {
        asyncSubsystemOperation(id, new SubsystemOperation()
        {
            @Override
            public void exec(Subsystem ss)
            {
                ss.stop();
            }
        });
    }

    private void uninstallSubsystem(long id) throws IOException
    {
        asyncSubsystemOperation(id, new SubsystemOperation()
        {
            @Override
            public void exec(Subsystem ss)
            {
                ss.uninstall();
            }
        });
    }

    private void asyncSubsystemOperation(long id, final SubsystemOperation op) throws IOException
    {
        try
        {
            Collection<ServiceReference<Subsystem>> refs =
                    bundleContext.getServiceReferences(Subsystem.class,
                            "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=" + id + ")");

            if (refs.size() < 1)
                throw new IOException(UNABLE_TO_FIND_TARGET_SUBSYSTEM);

            final ServiceReference<Subsystem> ref = refs.iterator().next();
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    Subsystem ss = bundleContext.getService(ref);
                    try
                    {
                        op.exec(ss);
                    }
                    finally
                    {
                        bundleContext.ungetService(ref);
                    }
                }
            }).start();
        }
        catch (InvalidSyntaxException e)
        {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private FileItem getParameter(Map params, String name)
    {
        FileItem[] items = (FileItem[]) params.get(name);
        if (items != null)
        {
            for (int i = 0; i < items.length; i++)
            {
                if (items[i].isFormField())
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }

    @SuppressWarnings("rawtypes")
    private FileItem[] getFileItems(Map params, String name)
    {
        final List<FileItem> files = new ArrayList<FileItem>();
        FileItem[] items = (FileItem[]) params.get(name);
        if (items != null)
        {
            for (int i = 0; i < items.length; i++)
            {
                if (!items[i].isFormField() && items[i].getSize() > 0)
                {
                    files.add(items[i]);
                }
            }
        }

        return files.toArray(new FileItem[files.size()]);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        RequestInfo reqInfo = getRequestInfo(req);

        StringWriter w = new StringWriter();
        PrintWriter w2 = new PrintWriter(w);
        renderResult(w2, reqInfo);

        // prepare variables
        DefaultVariableResolver vars = ((DefaultVariableResolver) WebConsoleUtil.getVariableResolver(req));
        vars.put("__data__", w.toString());

        res.getWriter().print(template);
    }

    private void renderResult(HttpServletResponse response, RequestInfo reqInfo) throws IOException
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        renderResult(response.getWriter(), reqInfo);
    }

    private void renderResult(PrintWriter pw, RequestInfo reqInfo) throws IOException
    {
        JSONWriter jw = new JSONWriter(pw);
        try
        {
            jw.object();

            List<Subsystem> subsystems = getSubsystems();

            jw.key("status");
            jw.value(subsystems.size());

            jw.key("data");
            jw.array();
            for (Subsystem ss : subsystems)
            {
                subsystem(jw, ss);
            }
            jw.endArray();

            jw.endObject();
        }
        catch (JSONException je)
        {
            throw new IOException(je);
        }
    }

    private void subsystem(JSONWriter jw, Subsystem ss) throws JSONException
    {
        jw.object();

        jw.key("id");
        jw.value(ss.getSubsystemId());
        jw.key("name");
        jw.value(ss.getSymbolicName());
        jw.key("version");
        jw.value(ss.getVersion());
        jw.key("state");
        jw.value(ss.getState());

        jw.endObject();
    }

    private List<Subsystem> getSubsystems() throws IOException
    {
        try
        {
            List<Subsystem> l = new ArrayList<Subsystem>();
            for (ServiceReference<Subsystem> ref : bundleContext.getServiceReferences(Subsystem.class, null))
            {
                l.add(bundleContext.getService(ref));
            }
            return l;
        }
        catch (InvalidSyntaxException e)
        {
            throw new IOException(e);
        }
    }

    public SimpleWebConsolePlugin register()
    {
        return register(bundleContext);
    }

    static RequestInfo getRequestInfo(HttpServletRequest request)
    {
        return (RequestInfo) request.getAttribute(WebConsolePlugin.class.getName());
    }

    class RequestInfo
    {
        public final long id;
        public final Object extension;

        protected RequestInfo(HttpServletRequest req)
        {
            String info = req.getPathInfo();
            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);

            // get extension
            if (info.endsWith(".json"))
            {
                extension = "json";
                info = info.substring(0, info.length() - 5);
            }
            else
            {
                extension = "html";
            }

            if (info.startsWith("/"))
                info = info.substring(1);

            if ("".equals(info))
                id = -1;
            else
                id = Long.parseLong(info);

            req.setAttribute(WebConsolePlugin.this.getClass().getName(), this);
        }
    }

    interface SubsystemOperation
    {
        void exec(Subsystem ss);
    }
}
