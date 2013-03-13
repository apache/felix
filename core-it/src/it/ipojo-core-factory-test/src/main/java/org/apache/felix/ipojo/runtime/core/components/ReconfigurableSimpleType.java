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

package org.apache.felix.ipojo.runtime.core.components;

public class ReconfigurableSimpleType {


	private String prop; // Property.

	private String x; // Property.

	boolean controller;

	public void start () {
		if (prop == null || prop.equals("KO")) {
			throw new IllegalStateException("Bad Configuration : " + prop);
		}

		if (x == null) {
			throw new IllegalStateException("x is null");
		}

		System.out.println("OK !!!!");
	}

	public void setX(String v) {
		x = v;
	}

	public void setProp(String p) {
		prop = p;
		if (prop == null || prop.equals("KO")) {
			controller = false;
		} else {
			controller = true;
			System.out.println("OK !!!!");
		}
	}

	public void setController(boolean p) {
		if (p) {
			System.out.println("OK !!!!");
		}
	}

}
