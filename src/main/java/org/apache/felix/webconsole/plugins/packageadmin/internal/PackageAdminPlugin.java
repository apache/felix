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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("serial")
public class PackageAdminPlugin extends GenericServlet {

    static final String LABEL = "pkgadmin";

    static final String TITLE = "Package Admin";

    private static final Comparator<ExportedPackage> EXPORT_PACKAGE_COMPARATOR = new ExportedPackageComparator();

    private final BundleContext bundleContext;

    private final ServiceTracker pkgAdminTracker;

    PackageAdminPlugin(final BundleContext bundleContext,
            final ServiceTracker pkgAdminTracker) {
        this.bundleContext = bundleContext;
        this.pkgAdminTracker = pkgAdminTracker;
    }

    public void service(ServletRequest req, ServletResponse res)
            throws IOException {

        final PrintWriter pw = ((HttpServletResponse) res).getWriter();

        final PackageAdmin pa = (PackageAdmin) this.pkgAdminTracker.getService();
        if (pa == null) {
            printStatLine(pw, "PackageAdmin Service not registered");
            return;
        }

        try {
            Map<String, Set<ExportedPackage>> exports = collectExportedPackages(pa);
            printStatLine(pw,
                "PackageAdmin service reports %d exported packages",
                exports.size());
            dumpDuplicates(pw, exports);
        } catch (Exception e) {
            pw.println("failure ...." + e);
            e.printStackTrace(pw);
        }
    }

    private Map<String, Set<ExportedPackage>> collectExportedPackages(
            final PackageAdmin pa) {
        Map<String, Set<ExportedPackage>> exports = new TreeMap<String, Set<ExportedPackage>>();

        for (Bundle bundle : this.bundleContext.getBundles()) {
            final ExportedPackage[] bundleExports = pa.getExportedPackages(bundle);
            if (bundleExports != null) {
                for (ExportedPackage exportedPackage : bundleExports) {
                    Set<ExportedPackage> exportSet = exports.get(exportedPackage.getName());
                    if (exportSet == null) {
                        exportSet = new TreeSet<ExportedPackage>(
                            EXPORT_PACKAGE_COMPARATOR);
                        exports.put(exportedPackage.getName(), exportSet);
                    }
                    exportSet.add(exportedPackage);
                }
            }
        }

        return exports;
    }

    private void printStatLine(final PrintWriter pw, final String format,
            final Object... args) {
        final String message = String.format(format, args);
        pw.printf("<p class=\"statline\">%s</p>%n", message);
    }

    private void printTitle(final PrintWriter pw, final String format,
            final Object... args) {
        pw.println("<div class=\"ui-widget-header ui-corner-top\">");
        pw.printf(format, args);
        pw.println("</div>");
    }

    private void dumpDuplicates(final PrintWriter pw,
            final Map<String, Set<ExportedPackage>> exports) {
        printTitle(pw, "Duplicate Exported Packages");
        pw.println("<table class=\"nicetable\">");
        pw.println("<tr><th>Package</th><th>Exports</th><th>Imports</th></tr>");
        for (Entry<String, Set<ExportedPackage>> exportEntry : exports.entrySet()) {
            Set<ExportedPackage> exportSet = exportEntry.getValue();
            if (exportSet.size() > 1) {
                String firstCol = String.format("<td rowspan=\"%s\">%s</td>",
                    exportSet.size(), exportEntry.getKey());
                for (ExportedPackage exportedPackage : exportSet) {
                    Bundle[] importers = exportedPackage.getImportingBundles();
                    pw.printf("<tr>%s<td>version=%s, Bundle %s</td><td>",
                        firstCol, exportedPackage.getVersion(),
                        exportedPackage.getExportingBundle());
                    if (importers != null && importers.length > 0) {
                        for (Bundle bundle : importers) {
                            pw.printf("%s<br>", bundle);
                        }
                    } else {
                        pw.print("&nbsp;");
                    }
                    pw.println("</td></tr>");
                    firstCol = "";
                }
            }
        }
        pw.println("</table>");
    }

    private void dumpDuplicatesAsTxt(final PrintWriter pw,
            final Map<String, Set<ExportedPackage>> exports)
    {
        pw.println("Duplicate Exported Packages");
        pw.println("---------------------------");
        final List<String[]> lines = new ArrayList<String[]>();
        lines.add(new String[] {"Package", "Exports", "Imports"});

        for (Entry<String, Set<ExportedPackage>> exportEntry : exports.entrySet())
        {
            final Set<ExportedPackage> exportSet = exportEntry.getValue();
            if (exportSet.size() > 1)
            {
                String firstCol = exportEntry.getKey();
                for (ExportedPackage exportedPackage : exportSet)
                {
                    final Bundle[] importers = exportedPackage.getImportingBundles();
                    final String secondCol = "version=" + exportedPackage.getVersion() + ", Bundle " + exportedPackage.getExportingBundle();
                    if (importers != null && importers.length > 0) {
                        boolean first = true;
                        for (Bundle bundle : importers)
                        {
                            if ( first )
                            {
                                lines.add(new String[] {firstCol, secondCol, bundle.toString()});
                                first = false;
                            }
                            else
                            {
                                lines.add(new String[] {"", "", bundle.toString()});

                            }
                        }
                    }
                    else
                    {
                        lines.add(new String[] {firstCol, secondCol, ""});
                    }
                    firstCol = "";
                }
            }
        }
        int maxFirst = 0, maxSecond = 0;
        for(final String[] entry : lines)
        {
            if ( entry[0].length() > maxFirst )
            {
                maxFirst = entry[0].length();
            }
            if ( entry[1].length() > maxSecond )
            {
                maxSecond = entry[1].length();
            }
        }
        maxFirst += 2;
        maxSecond += 2;
        for(final String[] entry : lines)
        {
            padText(pw, entry[0], maxFirst);
            padText(pw, entry[1], maxSecond);
            pw.println(entry[2]);
        }
    }

    private void padText(final PrintWriter pw, final String text, final int length)
    {
        pw.print(text);
        final int padLength = length - text.length();
        for(int i=0;i<padLength;i++) {
            pw.print(' ');
        }
    }

    private static class ExportedPackageComparator implements
            Comparator<ExportedPackage> {

        public int compare(ExportedPackage o1, ExportedPackage o2) {
            if (o1 == o2) {
                return 0;
            }

            int name = o1.getName().compareTo(o2.getName());
            if (name != 0) {
                return name;
            }

            int version = o1.getVersion().compareTo(o2.getVersion());
            if (version != 0) {
                return version;
            }

            return Long.valueOf(o1.getExportingBundle().getBundleId()).compareTo(
                o2.getExportingBundle().getBundleId());
        }

    }

    /**
     * Configuration printer
     */
    public void printConfiguration(final PrintWriter pw)
    {
        final PackageAdmin pa = (PackageAdmin) this.pkgAdminTracker.getService();
        if (pa == null)
        {
            pw.println("PackageAdmin Service not registered");
            return;
        }

        try {
            Map<String, Set<ExportedPackage>> exports = collectExportedPackages(pa);

            pw.print("PackageAdmin service reports ");
            pw.print(String.valueOf(exports.size()));
            pw.println(" exported packages.");
            pw.println();

            dumpDuplicatesAsTxt(pw, exports);
        } catch (Exception e) {
            pw.println("failure ...." + e);
        }
    }
}
