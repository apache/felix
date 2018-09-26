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
package org.apache.felix.dm.annotation.api;

import org.osgi.service.component.annotations.Component;

/**
 * Service scope for the {@link Component}/{@link AdapterService}/{@link AspectService}/{@link BundleAdapterService} annotations.
 */
public enum ServiceScope {
	/**
	 * When the component is registered as a service, it must be registered as a
	 * bundle scope service but only a single instance of the component must be
	 * used for all bundles using the service.
	 */
	SINGLETON,

	/**
	 * When the component is registered as a service, it must be registered as a
	 * bundle scope service and an instance of the component must be created for
	 * each bundle using the service.
	 */
	BUNDLE,

	/**
	 * When the component is registered as a service, it must be registered as a
	 * prototype scope service and an instance of the component must be created
	 * for each distinct request for the service.
	 */
	PROTOTYPE
}
