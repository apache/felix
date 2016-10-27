/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.util.function;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A callback that performs an operation and may throw an exception.
 * <p>
 * This is a functional interface and can be used as the assignment target for a
 * lambda expression or method reference.
 * 
 * @ThreadSafe
 * @since 1.1
 * @author $Id: 17ff376bc9c8c171caad89eb9d0bc496f46961ee $
 */
@ConsumerType
@FunctionalInterface
public interface Callback {
	/**
	 * Execute the callback.
	 * 
	 * @throws Exception An exception thrown by the method.
	 */
	void run() throws Exception;
}
