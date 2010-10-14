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
package org.apache.felix.webconsole.internal.misc;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.webconsole.*;
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
    private Method modeAwarePrintMethod;
    private Method printMethod;
    private Method attachmentMethod;
    private boolean checkedAttachmentMethod = false;

    private static final List CUSTOM_MODES = new ArrayList();
    static
    {
        CUSTOM_MODES.add( ConfigurationPrinter.MODE_TXT );
        CUSTOM_MODES.add( ConfigurationPrinter.MODE_WEB );
        CUSTOM_MODES.add( ConfigurationPrinter.MODE_ZIP );
    }

    public static ConfigurationPrinterAdapter createAdapter(
            final Object service,
            final ServiceReference ref)
    {
        String title;
        Object cfgPrinter = null;
        Object modes = null;
        Method printMethod = null;
        if ( service instanceof ConfigurationPrinter )
        {
            modes = ref.getProperty(WebConsoleConstants.CONFIG_PRINTER_MODES);
            if ( modes == null )
            {
                modes = ref.getProperty( ConfigurationPrinter.PROPERTY_MODES );
            }
            cfgPrinter = service;
            title = ((ConfigurationPrinter) service).getTitle();
        }
        else
        {
            modes = ref.getProperty( WebConsoleConstants.CONFIG_PRINTER_MODES );
            title = (String)ref.getProperty(  WebConsoleConstants.PLUGIN_TITLE );

            // first: printConfiguration(PrintWriter, String)
            final Method method2Params = searchMethod(service, "printConfiguration",
                    new Class[] {PrintWriter.class, String.class});
            if ( method2Params != null )
            {
                cfgPrinter = service;
                printMethod = method2Params;
            }

            if ( cfgPrinter == null )
            {
                // second: printConfiguration(PrintWriter)
                final Method method1Params = searchMethod(service, "printConfiguration",
                        new Class[] {PrintWriter.class});
                if ( method1Params != null )
                {
                    cfgPrinter = service;
                    printMethod = method1Params;
                }
            }
        }
        if ( cfgPrinter != null )
        {
            Object label =  ref.getProperty( WebConsoleConstants.PLUGIN_LABEL);
            boolean webUnescaped;
            Object ehObj = ref.getProperty( WebConsoleConstants.CONFIG_PRINTER_WEB_UNESCAPED );
            if ( ehObj instanceof Boolean )
            {
                webUnescaped = ( ( Boolean ) ehObj ).booleanValue();
            }
            else if ( ehObj instanceof String )
            {
                webUnescaped = Boolean.valueOf( ( String ) ehObj ).booleanValue();
            }
            else
            {
                webUnescaped = false;
            }
            return new ConfigurationPrinterAdapter(
                    cfgPrinter,
                    printMethod,
                    title,
                    (label instanceof String ? (String)label : null),
                    modes,
                    !webUnescaped);
        }
        return null;
    }

    private ConfigurationPrinterAdapter( final Object printer,
            final Method printMethod,
            final String title,
            final String label,
            final Object modes,
            final boolean escapeHtml )
    {
        this.printer = printer;
        this.title = title;
        this.label = label;
        this.escapeHtml = escapeHtml;
        if ( printMethod != null )
        {
            if ( printMethod.getParameterTypes().length > 1 )
            {
                this.modeAwarePrintMethod = printMethod;
            }
            else
            {
                this.printMethod = printMethod;
            }
        }
        if ( modes == null || !( modes instanceof String || modes instanceof String[] ) )
        {
            this.modes = null;
        }
        else
        {
            if ( modes instanceof String )
            {
                if ( CUSTOM_MODES.contains(modes) )
                {
                    this.modes = new String[] {modes.toString()};
                }
                else
                {
                    this.modes = null;
                }
            }
            else
            {
                final String[] values = (String[])modes;
                boolean valid = values.length > 0;
                for(int i=0; i<values.length; i++)
                {
                    if ( !CUSTOM_MODES.contains(values[i]) )
                    {
                        valid = false;
                        break;
                    }
                }
                if ( valid)
                {
                    this.modes = values;
                }
                else
                {
                    this.modes = null;
                }
            }
        }
    }

    public boolean match(final String mode)
    {
        if ( this.modes == null)
        {
            return true;
        }
        for(int i=0; i<this.modes.length; i++)
        {
            if ( this.modes[i].equals(mode) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean escapeHtml()
    {
        return escapeHtml;
    }

    public final void printConfiguration( final PrintWriter pw,
            final String mode )
    {
        if ( printer instanceof ModeAwareConfigurationPrinter )
        {
            ( ( ModeAwareConfigurationPrinter ) printer ).printConfiguration( pw, mode );
        }
        else if ( printer instanceof ConfigurationPrinter )
        {
            ( ( ConfigurationPrinter ) printer ).printConfiguration( pw );
        }
        else if ( this.modeAwarePrintMethod != null )
        {
            this.invoke(this.modeAwarePrintMethod, new Object[] {pw, mode});
        } else if ( this.printMethod != null )
        {
            this.invoke(this.printMethod, new Object[] {pw});
        }
    }

    public URL[] getAttachments( final String mode )
    {
        // check if printer implements binary configuration printer
        URL[] attachments = null;
        if ( printer instanceof AttachmentProvider )
        {
            attachments = ((AttachmentProvider)printer).getAttachments(mode);
        }
        else
        {
            if ( !checkedAttachmentMethod )
            {
                attachmentMethod = searchMethod(printer, "getAttachments", new Class[] {String.class});
                checkedAttachmentMethod = true;
            }
            if ( attachmentMethod != null )
            {
                attachments = (URL[])invoke(attachmentMethod, new Object[] {mode});
            }
        }
        return attachments;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return title + " (" + printer.getClass() + ")";
    }

    /**
     * Search a method with the given name and signature
     */
    private static Method searchMethod(final Object obj, final String mName, final Class[] params)
    {
        try
        {
            return obj.getClass().getDeclaredMethod(mName, params);
        }
        catch (Throwable nsme)
        {
            // ignore, we catch Throwable above to not only catch NoSuchMethodException
            // but also other ones like ClassDefNotFoundError etc.
        }
        return null;
    }

    /**
     * Invoke the method on the printer with the arguments.
     */
    protected Object invoke(final Method m, final Object[] args)
    {
        try
        {
            return m.invoke(printer, args);
        }
        catch (Throwable e)
        {
            // ignore
        }
        return null;
    }
}
