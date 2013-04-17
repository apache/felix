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
 * Java 1.4 compatible enumeration of formats used for inventory printing.
 * <p>
 * {@link InventoryPrinter} services indicate supported formats listing any of
 * these values in their {@link InventoryPrinter#FORMAT} service
 * properties.
 * <p>
 * Requestors of inventory printing indicate the desired output format by
 * specifying the respective constant when calling the
 * {@link InventoryPrinter#print(java.io.PrintWriter, Format, boolean)} method.
 * <p>
 * Round-tripping is guaranteed between the {@link #toString()} and
 * {@link #valueOf(String)} methods.
 */
public final class Format
{

    /**
     * Inventory is printed in plain text format.
     */
    public static Format TEXT = new Format("TEXT");

    /**
     * Inventory is printed in HTML format.
     * <p>
     * Technically the output is expected to be an HTML fragment which is
     * intended to be inserted into any block element, such as {@code <div>},
     * within a HTML {@code <body>}.
     */
    public static Format HTML = new Format("HTML");

    /**
     * Inventory is printed in JSON format.
     * <p>
     * The output is expected to be a valid JSON object. That is, the output
     * must start with an opening curly brace (<code>{</code>) and end with a
     * closing curly brace (<code>}</code>).
     */
    public static Format JSON = new Format("JSON");

    private final String format;

    private Format(final String format)
    {
        this.format = format;
    }

    /**
     * Converts the given {@code format} string into an instance of
     * this class.
     *
     * @param format The string value to be converted into a {@code Format}.
     * @return One of the defined {@code Format} constants or {@code null}.
     */
    public static Format valueOf(final String format)
    {
        if (TEXT.format.equalsIgnoreCase(format))
        {
            return TEXT;
        }
        else if (HTML.format.equalsIgnoreCase(format))
        {
            return HTML;
        }
        else if (JSON.format.equalsIgnoreCase(format))
        {
            return JSON;
        }
        return null;
    }

    /**
     * Returns the string value of this format.
     */
    public String toString()
    {
        return format;
    }
}
