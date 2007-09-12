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
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.assembly.installer.BundleRepositoryAdmin;
import org.apache.sling.assembly.installer.InstallerService;
import org.apache.sling.assembly.installer.Resource;
import org.apache.sling.console.web.Render;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>BundleListRender</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.reference name="installerService" interface="org.apache.sling.assembly.installer.InstallerService"
 * @scr.service
 */
public class BundleListRender implements Render {

    public static final String NAME = "list";
    public static final String LABEL = "Bundles";
    public static final String BUNDLE_ID = "bundleId";

    private BundleContext bundleContext;

    /** @scr.reference */
    private StartLevel startLevel;

    private BundleRepositoryAdmin repoAdmin;

    /*
     * (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#getName()
     */
    public String getName() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#getLabel()
     */
    public String getLabel() {
        return LABEL;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.internal.Render#render(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        PrintWriter pw = response.getWriter();

        this.header(pw);

        this.installForm(pw);
        pw.println("<tr class='content'>");
        pw.println("<td colspan='7' class='content'>&nbsp;</th>");
        pw.println("</tr>");


        this.tableHeader(pw);

        Bundle[] bundles = this.getBundles();
        if (bundles == null || bundles.length == 0) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content' colspan='6'>No " + this.getLabel() + " installed currently</td>");
            pw.println("</tr>");
        } else {
            long previousBundle = -1;
            for (int i = 0; i < bundles.length; i++) {

                if (previousBundle >= 0) {
                    // prepare for injected table information row
                    pw.println("<tr id='bundle" + previousBundle + "'></tr>");
                }

                this.bundle(pw, bundles[i]);

                previousBundle = bundles[i].getBundleId();
            }

            if (previousBundle >= 0) {
                // prepare for injected table information row
                pw.println("<tr id='bundle" + previousBundle + "'></tr>");
            }
        }

        pw.println("<tr class='content'>");
        pw.println("<td colspan='7' class='content'>&nbsp;</th>");
        pw.println("</tr>");

        this.installForm(pw);

