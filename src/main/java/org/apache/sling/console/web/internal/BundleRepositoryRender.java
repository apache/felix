/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.console.web.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.assembly.installer.BundleRepositoryAdmin;
import org.apache.sling.assembly.installer.InstallerService;
import org.apache.sling.assembly.installer.Repository;
import org.apache.sling.assembly.installer.Resource;
import org.apache.sling.console.web.Render;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>BundleRepositoryRender</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.reference name="installerService" interface="org.apache.sling.assembly.installer.InstallerService"
 * @scr.service
 */
public class BundleRepositoryRender implements Render {

    public static final String NAME = "bundlerepo";

    public static final String LABEL = "OSGi Repository";

    public static final String PARAM_REPO_ID = "repositoryId";

    public static final String PARAM_REPO_URL = "repositoryURL";

    private static final String REPOSITORY_PROPERTY = "obr.repository.url";

    private BundleContext bundleContext;
    private String[] repoURLs;

    private BundleRepositoryAdmin repoAdmin;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.manager.web.internal.Render#getName()
     */
    public String getName() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.manager.web.internal.Render#getLabel()
     */
    public String getLabel() {
        return LABEL;
    }

    public void render(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        PrintWriter pw = response.getWriter();
        this.header(pw);

        Set activeURLs = new HashSet();
        Iterator repos = this.repoAdmin.getRepositories();
        if (!repos.hasNext()) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content' colspan='4'>No Active Repositories</td>");
            pw.println("</tr>");
        } else {
            while (repos.hasNext()) {
                Repository repo = (Repository) repos.next();

                activeURLs.add(repo.getURL().toString());

                pw.println("<tr class='content'>");
                pw.println("<td class='content'>" + repo.getName() + "</td>");
                pw.println("<td class='content'>" + repo.getURL() + "</td>");
                pw.println("<td class='content'>"
                    + new Date(repo.getLastModified()) + "</td>");
                pw.println("<td class='content'>");
                pw.println("<form>");
                pw.println("<input type='hidden' name='" + Util.PARAM_ACTION
                    + "' value='" + RefreshRepoAction.NAME + "'>");
                pw.println("<input type='hidden' name='"
                    + RefreshRepoAction.PARAM_REPO + "' value='"
                    + repo.getURL() + "'>");
                pw.println("<input class='submit' type='submit' value='Refresh'>");
                pw.println("</form>");
                pw.println("</td>");
                pw.println("</tr>");
            }
        }

        // list any repositories configured but not active
        for (int i=0; i < this.repoURLs.length; i++) {
            if (!activeURLs.contains(this.repoURLs[i])) {
                pw.println("<tr class='content'>");
                pw.println("<td class='content'>-</td>");
                pw.println("<td class='content'>" + this.repoURLs[i] + "</td>");
                pw.println("<td class='content'>[inactive, click Refresh to activate]</td>");
                pw.println("<td class='content'>");
                pw.println("<form>");
                pw.println("<input type='hidden' name='" + Util.PARAM_ACTION
                    + "' value='" + RefreshRepoAction.NAME + "'>");
                pw.println("<input type='hidden' name='"
                    + RefreshRepoAction.PARAM_REPO + "' value='"
                    + this.repoURLs[i] + "'>");
                pw.println("<input class='submit' type='submit' value='Refresh'>");
                pw.println("</form>");
                pw.println("</td>");
                pw.println("</tr>");
            }
        }

        this.footer(pw);

