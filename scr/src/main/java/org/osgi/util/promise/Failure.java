/*
 * Copyright (c) OSGi Alliance (2014). All Rights Reserved.
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

package org.osgi.util.promise;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Failure callback for a Promise.
 * 
 * <p>
 * A Failure callback is registered with a {@link Promise} using the
 * {@link Promise#then(Success, Failure)} method and is called if the Promise is
 * resolved with a failure.
 * 
 * <p>
 * This is a functional interface and can be used as the assignment target for a
 * lambda expression or method reference.
 * 
 * @ThreadSafe
 * @author $Id: a4bd1ef9948a4abb9fc01a140bd9562be0beb9aa $
 */
@ConsumerType
public interface Failure {
	/**
	 * Failure callback for a Promise.
	 * 
	 * <p>
	 * This method is called if the Promise with which it is registered resolves
	 * with a failure.
	 * 
	 * <p>
	 * In the remainder of this description we will refer to the Promise
	 * returned by {@link Promise#then(Success, Failure)} when this Failure
	 * callback was registered as the chained Promise.
	 * 
	 * <p>
	 * If this methods completes normally, the chained Promise will be failed
	 * with the same exception which failed the resolved Promise. If this method
	 * throws an exception, the chained Promise will be failed with the thrown
	 * exception.
	 * 
	 * @param resolved The failed resolved {@link Promise}.
	 * @throws Exception The chained Promise will be failed with the thrown
	 *         exception.
	 */
	void fail(Promise<?> resolved) throws Exception;
}
