/*
<<<<<<< HEAD
 * Copyright (c) OSGi Alliance (2009, 2010). All Rights Reserved.
=======
 * Copyright (c) OSGi Alliance (2009, 2013). All Rights Reserved.
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
=======
import org.osgi.annotation.versioning.ProviderType;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
/**
 * A reference to a Bundle.
 * 
 * @since 1.5
 * @ThreadSafe
<<<<<<< HEAD
 * @noimplement
 * @version $Id: e61bd3e020264b04022a430fe09a85ee3aabf1a3 $
 */
=======
 * @author $Id: ad4e0b99177540205a1a8f37f9075989434cc59f $
 */
@ProviderType
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
