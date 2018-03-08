/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.installer.impl;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.felix.fileinstall.plugins.installer.Artifact;
import org.apache.felix.fileinstall.plugins.installer.InstallableUnit;
import org.apache.felix.fileinstall.plugins.installer.State;
import org.osgi.framework.Bundle;
import org.osgi.util.promise.Promise;

class InstallableUnitImpl implements InstallableUnit {

    private final DeploymentInstaller installer;
    private final File originFile;
    private final String name;
    private final String symbolicName;
    private final String version;
    private final Collection<Artifact> artifacts;

    private State state;
    private String errorMessage = null;

    InstallableUnitImpl(DeploymentInstaller installer, File originFile, String name, String symbolicName, String version, Collection<Artifact> artifacts) {
        this.installer = installer;
        this.originFile = originFile;
        this.name = name;
        this.symbolicName = symbolicName;
        this.artifacts = artifacts;
        this.version = version;
    }

    @Override
    public File getOrigin() {
        return this.originFile;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSymbolicName() {
        return this.symbolicName;
    }

    @Override
    public Collection<Artifact> getArtifacts() {
        return Collections.unmodifiableCollection(this.artifacts);
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public Promise<List<Bundle>> install() {
        if (!this.state.equals(State.RESOLVED)) {
            throw new IllegalStateException(String.format("Cannot install unit %s: not in the %s state", this.symbolicName, State.RESOLVED));
        }
        return this.installer.putInstallJob(this);
    }

    @Override
    public Promise<List<Bundle>> uninstall() {
        return this.installer.putUninstallJob(this);
    }

    /**
     * Set the state, return true if the state changed.
     */
    boolean setState(State state) {
        if (state == null) {
            throw new IllegalArgumentException("null state not permitted!");
        }
        State oldState = this.state;

        this.state = state;
        return !this.state.equals(oldState);
    }

    void setErrorMessage(String message) {
        this.errorMessage = message;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

}
