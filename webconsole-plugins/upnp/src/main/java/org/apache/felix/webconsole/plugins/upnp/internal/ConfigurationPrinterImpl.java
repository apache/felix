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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Prints the available UPnP devices
 *
 */
class ConfigurationPrinterImpl implements InventoryPrinter, Constants
{

    private final ServiceTracker tracker;

    ConfigurationPrinterImpl(ServiceTracker tracker)
    {
        this.tracker = tracker;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter
     *   #print(java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    public void print(PrintWriter pw, Format format, boolean isZip)
    {

        TreeMap componentMap = new TreeMap();

        ServiceReference[] refs = tracker.getServiceReferences();
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            ServiceReference ref = refs[i];
            if (null != ref.getProperty(UPnPDevice.UDN)) // make sure device is valid
            {
                // order components by friendly name
                componentMap.put(nameOf(ref).toString() + ref.getProperty(SERVICE_ID),
                    ref);
            }
        }

        if (Format.JSON.equals(format))
        {
            try
            {
                printJSON(componentMap, pw);
            }
            catch (IOException e)
            {
                printText(componentMap, pw);
            }
        }
        else
        {
            printText(componentMap, pw);
        }
    }

    private void printJSON(TreeMap componentMap, PrintWriter pw) throws IOException
    {
        final JSONWriter writer = new JSONWriter(pw);
        writer.object();

        writer.key("devices");
        writer.array();

        // render components
        for (Iterator ci = componentMap.values().iterator(); ci.hasNext();)
        {
            final ServiceReference ref = (ServiceReference) ci.next();
            final UPnPDevice device = (UPnPDevice) tracker.getService(ref);
            if (device != null)
            {
                writer.value(Serializer.deviceToJSON(ref, device));
            }
        }
        writer.endArray();

        writer.endObject();
        writer.flush();
    }

    private void printText(TreeMap componentMap, PrintWriter pw)
    {

        if (componentMap.isEmpty())
        {
            pw.println("Status: No UPnP devices found");
            return;
        }

        pw.println("Status: " + componentMap.size() + " UPnP devices found");
        pw.println();

        // render components
        for (Iterator ci = componentMap.values().iterator(); ci.hasNext();)
        {
            print(pw, (ServiceReference) ci.next());
        }
    }

    private static final Object nameOf(ServiceReference ref)
    {
        Object name = ref.getProperty(UPnPDevice.FRIENDLY_NAME);
        if (null == name)
        {
            name = ref.getProperty(UPnPDevice.UDN);
        }
        return name;
    }

    private void print(PrintWriter pw, ServiceReference ref)
    {

        pw.println(nameOf(ref));
        pw.println("  Properties:");

        String[] properties = ref.getPropertyKeys();
        Arrays.sort(properties);
        for (int i = 0; i < properties.length; i++)
        {
            String key = properties[i];
            if (OBJECTCLASS.equals(key) || SERVICE_ID.equals(key))
            {
                continue;
            }
            Object val = ref.getProperty(key);
            val = WebConsoleUtil.toString(val);
            pw.print("    "); //$NON-NLS-1$
            pw.print(key);
            pw.print(" = "); //$NON-NLS-1$
            pw.println(val);
        }

        UPnPDevice dev = (UPnPDevice) tracker.getService(ref);
        if (null == dev)
            return;

        UPnPService services[] = dev.getServices();
        if (null != services && services.length > 0)
        {
            pw.println("  Services:");
            for (int i = 0; i < services.length; i++)
            {
                print(pw, services[i]);
            }
        }

        pw.println();
    }

    private void print(PrintWriter pw, UPnPService serv)
    {
        pw.print("    Service: ");
        pw.print(serv.getId());
        pw.print(", ver ");
        pw.print(serv.getVersion());
        pw.print(" (type = ");
        pw.print(serv.getType());
        pw.println(')');
        UPnPAction[] actions = serv.getActions();
        if (actions != null && actions.length > 0)
        {
            pw.print("    Actions: ");
            for (int i = 0; i < actions.length; i++)
            {
                pw.print(actions[i].getName());
                if (i < actions.length - 1)
                {
                    pw.print(", ");
                }
            }
            pw.println();
        }

        UPnPStateVariable[] vars = serv.getStateVariables();
        if (vars != null && vars.length > 0)
        {
            pw.println("    State Vars:");
            for (int i = 0; i < vars.length; i++)
            {
                print(pw, vars[i]);
            }
        }
    }

    private void print(PrintWriter pw, UPnPStateVariable var)
    {
        pw.print("       ");
        pw.print(var.getName());
        pw.print(" (");
        pw.print(var.getUPnPDataType());
        pw.print(')');
        if (var.sendsEvents())
        {
            pw.print(", sends events");
        }
        if (null != var.getMinimum())
        {
            pw.print(", min = ");
            pw.print(var.getMinimum());
        }
        if (null != var.getMaximum())
        {
            pw.print(", max = ");
            pw.print(var.getMaximum());
        }
        if (null != var.getStep())
        {
            pw.print(", step = ");
            pw.print(var.getStep());
        }
        pw.println();
    }



}
