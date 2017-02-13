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

import java.util.HashMap;
import java.util.Map;

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

    static final Map<String, Object> deviceToJSON(ServiceReference ref, UPnPDevice device)
    {
        final Map<String, Object> json = new HashMap<String, Object>();
        json.put("icon", device.getIcons(null) != null); //$NON-NLS-1$

        // add properties
        final String[] props = ref.getPropertyKeys();
        final Map<String, Object> _props = new HashMap<String, Object>();
        for (int i = 0; props != null && i < props.length; i++)
        {
            _props.put(props[i], ref.getProperty(props[i]));
        }
        json.put("props", _props); //$NON-NLS-1$

        final UPnPService[] services = device.getServices();
        if ( services != null )
        {
            final String[] serviceTypes = new String[services.length];
            for (int i = 0; i < services.length; i++)
            {
                serviceTypes[i] = services[i].getType();
            }
            json.put("services", serviceTypes); //$NON-NLS-1$
        }

        return json;
    }

    static final Map<String, Object> serviceToJSON(UPnPService service,
        SessionObject session)
    {
        final Map<String, Object> json = new HashMap<String, Object>();

        // add service properties
        json.put("type", service.getType()); //$NON-NLS-1$
        json.put("id", service.getId()); //$NON-NLS-1$

        // add state variables
        final UPnPStateVariable[] vars = service.getStateVariables();
        if ( vars != null )
        {
            @SuppressWarnings("unchecked")
            Map<String, Object>[] arr = new Map[vars.length];
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

                arr[i] = variableToJSON(vars[i], vars[i].getName());
                arr[i].put("value", value);// //$NON-NLS-1$
            }
            json.put("variables", arr); //$NON-NLS-1$
        }

        // add actions
        final UPnPAction[] actions = service.getActions();
        if ( actions != null )
        {
            Object[] arr = new Object[actions.length];
            for (int i = 0; i < actions.length; i++)
            {
                arr[i] = actionToJSON(actions[i]);
            }
            json.put("actions", arr); //$NON-NLS-1$
        }

        return json;
    }

    static final Map<String, Object> variableToJSON(final UPnPStateVariable var,
        final String name)
    {
        final Map<String, Object> json = new HashMap<String, Object>();
        json.put("name", name); // //$NON-NLS-1$
        json.put("default", var.getDefaultValue()); // //$NON-NLS-1$
        json.put("min", var.getMinimum()); //$NON-NLS-1$
        json.put("max", var.getMaximum()); //$NON-NLS-1$
        json.put("step", var.getStep()); //$NON-NLS-1$
        json.put("allowed", var.getAllowedValues()); //$NON-NLS-1$
        json.put("sendsEvents", var.sendsEvents()); //$NON-NLS-1$
        json.put("type", var.getUPnPDataType()); //$NON-NLS-1$
        return json;
    }

    static final Map<String, Object> actionToJSON(UPnPAction action)
    {
        final Map<String, Object> json = new HashMap<String, Object>();
        json.put("name", action.getName()); //$NON-NLS-1$
        final String[] names = action.getInputArgumentNames();
        if ( names != null )
        {
            Object[] vars = new Object[names.length];
            for (int i = 0; i < names.length; i++)
            {
                final UPnPStateVariable variable = action.getStateVariable(names[i]);
                vars[i] = variableToJSON(variable, names[i]); //$NON-NLS-1$
            }
            json.put("inVars", vars); //$NON-NLS-1$
        }

        return json;

    }
}
