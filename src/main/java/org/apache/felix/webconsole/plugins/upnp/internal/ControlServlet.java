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
package org.apache.felix.webconsole.plugins.upnp.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This class handles requests from the Web Interface. It is separated from
 * the WebConsolePlugin just to improve readability. This servlet actually
 * is not registered in HTTP service.
 */
public class ControlServlet extends HttpServlet implements ServiceTrackerCustomizer
{

    private static final SimpleDateFormat DATA_FORMAT = new SimpleDateFormat(
        "EEE, d MMM yyyy HH:mm:ss Z"); //$NON-NLS-1$

    final HashMap icons = new HashMap(10);
    final HashMap sessions = new HashMap(10);

    private ServiceTracker tracker;
    private final BundleContext bc;

    private static final long LAST_MODIFIED = System.currentTimeMillis();

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

        String udn = request.getParameter("icon"); //$NON-NLS-1$

        if (udn != null)
        {
            UPnPIcon icon = (UPnPIcon) icons.get(udn);
            if (icon == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            else
            {
                if (request.getDateHeader("If-Modified-Since") > 0) //$NON-NLS-1$
                {
                    // if it is already in cache - don't bother to go further
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
                else
                {
                    // enable caching
                    response.setDateHeader("Last-Modified", LAST_MODIFIED); //$NON-NLS-1$

                    InputStream in = icon.getInputStream();
                    if (null == in)
                    { // this is buggy implementations
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    String mime = icon.getMimeType();
                    if (mime != null)
                        response.setContentType(mime);
                    OutputStream out = response.getOutputStream();

                    int size = icon.getSize();
                    if (size > 0)
                        response.setContentLength(size);

                    // can't use buffer, because it's might block if reading byte[]
                    int read;
                    while (-1 != (read = in.read()))
                        out.write(read);
                }
            }
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            JSONObject json = new JSONObject();

            String method = request.getParameter("action"); //$NON-NLS-1$

            if ("listDevices".equals(method)) //$NON-NLS-1$
            {
                getSession(request).unsubscribe();

                ServiceReference[] refs = tracker.getServiceReferences();
                // add root devices only
                for (int i = 0; refs != null && i < refs.length; i++)
                {
                    if (refs[i] != null
                        && refs[i].getProperty(UPnPDevice.PARENT_UDN) == null)
                    {
                        JSONObject deviceJSON = deviceTreeToJSON(refs[i]);
                        if (null != deviceJSON)
                        {
                            json.append("devices", deviceJSON); //$NON-NLS-1$
                        }
                    }
                }
            }
            else if ("serviceDetails".equals(method)) //$NON-NLS-1$
            {
                UPnPService service = requireService(request);
                SessionObject session = getSession(request)//
                .subscribe(require("udn", request), service.getId()); //$NON-NLS-1$

                json = serviceToJSON(service, session);
            }
            else if ("invokeAction".equals(method)) //$NON-NLS-1$
            {
                UPnPService service = requireService(request);
                UPnPAction action = service.getAction(require("actionID", request)); //$NON-NLS-1$

                json = invoke(action, //
                    request.getParameterValues("names"), //$NON-NLS-1$
                    request.getParameterValues("vals")); //$NON-NLS-1$
            }
            else
            {
                throw new ServletException("Invalid action: " + method);
            }

            response.setContentType("application/json"); //$NON-NLS-1$
            response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
            response.getWriter().print(json.toString(2));
        }
        catch (ServletException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException(e.toString());
        }
    }

    private final SessionObject getSession(HttpServletRequest request)
    {
        String sessionID = request.getSession().getId();
        SessionObject ret = (SessionObject) sessions.get(sessionID);
        if (ret == null)
        {
            ret = new SessionObject(bc, sessionID, sessions);
            request.getSession().setAttribute("___upnp.session.object", ret); //$NON-NLS-1$
        }
        return ret;
    }

    private static final String require(String name, HttpServletRequest request)
        throws ServletException
    {
        String value = request.getParameter(name);
        if (value == null)
            throw new ServletException("missing parameter: " + name);
        return value;
    }

    private final UPnPService requireService(HttpServletRequest request)
        throws ServletException
    {
        String deviceUdn = require("udn", request); //$NON-NLS-1$
        String serviceUrn = require("urn", request); //$NON-NLS-1$

        UPnPDevice device = getDevice(deviceUdn);
        return getService(device, serviceUrn);
    }

    private final JSONObject deviceTreeToJSON(ServiceReference ref) throws JSONException
    {
        UPnPDevice device = (UPnPDevice) tracker.getService(ref);
        Object[] refs = tracker.getServiceReferences();

        Object parentUdn = ref.getProperty(UPnPDevice.UDN);
        if (parentUdn == null)
        {
            plugin.log(LogService.LOG_ERROR,
                "Invalid device, no UDN property specified for " + device);
            return null;
        }

        JSONObject json = deviceToJSON(ref, device);

        // add child devices
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            ref = (ServiceReference) refs[i];

            Object parent = ref.getProperty(UPnPDevice.PARENT_UDN);
            Object currentUDN = ref.getProperty(UPnPDevice.UDN);
            if (parent == null)
            { // no parent
                continue;
            }
            else if (currentUDN != null && currentUDN.equals(parent))
            { // self ?
                continue;
            }
            else if (parentUdn.equals(parent))
            {
                device = (UPnPDevice) tracker.getService(ref);
                JSONObject deviceJSON = deviceTreeToJSON(ref);
                if (null != deviceJSON)
                {
                    json.append("children", deviceJSON); //$NON-NLS-1$
                }
            }
        }
        return json;
    }

    private static final JSONObject deviceToJSON(ServiceReference ref, UPnPDevice device)
        throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("icon", device.getIcons(null) != null); //$NON-NLS-1$

        // add properties
        String[] props = ref.getPropertyKeys();
        JSONObject _props = new JSONObject();
        for (int i = 0; props != null && i < props.length; i++)
        {
            _props.put(props[i], ref.getProperty(props[i]));
        }
        json.put("props", _props); //$NON-NLS-1$

        UPnPService[] services = device.getServices();
        for (int i = 0; services != null && i < services.length; i++)
        {
            json.append("services", services[i].getType()); //$NON-NLS-1$
        }

        return json;
    }

