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
package org.apache.felix.ipojo;

/**
 * Error Handler Service Definition.
 * When exposed, this service is invoked when iPOJO throws a warning
 * or an error. It's a hook on the internal iPOJO logger.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ErrorHandler {

	/**
	 * Method invokes when an error occurred.
	 * @param instance the instance (can be <code>null</null>)
	 * @param message the error message
	 * @param error the error itself (can be <code>null</code>)
	 */
	public void onError(ComponentInstance instance, String message, Throwable error);

	/**
	 * Method invokes when a warning occurred.
	 * @param instance the instance (can be <code>null</null>)
	 * @param message the error message
	 * @param error the error itself (can be <code>null</code>)
	 */
	public void onWarning(ComponentInstance instance, String message, Throwable error);

}
