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
package org.apache.felix.inventory.impl.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.impl.InventoryPrinterHandler;

/**
 * Base class for all configuration writers.
 */
public abstract class ConfigurationWriter extends PrintWriter
{

    ConfigurationWriter(final Writer delegatee)
    {
        super(delegatee);
    }

    protected void title(final String title) throws IOException
    {
        // dummy implementation
    }

    protected void end() throws IOException
    {
        // dummy implementation
    }

    public void printInventory(final Format format, final InventoryPrinterHandler handler) throws IOException
    {
        this.title(handler.getTitle());
        handler.print(this, format, false);
        this.end();
    }
}