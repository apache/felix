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
package org.apache.felix.webconsole.plugins.packageadmin.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

class WebConsolePrinter implements InventoryPrinter
{

    private final ServiceTracker tracker;
    private final BundleContext bc;

    WebConsolePrinter(BundleContext bc, ServiceTracker tracker)
    {
        this.bc = bc;
        this.tracker = tracker;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(
     *  java.io.PrintWriter, org.apache.felix.inventory.Format, boolean)
     */
    @SuppressWarnings("deprecation")
    public void print(PrintWriter pw, Format format, boolean isZip)
    {
        final PackageAdmin pa = (PackageAdmin) tracker.getService();
        if (pa == null)
        {
            pw.println("Status: PackageAdmin Service not registered");
            return;
        }

        try
        {
            Map<String, Set<ExportedPackage>> exports = WebConsolePlugin.collectExportedPackages(
                pa, bc);

            pw.print("Status: PackageAdmin service reports ");
            pw.print(String.valueOf(exports.size()));
            pw.println(" exported packages.");
            pw.println();

            dumpDuplicatesAsTxt(pw, exports);
        }
        catch (Exception e)
        {
            pw.println("failure ...." + e);
        }
    }

    @SuppressWarnings("deprecation")
    private void dumpDuplicatesAsTxt(final PrintWriter pw,
        final Map<String, Set<ExportedPackage>> exports)
    {
        pw.println("Duplicate Exported Packages");
        pw.println("---------------------------");
        final List<String[]> lines = new ArrayList<String[]>();
        lines.add(new String[] { "Package", "Exports", "Imports" });

        for (Iterator<Entry<String, Set<ExportedPackage>>> entriesIter = exports.entrySet().iterator(); entriesIter.hasNext();)
        {
            Entry<String, Set<ExportedPackage>> exportEntry = entriesIter.next();

            final Set<ExportedPackage> exportSet = exportEntry.getValue();
            if (exportSet.size() > 1)
            {
                String firstCol = exportEntry.getKey();
                for (Iterator<ExportedPackage> packageIter = exportSet.iterator(); packageIter.hasNext();)
                {
                    ExportedPackage exportedPackage = packageIter.next();
                    final Bundle[] importers = exportedPackage.getImportingBundles();
                    final String secondCol = "version=" + exportedPackage.getVersion()
                        + ", Bundle " + exportedPackage.getExportingBundle();
                    if (importers != null && importers.length > 0)
                    {
                        boolean first = true;
                        for (int j = 0; j < importers.length; j++)
                        {
                            final Bundle bundle = importers[j];
                            if (first)
                            {
                                lines.add(new String[] { firstCol, secondCol,
                                        bundle.toString() });
                                first = false;
                            }
                            else
                            {
                                lines.add(new String[] { "", "", bundle.toString() });

                            }
                        }
                    }
                    else
                    {
                        lines.add(new String[] { firstCol, secondCol, "" });
                    }
                    firstCol = "";
                }
            }
        }
        int maxFirst = 0, maxSecond = 0;
        for (int i = 0; i < lines.size(); i++)
        {
            final String[] entry = lines.get(i);
            if (entry[0].length() > maxFirst)
            {
                maxFirst = entry[0].length();
            }
            if (entry[1].length() > maxSecond)
            {
                maxSecond = entry[1].length();
            }
        }
        maxFirst += 2;
        maxSecond += 2;
        for (int i = 0; i < lines.size(); i++)
        {
            final String[] entry = lines.get(i);
            padText(pw, entry[0], maxFirst);
            padText(pw, entry[1], maxSecond);
            pw.println(entry[2]);
        }
    }

    private static final void padText(final PrintWriter pw, final String text,
        final int length)
    {
        pw.print(text);
        final int padLength = length - text.length();
        for (int i = 0; i < padLength; i++)
        {
            pw.print(' ');
        }
    }

    /**
     * @return
     */
    public String getTitle()
    {
        return "Duplicate Exports";
    }

}
