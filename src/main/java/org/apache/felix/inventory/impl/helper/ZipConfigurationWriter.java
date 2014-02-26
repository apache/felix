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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.impl.InventoryPrinterHandler;

/**
 * The ZIP configuration writer creates a zip with
 * - txt output of a inventory printers (if supported)
 * - json output of a inventory printers (if supported)
 * - attachments from a inventory printer (if supported)
 */
public class ZipConfigurationWriter extends ConfigurationWriter
{

    private final ZipConfigurationWriter.ConfigZipOutputStream zip;

    private int entryCounter;

    public static ZipConfigurationWriter create(final OutputStream out) throws IOException
    {
        final ZipConfigurationWriter.ConfigZipOutputStream zip = new ConfigZipOutputStream(out)
        {
        };
        zip.setLevel(Deflater.BEST_SPEED);
        zip.setMethod(ZipOutputStream.DEFLATED);

        return new ZipConfigurationWriter(zip);
    }

    private ZipConfigurationWriter(final ZipConfigurationWriter.ConfigZipOutputStream zip) throws IOException
    {
        super(new OutputStreamWriter(zip, "UTF-8"));

        this.zip = zip;
        this.entryCounter = -1;
    }

    public void finish() throws IOException
    {
        this.zip.finish();
    }

    /**
     * Overwrites the
     * {@link ConfigurationWriter#printInventory(Format, InventoryPrinterHandler)}
     * method writing the plain text output, the JSON output and any
     * attachements to the ZIP file. The {@code format} argument is ignored.
     *
     * @param formatIgnored Ignored, may be {@code null}.
     * @param handler The handler to be called to generate the output
     *
     * @throws IOException if an error occurrs writing to the ZIP file.
     */
    public void printInventory(final Format formatIgnored, final InventoryPrinterHandler handler)
        throws IOException
    {
        final String baseName = getBaseName(handler);

        this.zip.handler(handler);

        // print the plain text output
        if (handler.supports(Format.TEXT))
        {
            final ZipEntry entry = new ZipEntry(baseName.concat(".txt"));
            entry.setTime(System.currentTimeMillis());
            this.zip.putNextEntry(entry, Format.TEXT);
            handler.print(this, Format.TEXT, true);
            this.flush();
            this.zip.closeEntry();
        }

        // print the JSON format output
        if (handler.supports(Format.JSON))
        {
            final ZipEntry entry = new ZipEntry("json/" + baseName + ".json");
            entry.setTime(System.currentTimeMillis());
            this.zip.putNextEntry(entry, Format.JSON);
            handler.print(this, Format.JSON, true);
            this.flush();
            this.zip.closeEntry();
        }

        // any attachements from the handler
        this.zip.attachements();
        handler.addAttachments(this.zip, baseName.concat("/"));
        this.zip.endAttachements();

        this.zip.endHandler();
    }

    private String getBaseName(final InventoryPrinterHandler handler)
    {
        final String title = handler.getTitle();
        final StringBuffer name = new StringBuffer(title.length());
        for (int i = 0; i < title.length(); i++)
        {
            char c = title.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
            {
                name.append(c);
            }
            else
            {
                name.append('_');
            }
        }

        this.entryCounter++;
        return MessageFormat.format("{0,number,000}_{1}", new Object[]
            { new Integer(this.entryCounter), name });
    }

    private static class ConfigZipOutputStream extends ZipOutputStream
    {

        private final SimpleJson json;

        ConfigZipOutputStream(final OutputStream out)
        {
            super(out);

            this.json = new SimpleJson();
            this.json.object();

            // timestamp in the "created" object
            final Date now = new Date();
            final String nowFormatted = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US)
                .format(now);
            this.json.key("created");
            this.json.object();
            this.json.key("date").value(nowFormatted);
            this.json.key("stamp").value(String.valueOf(now.getTime()));
            this.json.endObject();

            // output from the printers in the "files" object
            this.json.key("files").object();
        }

        void handler(final InventoryPrinterHandler handler)
        {
            this.json.key(handler.getName());
            this.json.object();
            this.json.key("title").value(handler.getTitle());
        }

        void endHandler()
        {
            this.json.endObject();
        }

        void attachements()
        {
            this.json.key("attachements");
            this.json.array();
        }

        void endAttachements()
        {
            this.json.endArray();
        }

        void putNextEntry(ZipEntry e, Format format) throws IOException
        {
            this.json.key(format.toString().toLowerCase());
            this.putNextEntry(e);
        }

        public void putNextEntry(ZipEntry e) throws IOException
        {
            this.json.value(e.getName());
            super.putNextEntry(e);
        }

        public void finish() throws IOException
        {
            // end "files" and root objects
            this.json.endObject().endObject();

            final ZipEntry entry = new ZipEntry("index.json");
            entry.setTime(System.currentTimeMillis());
            super.putNextEntry(entry); // don't write the index to the JSON
            this.write(this.json.toString().getBytes("UTF-8"));
            this.flush();
            this.closeEntry();

            super.finish();
        }
    }
}