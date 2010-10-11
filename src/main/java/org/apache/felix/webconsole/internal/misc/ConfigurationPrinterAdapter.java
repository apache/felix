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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.webconsole.ConfigurationPrinter;


/**
 * Adapter for a service acting as configuration printer.
 */
public class ConfigurationPrinterAdapter implements ConfigurationPrinter
{

    private final Object service;

    private final String title;

    private final Method printMethod;

    public ConfigurationPrinterAdapter(final Object service,
            final String title,
            final Method printMethod)
    {
        this.title = title;
        this.service = service;
        this.printMethod = printMethod;
    }

    public String getTitle()
    {
        return this.title;
    }

    protected void invoke(final Object[] args)
    {
        try
        {
            printMethod.invoke(service, args);
        }
        catch (IllegalArgumentException e)
        {
            // ignore
        }
        catch (IllegalAccessException e)
        {
            // ignore
        }
        catch (InvocationTargetException e)
        {
            // ignore
        }
    }

    public void printConfiguration(final PrintWriter printWriter)
    {
        invoke(new Object[] {printWriter});
    }
}
