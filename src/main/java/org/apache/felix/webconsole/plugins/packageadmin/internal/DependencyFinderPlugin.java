/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.webconsole.plugins.packageadmin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("serial")
public class DependencyFinderPlugin extends HttpServlet
{

    static final String LABEL = "depfinder";

    static final String TITLE = "Dependency Finder";

    private static final String PARAM_FIND = "plugin.find";

    private static final String PARAM_SUBMIT = "plugin.submit";

    private static final String INDENT = "    ";

    private final BundleContext bundleContext;

    private final ServiceTracker pkgAdminTracker;

    DependencyFinderPlugin(final BundleContext bundleContext, final ServiceTracker pkgAdminTracker)
    {
        this.bundleContext = bundleContext;
        this.pkgAdminTracker = pkgAdminTracker;
    }

    private void drawForm(final PrintWriter pw, String findField)
    {
        titleHtml(pw, "Dependency Finder", "Enter a list of package or class names");
        pw.println("<tr>");
        pw.println("<td>Packages/Classes</td>");
        pw.print("<td colspan='2'>");
        pw.print("<form method='GET'>");
        pw.println("<textarea rows='10' cols='80' name='" + PARAM_FIND + "'>" + (findField != null ? findField : "")
            + "</textarea>");
        pw.println("&nbsp;&nbsp;<input type='submit' name='" + PARAM_SUBMIT + "' value='Find' class='submit'>");
        pw.print("</form>");
        pw.print("</td>");
        pw.println("</tr>");
    }

    private void endTable(final PrintWriter pw)
    {
        pw.println("</table>");
    }

    private void startTable(final PrintWriter pw)
    {
        pw.println("<table class='nicetable'>");
    }

    private void titleHtml(PrintWriter pw, String title, String description)
    {
        pw.println("<tr>");
        pw.println("<th colspan='3'>" + title + "</th>");
        pw.println("</tr>");

        if (description != null)
        {
            pw.println("<tr>");
            pw.println("<td colspan='3'>" + description + "</th>");
            pw.println("</tr>");
        }
    }

    private void printStatLine(final PrintWriter pw, final String format, final Object... args)
    {
        final String message = String.format(format, args);
        pw.printf("<p class=\"statline\">%s</p>%n", message);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        final PrintWriter pw = resp.getWriter();

        final PackageAdmin pa = (PackageAdmin) this.pkgAdminTracker.getService();
        if (pa == null)
        {
            printStatLine(pw, "PackageAdmin Service not registered");
            return;
        }

        printStatLine(pw, "Find exported packages and classes");

        startTable(pw);
        String findField = req.getParameter(PARAM_FIND);
        if (findField != null)
        {
            SortedSet<String> packageNames = getPackageNames(findField);
            if (!packageNames.isEmpty())
            {
                Set<Bundle> exportingBundles = new LinkedHashSet<Bundle>();

                titleHtml(pw, "Packages", "Here are the packages you entered and by which bundle they are exported.");
                for (String packageName : packageNames)
                {
                    pw.println("<tr>");
                    pw.print("<td>");
                    pw.print(packageName);
                    pw.print("</td>");

                    pw.print("<td colspan='2'>");
                    ExportedPackage[] exports = pa.getExportedPackages(packageName);
                    ExportedPackage export = pa.getExportedPackage(packageName);
                    if (export == null)
                    {
                        pw.print("<span style='color: red;'>NOT EXPORTED</span>");
                    }
                    else
                    {
                        Bundle exportingBundle = export.getExportingBundle();
                        pw.printf(
                            "<a href='/system/console/bundles/%s' style='text-decoration: none; color: #555;'>%s (%s)</a>",
                            exportingBundle.getBundleId(), exportingBundle.getSymbolicName(),
                            exportingBundle.getBundleId());
                        if (exports.length > 1)
                        {
                            pw.printf(" and %s others.", exports.length - 1);
                        }
                        exportingBundles.add(exportingBundle);

                    }
                    pw.print("</td>");

                    pw.println("</tr>");
                }

                titleHtml(pw, "Maven Dependencies", "Here are the bundles listed above as Maven dependencies");
                pw.println("<tr>");
                pw.print("<td colspan='3'>");
                pw.println("<pre>");
                for (Bundle bundle : exportingBundles)
                {
                    Enumeration<?> entries = bundle.findEntries("META-INF/maven", "pom.properties", true);
                    if (entries != null)
                    {
                        URL u = (URL) entries.nextElement();
                        java.util.Properties props = new java.util.Properties();
                        InputStream is = null;
                        try
                        {
                            is = u.openStream();
                            props.load(u.openStream());

                            indent(pw, 2, "<dependency>");
                            indent(pw, 3, String.format("<groupId>%s</groupId>", props.get("groupId")));
                            indent(pw, 3, String.format("<artifactId>%s</artifactId>", props.get("artifactId")));
                            indent(pw, 3, String.format("<version>%s</version>", props.get("version")));
                            indent(pw, 3, "<scope>provided</scope>");
                            indent(pw, 2, "</dependency>");
                        }
                        catch (IOException e)
                        {
                        }
                        finally
                        {
                            if (is != null)
                            {
                                is.close();
                            }
                        }
                    }
                }
                pw.print("</pre>");
                pw.print("</td>");
                pw.println("</tr>");
            }
        }
        drawForm(pw, findField);
        endTable(pw);
    }

    private void indent(PrintWriter pw, int count, String string)
    {
        for (int i = 0; i < count; i++)
        {
            pw.print(INDENT);
        }
        pw.println(string.replace("<", "&lt;").replace(">", "&gt;"));
    }

    static SortedSet<String> getPackageNames(String findField)
    {
        String[] parts = findField.split("\\s");
        SortedSet<String> result = new TreeSet<String>();
        for (String part : parts)
        {
            part = part.trim();
            if (part.length() > 0)
            {
                int idx = part.lastIndexOf('.');
                if (idx != -1)
                {
                    char firstCharAfterLastDot = part.charAt(idx + 1);
                    if (Character.isUpperCase(firstCharAfterLastDot))
                    {
                        result.add(part.substring(0, idx));
                    }
                    else
                    {
                        result.add(part);
                    }
                }
            }
        }
        return result;
    }

}
