/*
 * Copyright (c) OSGi Alliance (2012, 2014). All Rights Reserved.
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

package org.osgi.framework.hooks.weaving;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Woven Class Listener Service.
 * 
 * <p>
 * Bundles registering this service will receive notifications whenever a
 * {@link WovenClass woven class} completes a {@link WovenClass#getState()
 * state} transition. Woven Class Listeners are not able to modify the woven
 * class in contrast with {@link WeavingHook weaving hooks}.
 * 
 * <p>
 * Receiving a woven class in the {@link WovenClass#TRANSFORMED TRANSFORMED}
 * state allows listeners to observe the modified {@link WovenClass#getBytes()
 * byte codes} before the class has been {@link WovenClass#DEFINED DEFINED} as
 * well as the additional {@link WovenClass#getDynamicImports() dynamic imports}
 * before the {@link WovenClass#getBundleWiring() bundle wiring} has been
 * updated.
 * 
 * <p>
 * Woven class listeners are synchronously {@link #modified(WovenClass) called}
 * when a woven class completes a state transition. The woven class processing
 * will not proceed until all woven class listeners are done.
 * 
 * <p>
 * If the Java runtime environment supports permissions, the caller must have
 * {@code ServicePermission[WovenClassListener,REGISTER]} in order to register a
 * listener.
 * 
 * @ThreadSafe
 * @since 1.1
 * @author $Id$
 */
@ConsumerType
public interface WovenClassListener {
	/**
	 * Receives notification that a {@link WovenClass woven class} has completed
	 * a state transition.
	 * 
	 * <p>
	 * The listener will be notified when a woven class has entered the
	 * {@link WovenClass#TRANSFORMED TRANSFORMED}, {@link WovenClass#DEFINED
	 * DEFINED}, {@link WovenClass#TRANSFORMING_FAILED TRANSFORMING_FAILED} and
	 * {@link WovenClass#DEFINE_FAILED DEFINE_FAILED} states.
	 * 
	 * <p>
	 * If this method throws any exception, the Framework must log the exception
	 * but otherwise ignore it.
	 * 
	 * @param wovenClass The woven class that completed a state transition.
	 */
	public void modified(WovenClass wovenClass);
}
