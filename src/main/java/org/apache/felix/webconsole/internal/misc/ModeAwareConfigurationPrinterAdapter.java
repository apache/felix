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

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.ModeAwareConfigurationPrinter;


/**
 * Adapter for a service acting as mode aware configuration printer.
 */
public class ModeAwareConfigurationPrinterAdapter
    extends ConfigurationPrinterAdapter
    implements ModeAwareConfigurationPrinter
{
    public ModeAwareConfigurationPrinterAdapter(final Object service,
            final String title,
            final Method printMethod)
    {
        super(service, title, printMethod);
    }

    public void printConfiguration(final PrintWriter printWriter)
    {
        printConfiguration(printWriter, ConfigurationPrinter.MODE_ALWAYS);
    }

    public void printConfiguration(final PrintWriter printWriter, final String mode)
    {
        invoke(new Object[] {printWriter, mode});
    }
}