        this.footer(pw);
    }

    protected BundleContext getBundleContext() {
        return this.bundleContext;
    }

    protected StartLevel getStartLevelService() {
        return this.startLevel;
    }

    protected Bundle[] getBundles() {
        return this.bundleContext.getBundles();
    }

    private void header(PrintWriter pw) {
        Util.startScript(pw);
        pw.println("function showDetails(bundleId) {");
        pw.println("    var span = document.getElementById('bundle' + bundleId);");
        pw.println("    if (!span) {");
        pw.println("        return;");
        pw.println("    }");
        pw.println("    if (span.innerHTML) {");
        pw.println("        span.innerHTML = '';");
        pw.println("        return;");
        pw.println("    }");
        pw.println("    var parm = '?" + Util.PARAM_ACTION + "=" + AjaxBundleDetailsAction.NAME + "&" + BUNDLE_ID + "=' + bundleId;");
        pw.println("    sendRequest('GET', parm, displayBundleDetails);");
        pw.println("}");
        pw.println("function displayBundleDetails(obj) {");
        pw.println("    var span = document.getElementById('bundle' + obj." + BUNDLE_ID + ");");
        pw.println("    if (!span) {");
        pw.println("        return;");
        pw.println("    }");
        pw.println("    var innerHtml = '<td class=\"content\">&nbsp;</td><td class=\"content\" colspan=\"6\"><table broder=\"0\">';");
        pw.println("    var props = obj.props;");
        pw.println("    for (var i=0; i < props.length; i++) {");
        pw.println("        innerHtml += '<tr><td valign=\"top\" noWrap>' + props[i].key + '</td><td valign=\"top\">' + props[i].value + '</td></tr>';");
        pw.println("    }");
        pw.println("    innerHtml += '</table></td>';");
        pw.println("    span.innerHTML = innerHtml;");
        pw.println("}");
        Util.endScript(pw);

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
    }

    private void tableHeader(PrintWriter pw) {
//        pw.println("<tr class='content'>");
//        pw.println("<th class='content container' colspan='7'>Installed " + getLabel() + "</th>");
//        pw.println("</tr>");

        pw.println("<tr class='content'>");
        pw.println("<th class='content'>ID</th>");
        pw.println("<th class='content' width='100%'>Name</th>");
        pw.println("<th class='content'>Status</th>");
        pw.println("<th class='content' colspan='4'>Actions</th>");
        pw.println("</tr>");
    }

    private void footer(PrintWriter pw) {
        pw.println("</table>");
    }

    private void bundle(PrintWriter pw, Bundle bundle) {
        String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
        if (name == null || name.length() == 0) {
            name = bundle.getSymbolicName();
        }

        pw.println("<tr>");
        pw.println("<td class='content right'>" + bundle.getBundleId() + "</td>");
        pw.println("<td class='content'><a href='javascript:showDetails(" + bundle.getBundleId() + ")'>" + name + "</a></td>");
        pw.println("<td class='content center'>" + this.toStateString(bundle.getState()) + "</td>");

        // no buttons for system bundle
        if (bundle.getBundleId() == 0) {
            pw.println("<td class='content' colspan='4'>&nbsp;</td>");
        } else {
            boolean enabled = bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED;
            this.actionForm(pw, enabled, bundle.getBundleId(), StartAction.NAME, StartAction.LABEL);

            enabled = bundle.getState() == Bundle.ACTIVE;
            this.actionForm(pw, enabled, bundle.getBundleId(), StopAction.NAME, StopAction.LABEL);

            enabled = bundle.getState() != Bundle.UNINSTALLED && this.hasUpdates(bundle);
            this.actionForm(pw, enabled, bundle.getBundleId(), UpdateAction.NAME, UpdateAction.LABEL);

            enabled = bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.ACTIVE;
            this.actionForm(pw, enabled, bundle.getBundleId(), UninstallAction.NAME, UninstallAction.LABEL);
        }

        pw.println("</tr>");
    }

    private void actionForm(PrintWriter pw, boolean enabled, long bundleId, String action, String actionLabel) {
        pw.println("<form name='form" + bundleId + "' method='post'>");
        pw.println("<td class='content' align='right'>");
        pw.println("<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + action + "' />");
        pw.println("<input type='hidden' name='" + BUNDLE_ID + "' value='" + bundleId + "' />");
        pw.println("<input class='submit' type='submit' value='" + actionLabel + "'" +  (enabled ? "" : "disabled") + " />");
        pw.println("</td>");
        pw.println("</form>");
    }

    private void installForm(PrintWriter pw) {
        int startLevel = this.getStartLevelService().getInitialBundleStartLevel();

        pw.println("<form method='post' enctype='multipart/form-data'>");
        pw.println("<tr class='content'>");
        pw.println("<td class='content'>&nbsp;</td>");
        pw.println("<td class='content'>");
        pw.println("<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + InstallAction.NAME + "' />");
        pw.println("<input class='input' type='file' name='" + InstallAction.FIELD_BUNDLEFILE + "'>");
        pw.println(" - Start <input class='checkradio' type='checkbox' name='" + InstallAction.FIELD_START + "' value='start'>");
        pw.println(" - Start Level <input class='input' type='input' name='" + InstallAction.FIELD_STARTLEVEL + "' value='" + startLevel + "' width='4'>");
        pw.println("</td>");
        pw.println("<td class='content' align='right' colspan='5' noWrap>");
        pw.println("<input class='submit' style='width:auto' type='submit' value='" + InstallAction.LABEL + "'>");
        pw.println("&nbsp;");
        pw.println("<input class='submit' style='width:auto' type='submit' value='" + RefreshPackagesAction.LABEL + "' onClick='this.form[\"" + Util.PARAM_ACTION + "\"].value=\"" + RefreshPackagesAction.NAME + "\"; return true;'>");
        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</form>");
    }

    private String toStateString(int bundleState) {
        switch (bundleState) {
            case Bundle.INSTALLED: return "Installed";
            case Bundle.RESOLVED: return "Resolved";
            case Bundle.STARTING: return "Starting";
            case Bundle.ACTIVE: return "Active";
            case Bundle.STOPPING: return "Stopping";
            case Bundle.UNINSTALLED: return "Uninstalled";
            default: return "Unknown: " + bundleState;
        }
    }

    private boolean hasUpdates(Bundle bundle) {

        Version bundleVersion = Version.parseVersion((String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));

        for (Iterator ri=this.repoAdmin.getResources(); ri.hasNext(); ) {
            Resource res = (Resource) ri.next();
            if (bundle.getSymbolicName().equals(res.getSymbolicName())) {
                if (res.getVersion().compareTo(bundleVersion) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    //--------- SCR Integration -----------------------------------------------

    protected void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    protected void deactivate(ComponentContext context) {
        this.bundleContext = null;
    }

    protected void bindInstallerService(InstallerService installerService) {
        this.repoAdmin = installerService.getBundleRepositoryAdmin();
    }

    protected void unbindInstallerService(InstallerService installerService) {
        this.repoAdmin = null;
    }

    protected void bindStartLevel(StartLevel startLevel) {
        this.startLevel = startLevel;
    }

    protected void unbindStartLevel(StartLevel startLevel) {
        this.startLevel = null;
    }
}
