/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl;

import java.io.PrintWriter;

import org.apache.felix.service.command.Descriptor;

/**
 * The <code>ScrGogoCommand</code> implements the Apache Felix Gogo Shell
 * commands for the Apache Felix Declarative Services implementations. Supported
 * commands are:
 * <dl>
 * <dt><code>scr:list</code></dt>
 * <dd>List all components or components of a specific bundle</dd>
 * <dt><code>scr:info</code></dt>
 * <dd>Print full information of a single component</dd>
 * <dt><code>scr:enable</code></dt>
 * <dd>Enable a component</dd>
 * <dt><code>scr:disable</code></dt>
 * <dd>Disable a component</dd>
 * <dt><code>scr:config</code></dt>
 * <dd>Print configuration of the Apache Felix Declarative Services bundle</dd>
 * </dl>
 * <p>
 * This class uses Java 5 annotations to provide descriptions for the commands
 * and arguments. Thus the class must be compiled with Java 5 enabled and thus
 * the commands will only be available in Java 5 or higher VMs.
 */
class ScrGogoCommand
{

    // The actual implementation of the commands
    private final ScrCommand scrCommand;

    // this constructor is public to ease the reflection based implementation
    // See ScrCommand.register for fully disclosure of mechanics
    public ScrGogoCommand(final ScrCommand scrCommand)
    {
        this.scrCommand = scrCommand;
    }

    @Descriptor("List all component configurations")
    public void list()
    {
        try
        {
            scrCommand.list(null, new PrintWriter(System.out));
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println(e.getMessage());
        }
    }

    @Descriptor("List component configurations of a specific bundle")
    public void list(@Descriptor("Symbolic name or ID of the bundle") final String bundleIdentifier)
    {
        try
        {
            scrCommand.list(bundleIdentifier, new PrintWriter(System.out));
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println(e.getMessage());
        }
    }

    @Descriptor("Dump information of a component or component configuration")
    public void info(@Descriptor("Name of the component or ID of the component configuration") final String componentIdentifier)
    {
        try
        {
            scrCommand.info(componentIdentifier, new PrintWriter(System.out));
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println(e.getMessage());
        }
    }

    @Descriptor("Enable a disabled component")
    public void enable(@Descriptor("Name of the component") final String componentIdentifier)
    {
        try
        {
            scrCommand.change(componentIdentifier, new PrintWriter(System.out), true);
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println(e.getMessage());
        }
    }

    @Descriptor("Disable an enabled component")
    public void disable(@Descriptor("Name of the component") final String componentIdentifier)
    {
        try
        {
            scrCommand.change(componentIdentifier, new PrintWriter(System.out), false);
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println(e.getMessage());
        }
    }

    @Descriptor("Show the current SCR configuration")
    public void config()
    {
        scrCommand.config(new PrintWriter(System.out));
    }

}
