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

public final class InstallableUnitEvent {

	private final State newState;
	private final InstallableUnit unit;
	private final State oldState;
	
	public InstallableUnitEvent(State oldState, State newState, InstallableUnit unit) {
		this.oldState = oldState;
		this.newState = newState;
		this.unit = unit;
	}
	
	/**
	 * Get the state of the unit before this event, which may be null if the unit did not previously exist.
	 */
	public State getOldState() {
		return oldState;
	}
	
	/**
	 * Get the new state of the unit.
	 */
	public State getNewState() {
		return newState;
	}
	
	public InstallableUnit getUnit() {
		return unit;
	}
	
}
