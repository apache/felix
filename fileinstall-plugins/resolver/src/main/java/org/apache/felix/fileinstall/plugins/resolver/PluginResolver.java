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
package org.apache.felix.fileinstall.plugins.resolver;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service interface for a Plugin Resolver. Takes a request consisting of a list
 * of index URIs and returns a resolution result, consisting of a list of
 * resources to be installed.
 */
@ProviderType
public interface PluginResolver {

	ResolveResult resolve(ResolveRequest request) throws Exception;

}
