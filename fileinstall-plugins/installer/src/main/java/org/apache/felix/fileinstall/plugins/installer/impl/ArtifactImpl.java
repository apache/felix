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

import org.apache.felix.fileinstall.plugins.installer.Artifact;
import org.apache.felix.fileinstall.plugins.installer.Hash;

class ArtifactImpl implements Artifact {

	private final String name;
	private final String version;
	private final String location;
	private final Hash hash;

	ArtifactImpl(String name, String version, String location, Hash hash) {
		this.version = version;
        if (location == null) {
            throw new IllegalArgumentException("Artifact location may not be null");
        }
		this.name = name;
		this.location = location;
		this.hash = hash;
	}

	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public String getVersion() {
	    return version;
	}

	@Override
	public Hash getHash() {
		return this.hash;
	}

	@Override
	public String getLocation() {
		return this.location;
	}

	@Override
	public String toString() {
		return new StringBuilder()
		        .append(this.name)
		        .append('/').append(this.version)
		        .append(":").append(this.hash != null ? this.hash : "<no-hash>")
		        .append("@").append(this.location)
		        .toString();
	}

}
