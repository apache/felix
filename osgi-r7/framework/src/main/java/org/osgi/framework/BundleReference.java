/*
 * Copyright (c) OSGi Alliance (2009, 2013). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A reference to a Bundle.
 * 
 * @since 1.5
 * @ThreadSafe
 * @author $Id: ad4e0b99177540205a1a8f37f9075989434cc59f $
 */
@ProviderType
public interface BundleReference {
	/**
	 * Returns the {@code Bundle} object associated with this
	 * {@code BundleReference}.
	 * 
	 * @return The {@code Bundle} object associated with this
	 *         {@code BundleReference}.
	 */
	public Bundle getBundle();
}
