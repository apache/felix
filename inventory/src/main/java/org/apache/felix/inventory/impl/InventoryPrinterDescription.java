/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.inventory.impl;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.Format;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Helper class for a configuration printer.
 */
public class InventoryPrinterDescription implements Comparable
{

    private final ServiceReference reference;

    private final Format[] formats;

    private final String name;

    private final String title;

    private final String sortKey;

    public InventoryPrinterDescription(final ServiceReference ref)
    {
        this.reference = ref;

        // check formats
        Format[] formats = null;
        final Object formatsCfg = ref.getProperty(InventoryPrinter.FORMAT);
        if (formatsCfg instanceof String)
        {
            final Format format = Format.valueOf((String) formatsCfg);
            if (format != null)
            {
                formats = new Format[]
                    { format };
            }
        }
        else if (formatsCfg instanceof String[])
        {
            final String[] formatsCfgArray = (String[]) formatsCfg;
            final ArrayList formatList = new ArrayList();
            for (int i = 0; i < formatsCfgArray.length; i++)
            {
                final Format format = Format.valueOf(formatsCfgArray[i]);
                if (format != null)
                {
                    formatList.add(format);
                }
            }
            if (!formatList.isEmpty())
            {
                formats = (Format[]) formatList.toArray(new Format[formatList.size()]);
            }
        }

        // check name
        final String name;
        if (ref.getProperty(InventoryPrinter.NAME) != null)
        {
            name = ref.getProperty(InventoryPrinter.NAME).toString();
        }
        else
        {
            name = "InventoryPrinter." + ref.getProperty(Constants.SERVICE_ID);
        }

        // check title
        final String title;
        String sortKey = null;
        if (ref.getProperty(InventoryPrinter.TITLE) != null)
        {
            title = ref.getProperty(InventoryPrinter.TITLE).toString();
            if (title.startsWith("%"))
            {
                sortKey = title.substring(1);
            }
        }
        else
        {
            title = name;
        }

        // cleanup
        if (formats == null)
        {
            formats = new Format[]
                { Format.TEXT };
        }
        if (sortKey == null)
        {
            sortKey = title;
        }

        // set fields
        this.formats = formats;
        this.name = name;
        this.title = title;
        this.sortKey = sortKey;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String getSortKey()
    {
        return this.sortKey;
    }

    public String getName()
    {
        return this.name;
    }

    public Format[] getFormats()
    {
        return this.formats;
    }

    public ServiceReference getServiceReference()
    {
        return this.reference;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object spa)
    {
        return this.reference.compareTo(((InventoryPrinterDescription) spa).reference);
    }

    public boolean equals(final Object obj)
    {
        return this.reference.equals(obj);
    }

    public int hashCode()
    {
        return this.reference.hashCode();
    }

    public String toString()
    {
        return "InventoryPrinterDescription [title=" + title + ", name=" + name + ", formats=" + Arrays.asList(formats)
            + ", sortKey=" + sortKey + "]";
    }
}
