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
 * The {@code ComponentLogger} is the logger to be used to log on behalf of a component.
 * This avoids avoids that all clients doing logging on behalf of a component need to
 * pass in things like {@code ComponentMetadata} or the component Id.
 */
public class ComponentLogger extends AbstractLogger
{
    private final String name;

    private final String className;

    private final BundleLogger parent;

    private volatile int trackingCount = -3;

    private volatile InternalLogger currentLogger;

    public ComponentLogger(final ComponentMetadata metadata, final BundleLogger parent)
    {
        super(parent.getConfiguration(), ""); // we set the prefix later
        this.parent = parent;
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
        if ( metadata.getImplementationClassName() != null )
        {
            this.className = metadata.getImplementationClassName();
        }
        else
        {
            this.className = null;
        }
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
            this.setPrefix(this.parent.getPrefix() + "[" + name + "(" + id + ")] : ");
        }
        else
        {
            this.setPrefix(this.parent.getPrefix() + "[" + name + "] : ");
        }
    }

    @Override
    InternalLogger getLogger()
    {
        if ( this.trackingCount < this.parent.getTrackingCount() )
        {
            this.currentLogger = this.parent.getLogger(this.className);
            this.trackingCount = this.parent.getTrackingCount();
        }
        return currentLogger;
    }

}
