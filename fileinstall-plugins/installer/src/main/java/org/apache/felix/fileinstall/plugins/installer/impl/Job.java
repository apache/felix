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

import java.util.concurrent.Callable;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

class Job<T> implements Runnable {

	private final Deferred<T> deferred = new Deferred<>();
	private final Callable<T> call;

	Job(Callable<T> call) {
		this.call = call;
	}

	@Override
	public void run() {
		// Avoid repeating the work
		if (this.deferred.getPromise().isDone()) {
            return;
        }

		try {
			T result = this.call.call();
			this.deferred.resolve(result);
		} catch (Exception e) {
			this.deferred.fail(e);
		}
	}

	public Promise<T> getPromise() {
		return this.deferred.getPromise();
	}


}
