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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.utils.json.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class handles requests from the Web Interface. It is separated from
 * the WebConsolePlugin just to improve readability. This servlet actually
 * is not registered in HTTP service.
 */
public final class ControlServlet extends HttpServlet
{

    private static final long serialVersionUID = -5789642544511401813L;

    private static final SimpleDateFormat DATA_FORMAT = new SimpleDateFormat(
        "EEE, d MMM yyyy HH:mm:ss Z"); //$NON-NLS-1$

    private final HashMap/*<String,UPnPDevice>*/ devices = new HashMap(10);
    private final HashMap/*<String,UPnPIcon>*/ icons = new HashMap(10);
    // holds lock for the devices & icons cache above
    private final Object cacheLock = new Object();

    private final Map/*<String,SessionObject>*/ sessions = Collections.synchronizedMap(new HashMap(10));

    private final ServiceTracker tracker;
    private final BundleContext bc;

    private static final long LAST_MODIFIED = System.currentTimeMillis();

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

        String udn = request.getParameter("icon"); //$NON-NLS-1$

        if (udn != null)
        {
            UPnPIcon icon = getIcon(udn);
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
    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            Map<String, Object> json = new HashMap<String, Object>();

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
                        Map<String, Object> deviceJSON = deviceTreeToJSON(refs[i]);
                        if (null != deviceJSON)
                        {
                            @SuppressWarnings("unchecked")
                            List<Object> list = (List<Object>) json.get("devices"); //$NON-NLS-1$
                            if ( list == null )
                            {
                                list = new ArrayList<Object>();
                                json.put("devices", list); //$NON-NLS-1$
                            }
                            list.add(deviceJSON);
                        }
                    }
                }
            }
            else if ("serviceDetails".equals(method)) //$NON-NLS-1$
            {
                UPnPService service = requireService(request);
                SessionObject session = getSession(request)//
                .subscribe(require("udn", request), service.getId()); //$NON-NLS-1$

                json = Serializer.serviceToJSON(service, session);
            }
            else if ("invokeAction".equals(method)) //$NON-NLS-1$
            {
                UPnPService service = requireService(request);
                UPnPAction action = service.getAction(require("actionID", request)); //$NON-NLS-1$

                String[] names = request.getParameterValues("names"); //$NON-NLS-1$
                if (null == names)
                {
                  names = request.getParameterValues("names[]"); //$NON-NLS-1$
                }
                String[] vals = request.getParameterValues("vals"); //$NON-NLS-1$
                if (null == vals)
                {
                  vals = request.getParameterValues("vals[]"); //$NON-NLS-1$
                }

                json = invoke(action, names, vals);
            }
            else
            {
                throw new ServletException("Invalid action: " + method);
            }

            response.setContentType("application/json"); //$NON-NLS-1$
            response.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
            final JSONWriter writer = new JSONWriter(response.getWriter());
            writer.value(json);
            writer.flush();
        }
        catch (ServletException e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain"); //$NON-NLS-1$
            e.printStackTrace(response.getWriter());
            response.flushBuffer();
        }
        catch (Exception e)
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain"); //$NON-NLS-1$
            e.printStackTrace(response.getWriter());
            response.flushBuffer();
        }
    }

    private final SessionObject getSession(HttpServletRequest request)
    {
        final String sessionID = request.getSession().getId();
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
        final String value = request.getParameter(name);
        if (value == null)
            throw new ServletException("missing parameter: " + name);
        return value;
    }

    private final UPnPService requireService(HttpServletRequest request)
        throws ServletException
    {
        final String deviceUdn = require("udn", request); //$NON-NLS-1$
        final String serviceUrn = require("urn", request); //$NON-NLS-1$

        final UPnPDevice device = getDevice(deviceUdn);
        return getService(device, serviceUrn);
    }

    private final Map<String, Object> deviceTreeToJSON(ServiceReference ref)
    {
        final UPnPDevice device = (UPnPDevice) tracker.getService(ref);
        if (null == device)
        {
            return null; // the device is dynamically removed
        }

        final Object parentUdn = ref.getProperty(UPnPDevice.UDN);
        if (parentUdn == null)
        {
            plugin.log(LogService.LOG_ERROR,
                "Invalid device, no UDN property specified for " + device);
            return null;
        }

        final Map<String, Object> json = Serializer.deviceToJSON(ref, device);

        // add child devices
        final Object[] refs = tracker.getServiceReferences();
        if ( refs != null )
        {
            List<Object> children = new ArrayList<Object>();
            json.put("children", children); //$NON-NLS-1$
            for (int i = 0; i < refs.length; i++)
            {
                ref = (ServiceReference) refs[i];

                final Object parent = ref.getProperty(UPnPDevice.PARENT_UDN);
                final Object currentUDN = ref.getProperty(UPnPDevice.UDN);
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
                    Map<String, Object> deviceJSON = deviceTreeToJSON(ref);
                    if (null != deviceJSON)
                    {
                        children.add(deviceJSON);
                    }
                }
            }
        }
        return json;
    }

    private static final Map<String, Object> invoke(UPnPAction action, String[] names,
        String[] vals) throws Exception
    {
        final Map<String, Object> json = new HashMap<String, Object>();

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
        final Dictionary out = action.invoke(inputArgs);

        // prepare output arguments
        if (out != null && out.size() > 0)
        {
            final Object[] outputs = new Object[out.size()];
            int index = 0;
            for (Enumeration e = out.keys(); e.hasMoreElements();)
            {
                final String key = (String) e.nextElement();
                final UPnPStateVariable var = action.getStateVariable(key);

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
                    value = Hex.encode((byte[]) value);
                }

                final Map<String, Object> output = new HashMap<String, Object>();
                output.put("name", key); // //$NON-NLS-1$
                output.put("type", var.getUPnPDataType()); // //$NON-NLS-1$
                output.put("value", value); //$NON-NLS-1$

                outputs[index] = output;
                index++;
            }
            json.put("output", outputs); // //$NON-NLS-1$
        }
        return json;
    }

    private final void fillCache()
    {
        final ServiceReference[] refs = tracker.getServiceReferences();
        for (int i = 0; i < refs.length; i++)
        {
            final ServiceReference ref = refs[i];
            final Object udn = ref.getProperty(UPnPDevice.UDN);
            if (icons.containsKey(udn))
            {
                continue;
            }

            final UPnPDevice device = (UPnPDevice) bc.getService(ref);
            UPnPIcon icon = null;
            try
            { // Fix for FELIX-4012
                UPnPIcon[] _icons = device == null ? null : device.getIcons(null);
                icon = _icons != null && _icons.length > 0 ? _icons[0] : null;
            }
            catch (IllegalStateException e)
            { // since OSGi r4.3 ignore it
            }
            icons.put(udn, icon);
            devices.put(udn, device);
        }
    }

    private final UPnPIcon getIcon(final String udn)
    {
        synchronized (cacheLock)
        {
          fillCache();
          return (UPnPIcon) icons.get(udn);
        }
    }

    private final UPnPDevice getDevice(String udn)
    {
        final UPnPDevice device;
        synchronized (cacheLock)
        {
          fillCache();
          device = (UPnPDevice) devices.get(udn);
        }
        if (null == device)
        {
            throw new IllegalArgumentException("Device '" + udn + "' not found!");
        }
        return device;
    }

    private static final UPnPService getService(UPnPDevice device, String urn)
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
        synchronized (cacheLock)
        {
          icons.clear();
        }
        synchronized (sessions)
        {
          for (Iterator i = sessions.values().iterator(); i.hasNext();)
          {
              ((SessionObject) i.next()).unsubscribe();
          }
          sessions.clear();
        }
    }

    /* ---------- BEGIN SERVICE TRACKER */
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    final void removedService(ServiceReference ref)
    {
        final Object udn = ref.getProperty(UPnPDevice.UDN);
        synchronized (cacheLock)
        {
          icons.remove(udn);
          devices.remove(udn);
        }
    }

}
