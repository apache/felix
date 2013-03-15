
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

package org.apache.felix.jaas.internal;

import static org.apache.felix.jaas.internal.ConfigSpiOsgi.AppConfigurationHolder;
import static org.apache.felix.jaas.internal.ConfigSpiOsgi.Realm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class JaasWebConsolePlugin extends HttpServlet
{

    private final ConfigSpiOsgi configSpi;

    private final BundleLoginModuleCreator loginModuleCreator;

    public JaasWebConsolePlugin(ConfigSpiOsgi configSpi, BundleLoginModuleCreator loginModuleCreator)
    {
        this.configSpi = configSpi;
        this.loginModuleCreator = loginModuleCreator;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException
    {
        if (req.getPathInfo().endsWith("/data.json"))
        {
            getJson(resp);
        }
        else
        {
            getHtml(resp);
        }

    }

    private void getHtml(HttpServletResponse resp) throws IOException
    {
        final PrintWriter pw = resp.getWriter();

        printAppConfigurationDetails(pw);
        printAvailableModuleDetails(pw);

    }

    private void printAvailableModuleDetails(PrintWriter pw)
    {
        Map<Bundle, Set<String>> bundleMap = getAvailableLoginModuleInfo();

        pw.println("<p class=\"statline ui-state-highlight\">${Available LoginModules}</p>");
        if (bundleMap.isEmpty())
        {
            return;
        }

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Bundle}</th>");
        pw.println("<th class=\"header\">${Classes}</th>");
        pw.println("</tr></thead>");

        String rowClass = "odd";
        for (Map.Entry<Bundle, Set<String>> e : bundleMap.entrySet())
        {
            Bundle b = e.getKey();
            pw.print("<tr class=\"%s ui-state-default\">");
            pw.printf("<td><a href=\"${pluginRoot}/../bundles/%s\">%s (%s)</a></td>",
                b.getBundleId(), b.getSymbolicName(), b.getBundleId());
            pw.printf("<td>");
            for (String className : e.getValue())
            {
                pw.print(className);
                pw.print("<br/>");
            }
            pw.print("</td>");
            pw.println("</tr>");

            if (rowClass.equals("odd"))
            {
                rowClass = "even";
            }
            else
            {
                rowClass = "odd";
            }
        }
        pw.println("</table>");
    }

    private void printAppConfigurationDetails(PrintWriter pw)
    {
        Map<String, Realm> configs = getConfigurationDetails();
        if (configs.isEmpty())
        {
            pw.println("No JAAS LoginModule registered");
            return;
        }

        pw.println("<p class=\"statline ui-state-highlight\">${Registered LoginModules}</p>");

        pw.println("<table class=\"nicetable\">");
        pw.println("<thead><tr>");
        pw.println("<th class=\"header\">${Realm}</th>");
        pw.println("<th class=\"header\">${Rank}</th>");
        pw.println("<th class=\"header\">${Control Flag}</th>");
        pw.println("<th class=\"header\">${Type}</th>");
        pw.println("<th class=\"header\">${Classname}</th>");
        pw.println("</tr></thead>");

        for (Realm r : configs.values())
        {
            String realmName = r.getRealmName();
            pw.printf(
                "<tr class=\"ui-state-default\"><td>%s</td><td colspan=\"4\"></td></tr>",
                realmName);

            String rowClass = "odd";
            for (AppConfigurationHolder ah : r.getConfigs())
            {
                LoginModuleProvider lp = ah.getProvider();
                String type = getType(lp);
                pw.printf("<tr class=\"%s ui-state-default\"><td></td><td>%d</td>",
                    rowClass, lp.ranking());
                pw.printf("<td>%s</td>", ControlFlag.toString(lp.getControlFlag()));
                pw.printf("<td>%s</td>", type);

                pw.printf("<td>");
                pw.print(lp.getClassName());

                if (lp instanceof OsgiLoginModuleProvider)
                {
                    ServiceReference sr = ((OsgiLoginModuleProvider) lp).getServiceReference();
                    Object id = sr.getProperty(Constants.SERVICE_ID);
                    pw.printf("<a href=\"${pluginRoot}/../services/%s\">(%s)</a>", id, id);
                }
                else if (lp instanceof ConfigLoginModuleProvider)
                {
                    Map config = lp.options();
                    Object id = config.get(Constants.SERVICE_PID);
                    pw.printf("<a href=\"${pluginRoot}/../configMgr/%s\">(Details)</a>",
                        id);
                }
                pw.printf("</td>");

                pw.println("</tr>");
                if (rowClass.equals("odd"))
                {
                    rowClass = "even";
                }
                else
                {
                    rowClass = "odd";
                }
            }
        }
        pw.println("</table>");
    }

    private String getType(LoginModuleProvider lp)
    {
        String type = "Service";
        if (lp instanceof ConfigLoginModuleProvider)
        {
            type = "Configuration";
        }
        return type;
    }

    private void getJson(HttpServletResponse resp)
    {

    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    @SuppressWarnings("UnusedDeclaration")
    public void printConfiguration(final PrintWriter pw)
    {
        pw.println("JAAS Configuration Details:");
        pw.println();
        pw.println("Registered LoginModules");
        Map<String, Realm> configs = getConfigurationDetails();
        if (configs.isEmpty())
        {
            pw.println("No JAAS LoginModule registered");
        }
        else
        {
            for (Realm r : configs.values())
            {
                String realmName = r.getRealmName();
                pw.printf("Realm : %s \n", realmName);
                for (AppConfigurationHolder ah : r.getConfigs())
                {
                    addSpace(pw, 1);
                    pw.printf("%s \n", ah.getProvider().getClassName());

                    addSpace(pw, 2);
                    pw.printf("Flag    : %s \n",
                        ControlFlag.toString(ah.getProvider().getControlFlag()));
                    addSpace(pw, 2);
                    pw.printf("Type    : %s \n", getType(ah.getProvider()));
                    addSpace(pw, 2);
                    pw.printf("Ranking : %d \n", ah.getProvider().ranking());
                }
            }
        }

        pw.println();

        Map<Bundle, Set<String>> bundleMap = getAvailableLoginModuleInfo();
        pw.println("Available LoginModules");
        if (bundleMap.isEmpty())
        {
            //Nothing to do
        }
        else
        {
            for (Map.Entry<Bundle, Set<String>> e : bundleMap.entrySet())
            {
                Bundle b = e.getKey();
                pw.printf("%s (%s) \n", b.getSymbolicName(), b.getBundleId());
                for (String className : e.getValue())
                {
                    addSpace(pw, 1);
                    pw.println(className);
                }
            }
        }
    }

    private static void addSpace(PrintWriter pw, int count)
    {
        for (int i = 0; i < count; i++)
        {
            pw.print("  ");
        }
    }

    private Map<String, Realm> getConfigurationDetails()
    {
        return configSpi.getAllConfiguration();
    }

    private Map<Bundle, Set<String>> getAvailableLoginModuleInfo()
    {
        return loginModuleCreator.getBundleToLoginModuleMapping();
    }

}
