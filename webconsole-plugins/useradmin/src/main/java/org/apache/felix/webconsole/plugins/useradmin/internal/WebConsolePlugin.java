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
package org.apache.felix.webconsole.plugins.useradmin.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

class WebConsolePlugin extends SimpleWebConsolePlugin
{
    private static final long serialVersionUID = -3551087958597824593L;

    private static final String LABEL = "users"; //$NON-NLS-1$
    private static final String TITLE = "%role.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = { "/" + LABEL + "/res/plugin.css" }; //$NON-NLS-1$ //$NON-NLS-2$

    private final UserAdmin userAdmin;

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    WebConsolePlugin(UserAdmin userAdmin)
    {
        super(LABEL, TITLE, CSS);
        this.userAdmin = userAdmin;

        // load templates
        TEMPLATE = readTemplateFile("/res/plugin.html"); //$NON-NLS-1$
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(HttpServletRequest, HttpServletResponse)
     */
    protected final void renderContent(HttpServletRequest req,
        HttpServletResponse response) throws ServletException, IOException
    {
        response.getWriter().print(TEMPLATE);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        resp.setContentType("application/json"); //$NON-NLS-1$
        resp.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
        final PrintWriter out = resp.getWriter();
        final JSONWriter jw = new JSONWriter(out);
        final String action = req.getParameter("action"); //$NON-NLS-1$

        final String role = req.getParameter("role"); //$NON-NLS-1$
        final String group = req.getParameter("group"); //$NON-NLS-1$

        try
        {
            if ("addMember".equals(action)) { //$NON-NLS-1$
                final Role xrole = userAdmin.getRole(role);
                final Group xgroup = (Group) userAdmin.getRole(group);
                xgroup.addMember(xrole);
                toJSON(jw, xgroup, false);
            }
            else if ("addRequiredMember".equals(action)) { //$NON-NLS-1$
                final Role xrole = userAdmin.getRole(role);
                final Group xgroup = (Group) userAdmin.getRole(group);
                xgroup.addRequiredMember(xrole);
                toJSON(jw, xgroup, false);
            }
            else if ("removeMember".equals(action)) { //$NON-NLS-1$
                final Role xrole = userAdmin.getRole(role);
                final Group xgroup = (Group) userAdmin.getRole(group);
                xgroup.removeMember(xrole);
                toJSON(jw, xgroup, false);
            }
            else if ("del".equals(action)) { //$NON-NLS-1$
                out.print(userAdmin.removeRole(role));
            }
            else if ("get".equals(action)) { //$NON-NLS-1$
                final Role xrole = userAdmin.getRole(role);
                toJSON(jw, xrole, true);
            }
            else if ("set".equals(action)) { //$NON-NLS-1$
                final String dataRaw = req.getParameter("data"); //$NON-NLS-1$
                final JSONObject data = new JSONObject(dataRaw);
                Role xrole = userAdmin.getRole(data.getString("name")); //$NON-NLS-1$
                if (null == xrole)
                {
                    xrole = userAdmin.createRole(//
                        data.getString("name"), //$NON-NLS-1$
                        data.getInt("type")); //$NON-NLS-1$
                }
                doSetData(xrole, data);
                out.print(true);
            }
            else
            // list all roles without details
            {

                Role[] roles = userAdmin.getRoles(null);
                toJSON(jw, roles, false);
            }
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }

    private static final void doSetData(Role role, JSONObject data) throws JSONException
    {
        putProps(role.getProperties(), data.optJSONObject("properties")); //$NON-NLS-1$
        if (role instanceof User)
        {
            putProps(((User) role).getCredentials(), data.optJSONObject("credentials")); //$NON-NLS-1$
        }
    }

    private static final void putProps(Dictionary dest, JSONObject props)
        throws JSONException
    {
        // clear the old properties
        if (!dest.isEmpty())
        {
            for (Enumeration e = dest.keys(); e.hasMoreElements();)
            {
                dest.remove(e.nextElement());
            }
        }
        // it's empty - don't process it at all
        if (props == null || props.length() == 0)
        {
            return;
        }
        // append the new one
        for (Iterator i = props.keys(); i.hasNext();)
        {
            Object key = i.next();
            Object val = props.get((String) key);

            if (val instanceof JSONArray)
            {
                val = toArray((JSONArray) val);
            }
            dest.put(key, val);
        }
    }

    private static final byte[] toArray(JSONArray array) throws JSONException
    {
        final byte[] ret = new byte[array.length()];
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = (byte) (array.getInt(i) & 0xff);
        }
        return ret;
    }

    private static final void toJSON(JSONWriter jw, Role role, boolean details)
        throws JSONException
    {
        jw.object();
        jw.key("type"); //$NON-NLS-1$
        jw.value(role.getType());
        jw.key("name"); //$NON-NLS-1$
        jw.value(role.getName());

        if (role instanceof Group)
        {
            final Group group = (Group) role;
            Role[] roles;

            roles = group.getMembers();
            if (null != roles && roles.length > 0)
            {
                jw.key("members"); //$NON-NLS-1$
                toJSON(jw, roles, details);
            }

            roles = group.getRequiredMembers();
            if (null != roles && roles.length > 0)
            {
                jw.key("rmembers"); //$NON-NLS-1$
                toJSON(jw, roles, details);
            }
        }

        if (details)
        {
            Dictionary p;
            p = role.getProperties();
            if (null != p && !p.isEmpty())
            {
                jw.key("properties"); //$NON-NLS-1$
                toJSON(jw, p);
            }
            if (role instanceof User)
            {
                p = ((User) role).getCredentials();
                if (null != p && !p.isEmpty())
                {
                    jw.key("credentials"); //$NON-NLS-1$
                    toJSON(jw, p);
                }
            }
        }

        jw.endObject();
    }

    private static final void toJSON(JSONWriter jw, Dictionary props)
        throws JSONException
    {
        jw.object();
        for (Enumeration e = props.keys(); e.hasMoreElements();)
        {
            final Object key = e.nextElement();
            final Object val = props.get(key);
            jw.key((String) key);
            jw.value(val);
        }
        jw.endObject();
    }

    private static final void toJSON(JSONWriter jw, Role[] roles, boolean details)
        throws JSONException
    {
        jw.array();
        for (int i = 0; roles != null && i < roles.length; i++)
        {
            toJSON(jw, roles[i], details);
        }
        jw.endArray();
    }
}
