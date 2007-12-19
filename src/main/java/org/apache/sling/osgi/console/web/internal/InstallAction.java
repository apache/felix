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
package org.apache.sling.osgi.console.web.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.sling.osgi.assembly.installer.Installer;
import org.apache.sling.osgi.assembly.installer.InstallerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * The <code>InstallAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.service
 */
public class InstallAction extends BundleAction {

    public static final String NAME = "install";

    public static final String LABEL = "Install or Update";

    public static final String FIELD_STARTLEVEL = "bundlestartlevel";

    public static final String FIELD_START = "bundlestart";

    public static final String FIELD_BUNDLEFILE = "bundlefile";

    /** @scr.reference */
    private InstallerService installerService;

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return LABEL;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response) throws ServletException {

        // get the uploaded data
        @SuppressWarnings("unchecked")
        Map<String, FileItem[]> params = (Map<String, FileItem[]>) request.getAttribute(Util.ATTR_FILEUPLOAD);
        if (params == null) {
            return true;
        }

        FileItem startItem = this.getFileItem(params, FIELD_START, true);
        FileItem startLevelItem = this.getFileItem(params, FIELD_STARTLEVEL, true);
        FileItem bundleItem = this.getFileItem(params, FIELD_BUNDLEFILE, false);

        // don't care any more if not bundle item
        if (bundleItem == null || bundleItem.getSize() <= 0) {
            return true;
        }

        // default values
        boolean start = startItem != null; // don't care for the value, as long
        // it exists
        int startLevel = -1;
        String bundleLocation = "inputstream:";

        // convert the start level value
        if (startLevelItem != null) {
            try {
                startLevel = Integer.parseInt(startLevelItem.getString());
            } catch (NumberFormatException nfe) {
                // TODO: Handle or ignore
            }
        }

        // install the bundle now
        File tmpFile = null;
        try {
            // copy the data to a file for better processing
            tmpFile = File.createTempFile("install", ".tmp");
            bundleItem.write(tmpFile);
            bundleLocation = "inputstream:" + bundleItem.getName();

            this.installBundle(bundleLocation, tmpFile, startLevel, start);
        } catch (Exception e) {
            this.log(null, "Problem accessing uploaded bundle file", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }

        return true;
    }

    private FileItem getFileItem(Map<String, FileItem[]> params, String name, boolean isFormField) {
        FileItem[] items = params.get(name);
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].isFormField() == isFormField) {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }

    private void installBundle(String location, File bundleFile, int startLevel,
            boolean start) {
        if (bundleFile != null) {

            // try to get the bundle name, fail if none
            String symbolicName = this.getSymbolicName(bundleFile);
            if (symbolicName == null) {
                return;
            }

            // check for existing bundle first
            Bundle newBundle = null;
            Bundle[] bundles = this.getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++) {
                if ((bundles[i].getLocation() != null && bundles[i].getLocation().equals(
                    location))
                    || (bundles[i].getSymbolicName() != null && bundles[i].getSymbolicName().equals(
                        symbolicName))) {
                    newBundle = bundles[i];
                    break;
                }
            }

            Installer installer = this.installerService.getInstaller();
            try {
                // stream will be closed by update or installBundle
                InputStream bundleStream = new FileInputStream(bundleFile);

                if (newBundle != null) {
                    // update existing bundle, to not set startlevel or start
                    this.updateBackground(newBundle, bundleFile, bundleStream);

                } else {
                    // non-existing bundle is installed
                    URL source = bundleFile.toURI().toURL();
                    installer.addBundle(location, source, startLevel);
                    installer.install(start);
                }
            } catch (Throwable t) {
                this.log(null, "Failed to install bundle " + symbolicName
                    + " (Location:" + location + ")", t);
            } finally {
                installer.dispose();
            }
        }
    }

    private String getSymbolicName(File bundleFile) {
        JarFile jar = null;
        try {
            jar = new JarFile(bundleFile);
            Manifest m = jar.getManifest();
            if (m != null) {
                return m.getMainAttributes().getValue(
                    Constants.BUNDLE_SYMBOLICNAME);
            }
        } catch (IOException ioe) {
            // TODO: should log
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        // fall back to "not found"
        return null;
    }

    private void updateBackground(final Bundle bundle, final File bundleFile, final InputStream bundleStream) {
        Thread t = new Thread("Background Update") {
            public void run() {
                // wait some time for the request to settle
                try {
                    sleep(500L);
                } catch (InterruptedException ie) {
                    // don't care
                }

                // now deploy the resolved bundles
                try {
                    bundle.update(bundleStream);
                } catch (BundleException be) {
                    // TODO: log
                } finally {
                    bundleFile.delete();
                }
            }
        };

        t.setDaemon(true); // make a daemon thread (detach from current thread)
        t.start();
    }

    protected void bindInstallerService(InstallerService installerService) {
        this.installerService = installerService;
    }

    protected void unbindInstallerService(InstallerService installerService) {
        this.installerService = null;
    }
}
