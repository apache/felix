/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole.internal.misc;

import java.io.PrintWriter;

import org.apache.felix.webconsole.ModeAwareConfigurationPrinter;
import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;

/**
 * This class provides the Threads tab in the configuration status.
 */
public class ThreadPrinter extends AbstractConfigurationPrinter implements ModeAwareConfigurationPrinter
{

    private static final String TITLE = "Threads";

    private static final String LABEL = "_threads";

    private final ThreadDumper dumper = new ThreadDumper();

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public String getTitle()
    {
        return TITLE;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw)
    {
        printConfiguration(pw, MODE_TXT);
    }

    /**
     * @see ModeAwareConfigurationPrinter#printConfiguration(java.io.PrintWriter, java.lang.String)
     */
    public void printConfiguration(PrintWriter pw, String mode)
    {

        dumper.printThreads(pw, MODE_ZIP.equals(mode));
    }

}
