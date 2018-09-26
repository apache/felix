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
package org.osgi.service.jaxrs.whiteboard.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to require the Http Whiteboard implementation. It
 * can be used directly, or as a meta-annotation.
 * <p>
 * This annotation is applied to several of the Http Whiteboard component
 * property annotations meaning that it does not normally need to be applied to
 * DS components which use the Http Whiteboard.
 * 
 * @author $Id: d8dd2f2d079255ec829406ab1e496054a4528d1b $
 * @since 1.0
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RequireJaxRSWhiteboard {
	// This is a purely informational annotation and has no elements.
}
