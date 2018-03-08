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
package org.apache.felix.fileinstall.plugins.resolver;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Requirement;

/**
 * Represents a request to resolve certain requirements in the context of the
 * current OSGi Framework.
 */
public class ResolveRequest {

    private final String name;
    private final String symbolicName;
    private final String version;
    private final List<URI> indexes;
    private final Collection<Requirement> requirements;

    /**
     * Construct a request.
     *
     * @param indexes
     *            The list of repository indexes to search for installable
     *            resources.
     * @param requirements
     *            The list of requirements to resolve.
     */
    public ResolveRequest(String name, String symbolicName, String version, List<URI> indexes, Collection<Requirement> requirements) {
        this.name = name;
        this.symbolicName = symbolicName;
        this.version = version;
        this.indexes = new ArrayList<>(indexes);
        this.requirements = new ArrayList<>(requirements);
    }

    public String getName() {
        return this.name;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public String getVersion() {
        return this.version;
    }

    public List<URI> getIndexes() {
        return Collections.unmodifiableList(this.indexes);
    }

    public Collection<Requirement> getRequirements() {
        return Collections.unmodifiableCollection(this.requirements);
    }
}
