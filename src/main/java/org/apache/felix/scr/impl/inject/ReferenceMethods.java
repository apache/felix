/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.inject;

/**
 * {@code ReferenceMethods} holds pointers to methods for a single reference.
 * The methods are used for event (method) injection and field injection.
 *
 * <ul>
 *   <li>The bind method
 *   <li>The unbind method
 *   <li>The updated method
 *   <li>The init method (optional, only used for field references)
 * </ul>
 */
public interface ReferenceMethods
{
    /** 
     * Get the method to bind a service. 
     * Never returns {@code null}. 
     */
    ReferenceMethod getBind();

    /** 
     * Get the method to unbind a service. 
     * Never returns {@code null}. 
     */
    ReferenceMethod getUnbind();

    /** 
     * Get the method to update a service. 
     * Never returns {@code null}. 
     */
    ReferenceMethod getUpdated();

    /** 
     * Get an optional method to initialize the component reference handling.
     * This is optional and might return {@code null} 
     */
    InitReferenceMethod getInit();

    /**
     * This is a NOP implementation.
     */
    ReferenceMethods NOPReferenceMethod = new ReferenceMethods() {

		@Override
		public ReferenceMethod getBind() {
			return ReferenceMethod.NOPReferenceMethod;
		}

		@Override
		public ReferenceMethod getUnbind() {
			return ReferenceMethod.NOPReferenceMethod;
		}

		@Override
		public ReferenceMethod getUpdated() {
			return ReferenceMethod.NOPReferenceMethod;
		}

		@Override
		public InitReferenceMethod getInit() {
			return null;
		}    	
    };
}
