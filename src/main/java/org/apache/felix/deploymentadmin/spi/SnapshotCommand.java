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
package org.apache.felix.deploymentadmin.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

public class SnapshotCommand extends Command {
    /** The ZIP specification mandates that directory-entries end with a forward slash (on all platforms). */
    static final String FORWARD_SLASH = "/";

    private final GetStorageAreaCommand m_getStorageAreaCommand;

    public SnapshotCommand(GetStorageAreaCommand getStorageAreaCommand) {
        m_getStorageAreaCommand = getStorageAreaCommand;
    }

    protected void doExecute(DeploymentSessionImpl session) throws Exception {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();

        BundleInfo[] infos = target.getBundleInfos();
        Map storageAreas = m_getStorageAreaCommand.getStorageAreas();
        for (int i = 0; i < infos.length; i++) {
            if (isCancelled()) {
                throw new DeploymentException(CODE_CANCELLED);
            }

            String symbolicName = infos[i].getSymbolicName();
            Bundle bundle = target.getBundle(symbolicName);
            if (bundle != null) {
                File root = (File) storageAreas.get(symbolicName);
                if (root != null) {
                    File snapshot = context.getDataFile("snapshots");
                    snapshot.mkdirs();
                    snapshot = new File(snapshot, infos[i].getSymbolicName());
                    try {
                        snapshot.createNewFile();
                        store(root, snapshot);
                        addRollback(new RestoreSnapshotRunnable(session, snapshot, root));
                        addCommit(new DeleteSnapshotRunnable(session, snapshot));
                    }
                    catch (Exception e) {
                        session.getLog().log(LogService.LOG_WARNING, "Could not access storage area of bundle '" + symbolicName + "'!", e);
                        snapshot.delete();
                    }
                }
                else {
                    session.getLog().log(LogService.LOG_WARNING, "Could not retrieve storage area of bundle '" + symbolicName + "', skipping it.");
                }
            }
        }
    }

    protected static void restore(File archiveFile, File targetDir) throws IOException {
        ZipInputStream input = null;
        try {
            input = new ZipInputStream(new FileInputStream(archiveFile));

            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                File targetEntry = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    if (!targetEntry.mkdirs()) {
                        throw new IOException("Failed to create one or more sub-directories!");
                    }
                }
                else {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(targetEntry);
                        copy(input, output);
                    }
                    finally {
                        closeSilently(output);
                    }
                }

                input.closeEntry();
            }
        }
        finally {
            closeSilently(input);
        }
    }

    protected static void store(File sourceDir, File archiveFile) throws IOException {
        ZipOutputStream output = null;
        try {
            output = new ZipOutputStream(new FileOutputStream(archiveFile));
            // Traverse source directory recursively, and store all entries...
            store(output, sourceDir, "");
        }
        finally {
            closeSilently(output);
        }
    }

    protected static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
        finally {
            os.flush();
        }
    }

    private static void store(ZipOutputStream output, File sourceDir, String entryName) throws IOException {
        File entry = new File(sourceDir, entryName);

        if (entry.isFile()) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipEntry.setSize(entry.length());
            zipEntry.setTime(entry.lastModified());

            output.putNextEntry(zipEntry);

            InputStream input = null;
            try {
                input = new FileInputStream(entry);
                copy(input, output);
            }
            finally {
                closeSilently(input);
                output.closeEntry();
            }
        }
        else if (entry.isDirectory()) {
            String baseDir = "";
            if (!"".equals(entryName)) {
                baseDir = entryName;
                // Directories *must* use forward slashes...
                if (!baseDir.endsWith(FORWARD_SLASH)) {
                    baseDir = baseDir.concat(FORWARD_SLASH);
                }

                output.putNextEntry(new ZipEntry(baseDir));
                output.closeEntry();
            }

            String[] entries = entry.list();
            for (int i = 0; i < entries.length; i++) {
                store(output, sourceDir, baseDir.concat(entries[i]));
            }
        }
    }

    private static class DeleteSnapshotRunnable extends AbstractAction {
        private final DeploymentSessionImpl m_session;
        private final File m_snapshot;

        private DeleteSnapshotRunnable(DeploymentSessionImpl session, File snapshot) {
            m_session = session;
            m_snapshot = snapshot;
        }

        protected void doRun() {
            if (!m_snapshot.delete()) {
                m_session.getLog().log(LogService.LOG_WARNING, "Failed to delete snapshot in " + m_snapshot + "!");
            }
        }
    }

    private static class RestoreSnapshotRunnable extends AbstractAction {
        private final DeploymentSessionImpl m_session;
        private final File m_snapshot;
        private final File m_root;

        private RestoreSnapshotRunnable(DeploymentSessionImpl session, File snapshot, File root) {
            m_session = session;
            m_snapshot = snapshot;
            m_root = root;
        }

        protected void doRun() throws Exception {
            try {
                Utils.delete(m_root, false /* deleteRoot */);
                restore(m_snapshot, m_root);
            }
            finally {
                m_snapshot.delete();
            }
        }

        protected void onFailure(Exception e) {
            m_session.getLog().log(LogService.LOG_WARNING, "Failed to restore snapshot!", e);
        }
    }
}