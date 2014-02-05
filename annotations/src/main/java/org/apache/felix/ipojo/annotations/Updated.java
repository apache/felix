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
 * This annotation declares an updated callback.
 * Updated callback are called after a <a href="http://felix.apache.org/documentation/subprojects/apache-felix-ipojo/apache-felix-ipojo-userguide/describing-components/configuration-handler.html#being-notified-when-a-reconfiguration-is-completed">reconfiguration</a>.
 *
 * Methods annotated with {@code @Updated} must have one of the 2 following signatures:
 * <pre>
 *     {@code @Updated}
 *     public void updated() {
 *         // The instance was reconfigured
 *     }
 * </pre>
 *
 * <pre>
 *     {@code @Updated}
 *     public void updated(Dictionary conf) {
 *         // The instance was reconfigured, conf is the new configuration.
 *     }
 * </pre>
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Updated {

}