    private static final JSONObject serviceToJSON(UPnPService service,
        SessionObject session) throws JSONException
    {
        JSONObject json = new JSONObject();

        // add service properties
        json.put("type", service.getType()); //$NON-NLS-1$
        json.put("id", service.getId()); //$NON-NLS-1$

        // add state variables
        UPnPStateVariable[] vars = service.getStateVariables();
        for (int i = 0; vars != null && i < vars.length; i++)
        {
            Object value = null;
            if (vars[i] instanceof UPnPLocalStateVariable)
            {
                value = ((UPnPLocalStateVariable) vars[i]).getCurrentValue();
            }

            if (value == null)
                value = session.getValue(vars[i].getName());
            if (value == null)
                value = "---"; //$NON-NLS-1$

            json.append("variables", new JSONObject() // //$NON-NLS-1$
            .put("name", vars[i].getName()) // //$NON-NLS-1$
            .put("value", value) // //$NON-NLS-1$
            .put("defalt", vars[i].getDefaultValue()) // //$NON-NLS-1$
            .put("min", vars[i].getMinimum()) // //$NON-NLS-1$
            .put("max", vars[i].getMaximum()) // //$NON-NLS-1$
            .put("step", vars[i].getStep()) // //$NON-NLS-1$
            .put("allowed", vars[i].getAllowedValues()) // //$NON-NLS-1$
            .put("sendsEvents", vars[i].sendsEvents()) // //$NON-NLS-1$
            );
        }

        // add actions
        UPnPAction[] actions = service.getActions();
        for (int i = 0; actions != null && i < actions.length; i++)
        {
            json.append("actions", actionToJSON(actions[i])); //$NON-NLS-1$
        }

        return json;
    }

    private static final JSONObject actionToJSON(UPnPAction action) throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("name", action.getName()); //$NON-NLS-1$
        String[] names = action.getInputArgumentNames();
        for (int i = 0; names != null && i < names.length; i++)
        {
            UPnPStateVariable variable = action.getStateVariable(names[i]);
            json.append("inVars", new JSONObject()// //$NON-NLS-1$
            .put("name", names[i])// //$NON-NLS-1$
            .put("type", variable.getUPnPDataType())); //$NON-NLS-1$
        }

