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
package org.apache.felix.scr.impl.logger;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;

/**
 * The <code>ComponentLogger</code> interface defines a simple API to enable some logging
 * for a component. This avoids avoids that all clients doing logging on behalf of
 * a component need to pass in things like {@code ComponentMetadata} or the component Id.
 */
public class ComponentLogger
{
    private final String name;

    private final BundleLogger parent;

    private volatile String prefix;

    public ComponentLogger(final ComponentMetadata metadata, final BundleLogger parent)
    {
        if ( metadata.getName() != null )
        {
            this.name = metadata.getName();
        }
        else if ( metadata.getImplementationClassName() != null )
        {
            this.name = "implementation class " + metadata.getImplementationClassName();
        }
        else
        {
            this.name = "UNKNOWN";
        }
        this.parent = parent;
        this.setComponentId(-1);
    }

    /**
     * Update the logger with the correct component id.
     * @param id The component id
     */
    public void setComponentId(final long id)
    {
        if ( id > -1 )
        {
            prefix = "[" + name + "(" + id + ")] : ";
        }
        else
        {
            prefix = "[" + name + "] : ";
        }
    }

    /**
     * Log a message using the {@code BundleLogger} and prefix the message
     * with information about the component.
     *
     * @param level The log level
     * @param message The message
     * @param ex Optional exception
     */
    public void log(final int level, final String message, final Throwable ex)
    {
        if ( parent.isLogEnabled(level) )
        {
            parent.log(level,
                    prefix + message,
                    ex);
        }
    }

    /**
     * Log a message using the {@code BundleLogger} and prefix the message
     * with information about the component.
     *
     * @param level The log level
     * @param pattern The pattern
     * @param ex Optional exception
     * @param arguments Optional arguments for the pattern
     */
    public void log(final int level, final String pattern, final Throwable ex, final Object... arguments)
    {
        if ( parent.isLogEnabled(level) )
        {
            parent.log(level,
                    prefix + pattern,
                    ex,
                    arguments);
        }
    }

    /**
     * Returns {@code true} if logging for the given level is enabled.
     * @return If the {@code BundleLogger} returns {@code true}.
     */
    public boolean isLogEnabled(final int level)
    {
        return parent.isLogEnabled(level);
    }
}
