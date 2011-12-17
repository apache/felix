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

/**
 * Provides the plugin and extension API for the Apache Felix Web Console:
 * <dl>
 * <dt>{@link org.apache.felix.webconsole.AbstractWebConsolePlugin}</dt>
 * <dd>Service API for plugins.</dd>
 * <dt>{@link org.apache.felix.webconsole.ConfigurationPrinter}</dt>
 * <dd>Service API for plugins for the Configuration Status page.</dd>
 * <dt>{@link org.apache.felix.webconsole.BrandingPlugin}</dt>
 * <dd>Service API for the branding of the Web Console.</dd>
 * <dt>{@link org.apache.felix.webconsole.WebConsoleSecurityProvider}</dt>
 * <dd>Service API to provide custom authentication and access control.</dd>
 * </dl>
 */
@Version("3.1.2")
@Export(optional = "provide:=true")
package org.apache.felix.webconsole;

import aQute.bnd.annotation.Export;
import aQute.bnd.annotation.Version;

