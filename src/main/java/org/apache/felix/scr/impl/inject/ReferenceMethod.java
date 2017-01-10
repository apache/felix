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
package org.apache.felix.scr.impl.inject;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.framework.BundleContext;

/**
 * Component method to be invoked on service (un)binding or updating
 */
public interface ReferenceMethod
{
    /**
     * Invoke the reference method and bind/unbind/update the reference.
     *
     * @param componentInstance The component instance
     * @param parameters The parameters for the reference.
     * @param methodCallFailureResult Return result for failure
     * @param logger Logger
     * @return The method result
     */
	
	<S, T> MethodResult invoke( Object componentInstance,
                         BindParameters parameters,
                         MethodResult methodCallFailureResult,
                         SimpleLogger logger );

    <S, T> boolean getServiceObject( BindParameters parameters,
            BundleContext context,
            SimpleLogger logger );

    /**
     * A NOP implementation.
     */
    ReferenceMethod NOPReferenceMethod = new ReferenceMethod() {

		@Override
		public <S, T> MethodResult invoke(final Object componentInstance, 
				final BindParameters parameters,
				final MethodResult methodCallFailureResult, 
				final SimpleLogger logger) {
			return MethodResult.VOID;
		}

		@Override
		public <S, T> boolean getServiceObject(final BindParameters parameters,
				final BundleContext context, 
				final SimpleLogger logger) {
			return true;
		}
    };
}
