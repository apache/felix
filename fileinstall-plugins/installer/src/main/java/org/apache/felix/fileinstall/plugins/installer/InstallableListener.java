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

import java.util.Collection;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A whiteboard listener for installation units that can be installed into the
 * present OSGi Framework.
 */
@ConsumerType
public interface InstallableListener {

	/**
	 * Notifies that one or more installable units have changed their states.
	 */
	void installableUnitsChanged(Collection<InstallableUnitEvent> events);

}
