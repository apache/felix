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

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

class Serializer
{

    private Serializer()
    {
        // prevent instantiation
    }

    static final JSONObject deviceToJSON(ServiceReference ref, UPnPDevice device)
        throws JSONException
    {
        final JSONObject json = new JSONObject();
        json.put("icon", device.getIcons(null) != null); //$NON-NLS-1$

        // add properties
        final String[] props = ref.getPropertyKeys();
        final JSONObject _props = new JSONObject();
        for (int i = 0; props != null && i < props.length; i++)
        {
            _props.put(props[i], ref.getProperty(props[i]));
        }
        json.put("props", _props); //$NON-NLS-1$

        final UPnPService[] services = device.getServices();
        for (int i = 0; services != null && i < services.length; i++)
        {
            json.append("services", services[i].getType()); //$NON-NLS-1$
        }

        return json;
    }

    static final JSONObject serviceToJSON(UPnPService service,
        SessionObject session) throws JSONException
    {
        final JSONObject json = new JSONObject();

        // add service properties
        json.put("type", service.getType()); //$NON-NLS-1$
        json.put("id", service.getId()); //$NON-NLS-1$

        // add state variables
        final UPnPStateVariable[] vars = service.getStateVariables();
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

            json.append("variables", variableToJSON(vars[i], vars[i].getName()) //$NON-NLS-1$
            .put("value", value));// //$NON-NLS-1$
        }

        // add actions
        final UPnPAction[] actions = service.getActions();
        for (int i = 0; actions != null && i < actions.length; i++)
        {
            json.append("actions", actionToJSON(actions[i])); //$NON-NLS-1$
        }

        return json;
    }

    static final JSONObject variableToJSON(final UPnPStateVariable var,
        final String name) throws JSONException
    {
        return new JSONObject()//
        .put("name", name) // //$NON-NLS-1$
        .put("default", var.getDefaultValue()) // //$NON-NLS-1$
        .put("min", var.getMinimum()) //$NON-NLS-1$
        .put("max", var.getMaximum()) //$NON-NLS-1$
        .put("step", var.getStep()) //$NON-NLS-1$
        .put("allowed", var.getAllowedValues()) //$NON-NLS-1$
        .put("sendsEvents", var.sendsEvents()) //$NON-NLS-1$
        .put("type", var.getUPnPDataType()); //$NON-NLS-1$
    }

    static final JSONObject actionToJSON(UPnPAction action) throws JSONException
    {
        final JSONObject json = new JSONObject();
        json.put("name", action.getName()); //$NON-NLS-1$
        final String[] names = action.getInputArgumentNames();
        for (int i = 0; names != null && i < names.length; i++)
        {
            final UPnPStateVariable variable = action.getStateVariable(names[i]);
            json.append("inVars", variableToJSON(variable, names[i])); //$NON-NLS-1$
        }

        return json;

    }
}