        return json;

    }

    private static final JSONObject invoke(UPnPAction action, String[] names,
        String[] vals) throws Exception
    {
        JSONObject json = new JSONObject();

        // check input arguments
        Hashtable inputArgs = null;
        if (names != null && vals != null && names.length > 0
            && names.length == vals.length)
        {
            inputArgs = new Hashtable(names.length);
            for (int i = 0; i < names.length; i++)
            {
                final UPnPStateVariable var = action.getStateVariable(names[i]);
                final String upnpType = var.getUPnPDataType();
                final Object argObj;
                if (UPnPStateVariable.TYPE_STRING.equals(upnpType))
                {
                    argObj = vals[i];
                }
                else if (UPnPStateVariable.TYPE_CHAR.equals(upnpType))
                {
                    argObj = new Character(vals[i].charAt(0));
                }
                else if (UPnPStateVariable.TYPE_BIN_BASE64.equals(upnpType))
                {
                    argObj = Base64.decodeBase64(vals[i]);
                }
                else if (UPnPStateVariable.TYPE_BIN_HEX.equals(upnpType))
                {
                    argObj = Hex.decode(vals[i]);
                }
                else
                {
                    Class javaType = var.getJavaDataType();
                    Constructor constructor = javaType.getConstructor(new Class[] { String.class });
                    argObj = constructor.newInstance(new Object[] { vals[i] });
                }

                inputArgs.put(names[i], argObj);
            }
        }

        // invoke
        Dictionary out = action.invoke(inputArgs);

        // prepare output arguments
        if (out != null && out.size() > 0)
        {
            for (Enumeration e = out.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                UPnPStateVariable var = action.getStateVariable(key);

                Object value = out.get(key);
                if (value instanceof Date)
                {
                    synchronized (DATA_FORMAT)
                    {
                        value = DATA_FORMAT.format((Date) value);
                    }
                }
                else if (value instanceof byte[])
                {
                    value = hex((byte[]) value);
                }

                json.append("output", new JSONObject() // //$NON-NLS-1$
                .put("name", key)// //$NON-NLS-1$
                .put("type", var.getUPnPDataType()) // //$NON-NLS-1$
                .put("value", value)); //$NON-NLS-1$
            }
        }
        return json;
    }

    private static final String hex(byte[] data)
    {
        if (data == null)
            return "null"; //$NON-NLS-1$
        StringBuffer sb = new StringBuffer(data.length * 3);
        synchronized (sb)
        {
            for (int i = 0; i < data.length; i++)
            {
                sb.append(Integer.toHexString(data[i] & 0xff)).append('-');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private final UPnPDevice getDevice(String udn)
    {
        ServiceReference[] refs = tracker.getServiceReferences();
        String _udn;
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            _udn = (String) refs[i].getProperty(UPnPDevice.UDN);
            if (_udn != null && _udn.equals(udn))
            {
                return (UPnPDevice) tracker.getService(refs[i]);
            }
        }

        throw new IllegalArgumentException("Device '" + udn + "' not found!");
    }

    private final UPnPService getService(UPnPDevice device, String urn)
    {
        UPnPService[] services = device.getServices();
        for (int i = 0; services != null && i < services.length; i++)
        {
            if (services[i].getType().equals(urn))
            {
                return services[i];
            }
        }

        throw new IllegalArgumentException("Service '" + urn + "' not found!");
    }

    private final WebConsolePlugin plugin;

    /**
     * Creates new XML-RPC handler.
     * 
     * @param bc the bundle context
     * @param iconServlet the icon servlet.
     */
    ControlServlet(BundleContext bc, ServiceTracker tracker, WebConsolePlugin plugin)
    {
        this.bc = bc;
        this.tracker = tracker;
        this.plugin = plugin;
    }

    /**
     * Cancels the scheduled timers
     */
    void close()
    {
        icons.clear();
        for (Iterator i = sessions.values().iterator(); i.hasNext();)
        {
            ((SessionObject) i.next()).unsubscribe();
        }
        sessions.clear();
    }

    /* ---------- BEGIN SERVICE TRACKER */
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void modifiedService(ServiceReference ref, Object serv)
    {/* unused */
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void removedService(ServiceReference ref, Object serv)
    {
        icons.remove(ref.getProperty(UPnPDevice.UDN));
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public final Object addingService(ServiceReference ref)
    {
        UPnPDevice device = (UPnPDevice) bc.getService(ref);

        UPnPIcon[] _icons = device.getIcons(null);
        if (_icons != null && _icons.length > 0)
        {
            icons.put(ref.getProperty(UPnPDevice.UDN), _icons[0]);
        }

        return device;
    }

}
