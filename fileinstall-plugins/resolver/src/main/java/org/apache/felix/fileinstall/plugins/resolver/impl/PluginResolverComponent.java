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
package org.apache.felix.fileinstall.plugins.resolver.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.fileinstall.plugins.resolver.PluginResolver;
import org.apache.felix.fileinstall.plugins.resolver.ResolveRequest;
import org.apache.felix.fileinstall.plugins.resolver.ResolveResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.Resolver;

@Component(name = "org.apache.felix.fileinstall.plugins.resolver")
public class PluginResolverComponent implements PluginResolver {
	
	private BundleContext bundleContext;
	
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	LogService log;
	
	@Reference(target = "(!(" + Constants.SERVICE_BUNDLEID + "=0))")
	Resolver frameworkResolver;

	@Activate
	void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public ResolveResult resolve(ResolveRequest request) throws Exception {
		PluginResolveContext context = new PluginResolveContext(bundleContext, request, log);
		Map<Resource, List<Wire>> resolved = frameworkResolver.resolve(context);
		
		ResolveResultImpl result = new ResolveResultImpl(request);
		for (Entry<Resource, List<Wire>> entry : resolved.entrySet()) {
			Resource resource = entry.getKey();
			// Skip the synthetic "<<INITIAL>>" resource
			if (!context.isInitialResource(resource)) {
				result.addResource(resource, context.getLocation(resource));
			}
		}
		
		return result;
	}
}