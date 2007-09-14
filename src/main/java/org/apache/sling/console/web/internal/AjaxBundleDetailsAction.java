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
import java.util.Date;
import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>AjaxBundleDetailsAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.reference name="log" interface="org.osgi.service.log.LogService"
 * @scr.service
 */
public class AjaxBundleDetailsAction extends BundleAction {

    public static final String NAME = "ajaxBundleDetails";

    /** @scr.reference */
    private StartLevel startLevelService;

    /** @scr.reference */
    private PackageAdmin packageAdmin;

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        JSONObject result = null;
        try {
            long bundleId = getBundleId(request);
            Bundle bundle = getBundleContext().getBundle(bundleId);
            if (bundle != null) {
                Dictionary headers = bundle.getHeaders();

                JSONArray props = new JSONArray();
                keyVal(props, "Symbolic Name", bundle.getSymbolicName());
                keyVal(props, "Version", headers.get(Constants.BUNDLE_VERSION));
                keyVal(props, "Location", bundle.getLocation());
                keyVal(props, "Last Modification", new Date(
                    bundle.getLastModified()));

                keyVal(props, "Vendor", headers.get(Constants.BUNDLE_VENDOR));
                keyVal(props, "Copyright",
                    headers.get(Constants.BUNDLE_COPYRIGHT));
                keyVal(props, "Description",
                    headers.get(Constants.BUNDLE_DESCRIPTION));

                keyVal(props, "Start Level", getStartLevel(bundle));

                listImportExport(props, bundle);

                listServices(props, bundle);

                result = new JSONObject();
                result.put(BundleListRender.BUNDLE_ID, bundleId);
                result.put("props", props);
            }
        } catch (Exception exception) {
            // create an empty result on problems
            result = new JSONObject();
        }

        // send the result
        response.setContentType("text/javascript");
        response.getWriter().print(result.toString());

        return false;
    }

    private Integer getStartLevel(Bundle bundle) {
        if (startLevelService == null) {
            return null;
        }

        return new Integer(startLevelService.getBundleStartLevel(bundle));
    }

    private void listImportExport(JSONArray props, Bundle bundle) {
        ExportedPackage[] exports = packageAdmin.getExportedPackages(bundle);
        if (exports != null && exports.length > 0) {
            StringBuffer val = new StringBuffer();
            for (int i = 0; i < exports.length; i++) {
                val.append(exports[i].getName());
                val.append(",version=");
                val.append(exports[i].getVersion());
                val.append("<br />");
            }
            keyVal(props, "Exported Packages", val.toString());
        } else {
            keyVal(props, "Exported Packages", "None");
        }

        exports = packageAdmin.getExportedPackages((Bundle) null);
        if (exports != null && exports.length > 0) {
            StringBuffer val = new StringBuffer();
            for (int i = 0; i < exports.length; i++) {
                ExportedPackage ep = exports[i];
                Bundle[] importers = ep.getImportingBundles();
                for (int j = 0; importers != null && j < importers.length; j++) {
                    if (importers[j].getBundleId() == bundle.getBundleId()) {
                        val.append(ep.getName());
                        val.append(",version=").append(ep.getVersion());
                        val.append(" from ");
                        val.append(ep.getExportingBundle().getSymbolicName());
                        val.append(" (").append(
                            ep.getExportingBundle().getBundleId());
                        val.append(")");
                        val.append("<br />");

                        break;
                    }
                }
            }

            // add description if there are no imports
            if (val.length() == 0) {
                val.append("None");
            }

            keyVal(props, "Imported Packages", val.toString());
        }
    }

    private void listServices(JSONArray props, Bundle bundle) {
        ServiceReference[] refs = bundle.getRegisteredServices();
        if (refs == null || refs.length == 0) {
            return;
        }

        for (int i = 0; i < refs.length; i++) {
            String key = "Service ID "
                + refs[i].getProperty(Constants.SERVICE_ID);

            StringBuffer val = new StringBuffer();

            appendProperty(val, refs[i], Constants.OBJECTCLASS, "Types");
            appendProperty(val, refs[i], "sling.context", "Sling Context");
            appendProperty(val, refs[i], Constants.SERVICE_PID, "PID");
            appendProperty(val, refs[i], ConfigurationAdmin.SERVICE_FACTORYPID,
                "Factory PID");
            appendProperty(val, refs[i], ComponentConstants.COMPONENT_NAME,
                "Component Name");
            appendProperty(val, refs[i], ComponentConstants.COMPONENT_ID,
                "Component ID");
            appendProperty(val, refs[i], ComponentConstants.COMPONENT_FACTORY,
                "Component Factory");
            appendProperty(val, refs[i], Constants.SERVICE_DESCRIPTION,
                "Description");
            appendProperty(val, refs[i], Constants.SERVICE_VENDOR, "Vendor");

            keyVal(props, key, val.toString());
        }
    }

    private void appendProperty(StringBuffer dest, ServiceReference ref,
            String name, String label) {
        Object value = ref.getProperty(name);
        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            dest.append(label).append(": ");
            for (int j = 0; j < values.length; j++) {
                if (j > 0) dest.append(", ");
                dest.append(values[j]);
            }
            dest.append("<br />"); // assume HTML use of result
        } else if (value != null) {
            dest.append(label).append(": ").append(value).append("<br />");
        }
    }

    private void keyVal(JSONArray props, String key, Object value) {
        if (key != null && value != null) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("key", key);
                obj.put("value", value);
                props.put(obj);
            } catch (JSONException je) {
                // don't care
            }
        }
    }
}
