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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

public class SnapshotCommand extends Command {

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
                throw new DeploymentException(DeploymentException.CODE_CANCELLED);
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
                } else {
                    session.getLog().log(LogService.LOG_WARNING, "Could not retrieve storage area of bundle '" + symbolicName + "', skipping it.");
                }
            }
        }
    }

    private void store(File source, File target) throws IOException {
        ZipOutputStream output = null;
        try {
            File[] children = source.listFiles();
            output = new ZipOutputStream(new FileOutputStream(target));
            for (int i = 0; i < children.length; i++) {
                storeRecursive(source, new File(children[i].getName()), output);
            }
        }
        finally {
            closeSilently(output);
        }
    }

    private void storeRecursive(File current, File path, ZipOutputStream output) throws IOException {
        output.putNextEntry(new ZipEntry(path.getPath()));
        if (current.isDirectory()) {
            output.closeEntry();
            File[] childs = current.listFiles();
            for (int i = 0; i < childs.length; i++) {
                storeRecursive(childs[i], new File(path, childs[i].getName()), output);
            }
        } else {
            InputStream input = null;
            try {
                input = new FileInputStream(current);
                byte[] buffer = new byte[4096];
                for (int i = input.read(buffer); i != -1; i = input.read(buffer)) {
                    output.write(buffer, 0, i);
                }
                output.closeEntry();
            }
            finally {
                closeSilently(input);
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
                delete(m_root, false);
                unpack(m_snapshot, m_root);
            }
            finally {
                m_snapshot.delete();
            }
        }

        protected void onFailure(Exception e) {
            m_session.getLog().log(LogService.LOG_WARNING, "Failed to restore snapshot!", e);
        }

        private void delete(File root, boolean deleteRoot) {
            if (root.isDirectory()) {
                File[] childs = root.listFiles();
                for (int i = 0; i < childs.length; i++) {
                    delete(childs[i], true);
                }
            }
            if (deleteRoot) {
                root.delete();
            }
        }

        private void unpack(File source, File target) throws IOException {
            ZipInputStream input = null;
            try {
                input = new ZipInputStream(new FileInputStream(source));
                for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                    if (entry.isDirectory()) {
                        (new File(target, entry.getName())).mkdirs();
                    } else {
                        OutputStream output = null;
                        try {
                            output = new FileOutputStream(target);
                            byte[] buffer = new byte[4096];
                            for (int i = input.read(buffer); i > -1; i = input.read(buffer)) {
                                output.write(buffer, 0, i);
                            }
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
    }
}