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
package org.apache.felix.fileinstall.plugins.installer;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.util.promise.Promise;

@ProviderType
public interface InstallableUnit {

    /**
     * Get the path to the original archive file from which this installable unit arises.
     */
    File getOrigin();

    String getName();

    String getSymbolicName();

    String getVersion();

    /**
     * Get the physical artifacts that must be installed into the OSGi Framework
     * in order to fully install this unit. May return {@code null} or empty if
     * the state is {@link State#ERROR}.
     */
    Collection<Artifact> getArtifacts();

    /**
     * Get the current state of this installable unit.
     */
    State getState();

    /**
     * Get any error associated with this installable unit. Likely to be
     * {@code null} unless {@link #getState()} returns {@link State#ERROR}.
     */
    String getErrorMessage();

    /**
     * Install the unit.
     *
     * @return A promise of the list of bundles that were actually installed
     *         into the OSGi Framework as a result of this operation.
     */
    Promise<List<Bundle>> install();

    /**
     * Uninstall the unit.
     *
     * @return A promise of the list of bundles that were actually uninstalled
     *         from the OSGi Framework as a result of this operation.
     */
    Promise<List<Bundle>> uninstall();

}
