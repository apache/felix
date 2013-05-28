/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.inventory.impl.webconsole;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.inventory.Format;
import org.osgi.framework.ServiceReference;

/**
 * Helper class for a configuration printer.
 */
public class ConfigurationPrinterAdapter
{

    private final Object printer;
    public String title;
    public String label;
    private final String[] modes;
    private final boolean escapeHtml;
    private final Method printMethod;
    private final Method attachmentMethod;

    private static final List CUSTOM_MODES = new ArrayList();
    static
    {
        CUSTOM_MODES.add(ConsoleConstants.MODE_TXT);
        CUSTOM_MODES.add(ConsoleConstants.MODE_WEB);
        CUSTOM_MODES.add(ConsoleConstants.MODE_ZIP);
    }

    /**
     * Check whether the class implements the configuration printer.
     * This is done manually to avoid having the configuration printer class
     * available.
     */
    private static boolean isConfigurationPrinter(final Class clazz)
    {
        final Class[] interf = clazz.getInterfaces();
        for (int i = 0; i < interf.length; i++)
        {
            if (interf[i].getName().equals(ConsoleConstants.INTERFACE_CONFIGURATION_PRINTER))
            {
                return true;
            }
        }
        if (clazz.getSuperclass() != null)
        {
            return isConfigurationPrinter(clazz.getSuperclass());
        }
        return false;
    }

    /**
     * Try to create a new configuration printer adapter.
     */
    public static ConfigurationPrinterAdapter createAdapter(final Object service, final ServiceReference ref)
    {
        String title;
        Object modes = null;
        if (isConfigurationPrinter(service.getClass()))
        {
            modes = ref.getProperty(ConsoleConstants.CONFIG_PRINTER_MODES);
            if (modes == null)
            {
                modes = ref.getProperty(ConsoleConstants.PROPERTY_MODES);
            }
            final Method titleMethod = getMethod(service.getClass(), "getTitle", null, false);
            if (titleMethod == null)
            {
                return null;
            }
            title = (String) invoke(service, titleMethod, null);
        }
        else
        {
            modes = ref.getProperty(ConsoleConstants.CONFIG_PRINTER_MODES);
            title = (String) ref.getProperty(ConsoleConstants.PLUGIN_TITLE);
        }

        Object cfgPrinter = null;
        Method printMethod = null;

        // first: printConfiguration(PrintWriter, String)
        final Method method2Params = getMethod(service.getClass(), "printConfiguration", new Class[]
            { PrintWriter.class, String.class }, true);
        if (method2Params != null)
        {
            cfgPrinter = service;
            printMethod = method2Params;
        }

        if (cfgPrinter == null)
        {
            // second: printConfiguration(PrintWriter)
            final Method method1Params = getMethod(service.getClass(), "printConfiguration", new Class[]
                { PrintWriter.class }, true);
            if (method1Params != null)
            {
                cfgPrinter = service;
                printMethod = method1Params;
            }
        }

        if (cfgPrinter != null)
        {
            final Object label = ref.getProperty(ConsoleConstants.PLUGIN_LABEL);
            // check escaping
            boolean webUnescaped;
            Object ehObj = ref.getProperty(ConsoleConstants.CONFIG_PRINTER_WEB_UNESCAPED);
            if (ehObj instanceof Boolean)
            {
                webUnescaped = ((Boolean) ehObj).booleanValue();
            }
            else if (ehObj instanceof String)
            {
                webUnescaped = Boolean.valueOf((String) ehObj).booleanValue();
            }
            else
            {
                webUnescaped = false;
            }

            final String[] modesArray;
            // check modes
            if (modes == null || !(modes instanceof String || modes instanceof String[]))
            {
                modesArray = null;
            }
            else
            {
                if (modes instanceof String)
                {
                    if (CUSTOM_MODES.contains(modes))
                    {
                        modesArray = new String[]
                            { modes.toString() };
                    }
                    else
                    {
                        modesArray = null;
                    }
                }
                else
                {
                    final String[] values = (String[]) modes;
                    boolean valid = values.length > 0;
                    for (int i = 0; i < values.length; i++)
                    {
                        if (!CUSTOM_MODES.contains(values[i]))
                        {
                            valid = false;
                            break;
                        }
                    }
                    if (valid)
                    {
                        modesArray = values;
                    }
                    else
                    {
                        modesArray = null;
                    }
                }
            }

            return new ConfigurationPrinterAdapter(cfgPrinter, printMethod, getMethod(cfgPrinter.getClass(),
                "getAttachments", new Class[]
                    { String.class }, true), title, (label instanceof String ? (String) label : null), modesArray,
                !webUnescaped);
        }
        return null;
    }

    private ConfigurationPrinterAdapter(final Object printer, final Method printMethod, final Method attachmentMethod,
        final String title, final String label, final String[] modesArray, final boolean escapeHtml)
    {
        this.printer = printer;
        this.title = title;
        this.label = label;
        this.escapeHtml = escapeHtml;
        this.printMethod = printMethod;
        this.attachmentMethod = attachmentMethod;
        this.modes = modesArray;
    }

    /**
     * Map the modes to inventory printer modes
     */
    public String[] getPrinterModes()
    {
        final Set list = new HashSet();
        if (this.match(ConsoleConstants.MODE_TXT) || this.match(ConsoleConstants.MODE_ZIP))
        {
            list.add(Format.TEXT.toString());
        }
        if (this.match(ConsoleConstants.MODE_WEB))
        {
            if (!escapeHtml)
            {
                list.add(Format.HTML.toString());
            }
            else
            {
                list.add(Format.TEXT.toString());
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    private boolean match(final String mode)
    {
        if (this.modes == null)
        {
            return true;
        }
        for (int i = 0; i < this.modes.length; i++)
        {
            if (this.modes[i].equals(mode))
            {
                return true;
            }
        }
        return false;
    }

    public final void printConfiguration(final PrintWriter pw, final String mode)
    {
        if (printMethod.getParameterTypes().length > 1)
        {
            invoke(this.printer, this.printMethod, new Object[]
                { pw, mode });
        }
        else
        {
            invoke(this.printer, this.printMethod, new Object[]
                { pw });
        }
    }

    public URL[] getAttachments()
    {
        // check if printer implements binary configuration printer
        URL[] attachments = null;
        if (attachmentMethod != null)
        {
            attachments = (URL[]) invoke(printer, attachmentMethod, new Object[]
                { ConsoleConstants.MODE_ZIP });
        }
        return attachments;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return title + " (" + printer.getClass() + ")";
    }

    private static Method getMethod(final Class clazz, final String mName, final Class[] params, final boolean declaredByClass)
    {
        try
        {
            if (declaredByClass)
            {
                final Method m = clazz.getDeclaredMethod(mName, params);
                if (Modifier.isPublic(m.getModifiers()))
                {
                    return m;
                }
            }

            return clazz.getMethod(mName, params);
       }
        catch (Throwable nsme)
        {
            // ignore, we catch Throwable above to not only catch
            // NoSuchMethodException
            // but also other ones like ClassDefNotFoundError etc.
        }
        return null;
    }

    /**
     * Invoke the method on the printer with the arguments.
     */
    private static Object invoke(final Object obj, final Method m, final Object[] args)
    {
        try
        {
            return m.invoke(obj, args);
        }
        catch (final Throwable e)
        {
            // ignore
        }
        return null;
    }
}
