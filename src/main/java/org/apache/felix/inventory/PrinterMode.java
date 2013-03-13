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
package org.apache.felix.inventory;

/**
 * Enumeration for the different printer modes.
 */
public final class PrinterMode
{

    // plain text
    public static PrinterMode TEXT = new PrinterMode("TEXT");

    // valid HTML fragment (no external references)
    public static PrinterMode HTML_FRAGMENT = new PrinterMode("HTML_FRAGMENT");

    // JSON output
    public static PrinterMode JSON = new PrinterMode("JSON");

    private final String mode;

    private PrinterMode(final String mode)
    {
        this.mode = mode;
    }

    public static PrinterMode valueOf(final String m)
    {
        if (TEXT.name().equalsIgnoreCase(m))
        {
            return TEXT;
        }
        else if (HTML_FRAGMENT.name().equalsIgnoreCase(m))
        {
            return HTML_FRAGMENT;
        }
        else if (JSON.name().equalsIgnoreCase(m))
        {
            return JSON;
        }
        return null;
    }

    public String name()
    {
        return this.mode;
    }

    public String toString()
    {
        return name();
    }
}