        this.listResources(pw);
    }

    private void header(PrintWriter pw) {
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        pw.println("<tr class='content'>");
        pw.println("<th class='content container' colspan='4'>Bundle Repositories</th>");
        pw.println("</tr>");
        pw.println("<tr class='content'>");
        pw.println("<th class='content'>Name</th>");
        pw.println("<th class='content'>URL</th>");
        pw.println("<th class='content'>Last Modification Time</th>");
        pw.println("<th class='content'>&nbsp;</th>");
        pw.println("</tr>");
    }

    private void footer(PrintWriter pw) {
        pw.println("</table>");
    }

    private void resourcesHeader(PrintWriter pw, boolean doForm) {

        if (doForm) {
            pw.println("<form method='post'>");
            pw.println("<input type='hidden' name='"
                + Util.PARAM_ACTION + "' value='" + InstallFromRepoAction.NAME
                + "'>");
        }

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        pw.println("<tr class='content'>");
        pw.println("<th class='content container' colspan='3'>Available Resources</th>");
        pw.println("</tr>");
        pw.println("<tr class='content'>");
        pw.println("<th class='content'>Deploy</th>");
        pw.println("<th class='content'>Name</th>");
        pw.println("<th class='content'>Version</th>");
        pw.println("</tr>");
    }

    private void listResources(PrintWriter pw) {
        Map bundles = this.getBundles();

        Iterator resources = this.repoAdmin.getResources();
        SortedSet resSet = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2 || o1.equals(o2)) {
                    return 0;
                }

                Resource r1 = (Resource) o1;
                Resource r2 = (Resource) o2;

                if (r1.getPresentationName().equals(r2.getPresentationName())) {
                    return r1.getVersion().compareTo(r2.getVersion());
                }

                return r1.getPresentationName().compareTo(r2.getPresentationName());
            }
        });

        while (resources.hasNext()) {
            Resource res = (Resource) resources.next();
            Version ver = (Version) bundles.get(res.getSymbolicName());
            if (ver == null || ver.compareTo(res.getVersion()) < 0) {
                resSet.add(res);
            }
        }

        this.resourcesHeader(pw, !resSet.isEmpty());

        for (Iterator ri=resSet.iterator(); ri.hasNext(); ) {
            this.printResource(pw, (Resource) ri.next());
        }

        this.resourcesFooter(pw, !resSet.isEmpty());
    }

    private void printResource(PrintWriter pw, Resource res) {
        pw.println("<tr class='content'>");
        pw.println("<td class='content' valign='top' align='center'><input class='checkradio' type='checkbox' name='bundle' value='" + res.getSymbolicName() + "," + res.getVersion() + "'></td>");

        // check whether the resource is an assembly (category name)
        String style = "";
        String[] cat = res.getCategories();
        for (int i=0; cat != null && i < cat.length; i++) {
            if ("assembly".equals(cat[i])) {
                style = "style='font-weight:bold'";
            }
        }
        pw.println("<td class='content' " + style + ">" + res.getPresentationName() + " (" + res.getSymbolicName() + ")</td>");
        pw.println("<td class='content' " + style + " valign='top'>" + res.getVersion() + "</td>");

        pw.println("</tr>");
    }

    private void resourcesButtons(PrintWriter pw) {
        pw.println("<tr class='content'>");
        pw.println("<td class='content'>&nbsp;</td>");
        pw.println("<td class='content' colspan='2'>");
        pw.println("<input class='submit' style='width:auto' type='submit' name='deploy' value='Deploy Selected'>");
        pw.println("&nbsp;&nbsp;&nbsp;");
        pw.println("<input class='submit' style='width:auto' type='submit' name='deploystart' value='Deploy and Start Selected'>");
        pw.println("</td></tr>");
    }

    private void resourcesFooter(PrintWriter pw, boolean doForm) {
        if (doForm) {
            this.resourcesButtons(pw);
        }
        pw.println("</table></form>");
    }

    private Map getBundles() {
        Map bundles = new HashMap();

        Bundle[] installed = this.bundleContext.getBundles();
        for (int i=0; i < installed.length; i++) {
            String ver = (String) installed[i].getHeaders().get(Constants.BUNDLE_VERSION);
            Version bundleVersion = Version.parseVersion(ver);

            // assume one bundle instance per symbolic name !!
            bundles.put(installed[i].getSymbolicName(), bundleVersion);
        }

        return bundles;
    }

    //--------- SCR Integration -----------------------------------------------

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();

        String urlStr = this.bundleContext.getProperty(REPOSITORY_PROPERTY);
        StringTokenizer st = new StringTokenizer(urlStr);
        List urlList = new ArrayList();
        while (st.hasMoreTokens()) {
            urlList.add(st.nextToken());
        }
        this.repoURLs = (String[]) urlList.toArray(new String[urlList.size()]);
    }

    protected void deactivate(ComponentContext context) {
        this.bundleContext = null;
        this.repoURLs = null;
    }

    protected void bindInstallerService(InstallerService installerService) {
        this.repoAdmin = installerService.getBundleRepositoryAdmin();
    }

    protected void unbindInstallerService(InstallerService installerService) {
        this.repoAdmin = null;
    }

}
