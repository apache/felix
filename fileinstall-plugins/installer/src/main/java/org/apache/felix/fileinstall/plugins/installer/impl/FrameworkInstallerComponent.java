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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.fileinstall.plugins.installer.FrameworkInstaller;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.log.LogService;

@Component
public class FrameworkInstallerComponent implements FrameworkInstaller {

	private final Map<Long, Set<Object>> bundleSponsors = new HashMap<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private LogService log;

	private BundleContext context;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	@Override
	public synchronized List<Bundle> addLocations(Object sponsor, List<String> bundleLocations) throws BundleException, IOException {
		List<Bundle> installed = new ArrayList<>(bundleLocations.size());

		for (String location : bundleLocations) {
			// Find an existing bundle with that location.
			Bundle existing = null;
			for (Bundle bundle : this.context.getBundles()) {
				if (bundle.getLocation().equals(location)) {
					existing = bundle;

					// If the existing bundle was previously installed by us then add to the sponsors.
					Set<Object> sponsors = this.bundleSponsors.get(bundle.getBundleId());
					if (sponsors != null) {
                        sponsors.add(sponsor);
                    }
					break;
				}
			}

			if (existing == null) {
				// No existing bundle with that location. Install it and add the sponsor.
				try {
					// Ensure that the URLConnection doesn't cache. Mostly useful for JarURLConnection,
					// which leaks file handles if this is not done.
					URI locationUri = new URI(location);
					URLConnection connection = locationUri.toURL().openConnection();
					connection.setUseCaches(false);
					try (InputStream stream = connection.getInputStream()) {
						Bundle bundle = this.context.installBundle(location, stream);
						installed.add(bundle);

						Set<Object> sponsors = new HashSet<>();
						sponsors.add(sponsor);
						this.bundleSponsors.put(bundle.getBundleId(), sponsors);
					} catch (BundleException e) {
					    if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
					        log.log(LogService.LOG_WARNING, "Duplicate bundle symbolic-name/version in install for location " + location, e);
					    } else {
					        throw e;
					    }
					}
				} catch (URISyntaxException e) {
					throw new IOException("Invalid bundle location URI: " + location, e);
				}
			}
		}

		return installed;
	}

	@Override
	public synchronized List<Bundle> removeSponsor(Object sponsor) {
		List<Long> toRemove = new LinkedList<>();

		for (Iterator<Entry<Long, Set<Object>>> iter = this.bundleSponsors.entrySet().iterator(); iter.hasNext(); ) {
			Entry<Long, Set<Object>> entry = iter.next();
			long bundleId = entry.getKey();
			Set<Object> sponsors = entry.getValue();

			if (sponsors.remove(sponsor) && sponsors.isEmpty()) {
				// We removed our sponsor and the sponsor set is now empty => this bundle should be removed.
				toRemove.add(bundleId);
				// Also remove the entry from the sponsor map.
				iter.remove();
			}
		}

		List<Bundle> removed = new LinkedList<>();
		for (long bundleId : toRemove) {
			Bundle bundle = this.context.getBundle(bundleId);
			if (bundle != null) { // just in case the bundle was already removed by somebody else
				try {
					bundle.uninstall();
					removed.add(bundle);
				} catch (BundleException e) {
					if (this.log != null) {
                        this.log.log(LogService.LOG_ERROR, "Error uninstalling bundle: " + bundle.getSymbolicName(), e);
                    }
				}
			}
		}
		return removed;
	}

}
