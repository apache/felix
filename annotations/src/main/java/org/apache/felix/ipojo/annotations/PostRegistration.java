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
package org.apache.felix.ipojo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * Declares a method to be <a href="http://felix.apache.org/documentation/subprojects/apache-felix-ipojo/apache-felix-ipojo-userguide/describing-components/providing-osgi-services.html#being-notified-of-the-service-registration-and-unregistration">notified after service registration</a> is effective.
 *
 * <pre>
 *     {@code @PostRegistration}
 *     public void registered(ServiceReference<?> reference) {
 *         // Called after the service publication
 *     }
 * </pre>
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @see org.apache.felix.ipojo.annotations.Provides
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface PostRegistration {

}
