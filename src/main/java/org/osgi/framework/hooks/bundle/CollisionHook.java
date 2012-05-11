/*
 * Copyright (c) OSGi Alliance (2011, 2012). All Rights Reserved.
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

package org.osgi.framework.hooks.bundle;

import java.util.Collection;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * OSGi Framework Bundle Collision Hook Service.
 * 
 * <p>
 * If the framework was launched with the {@link Constants#FRAMEWORK_BSNVERSION
 * org.osgi.framework.bsnversion} framework launching property set to
 * {@link Constants#FRAMEWORK_BSNVERSION_MANAGED managed}, then all registered
 * collision hook services will be called during framework bundle install and
 * update operations to determine if an install or update operation will result
 * in a bundle symbolic name and version collision.
 * 
 * @ThreadSafe
 * @version $Id: a1a25ee0432f210a56e911246f477f19edc28bc1 $
 */
public interface CollisionHook {

	/**
	 * Specifies a bundle install operation is being performed.
	 */
	int	INSTALLING	= 1;

	/**
	 * Specifies a bundle update operation is being performed.
	 */
	int	UPDATING	= 2;

	/**
	 * Filter bundle collisions hook method. This method is called during the
	 * install or update operation. The operation type will be
	 * {@link #INSTALLING installing} or {@link #UPDATING updating}. Depending
	 * on the operation type the target bundle and the collision candidate
	 * collection are the following:
	 * <ul>
	 * <li> {@link #INSTALLING installing} - The target is the bundle associated
	 * with the {@link BundleContext} used to call one of the
	 * {@link BundleContext#installBundle(String) install} methods. The
	 * collision candidate collection contains the existing bundles installed
	 * which have the same symbolic name and version as the bundle being
	 * installed.
	 * <li> {@link #UPDATING updating} - The target is the bundle used to call
	 * one of the {@link Bundle#update() update} methods. The collision
	 * candidate collection contains the existing bundles installed which have
	 * the same symbolic name and version as the content the target bundle is
	 * being updated to.
	 * </ul>
	 * This method can filter the collection of collision candidates by removing
	 * potential collisions. For the specified operation to succeed, the
	 * collection of collision candidates must be empty after all registered
	 * collision hook services have been called.
	 * 
	 * @param operationType The operation type. Must be the value of
	 *        {@link #INSTALLING installing} or {@link #UPDATING updating}.
	 * @param target The target bundle used to determine what collision
	 *        candidates to filter.
	 * @param collisionCandidates The collection of collision candidates. The
	 *        collection supports all the optional {@code Collection} operations
	 *        except {@code add} and {@code addAll}. Attempting to add to the
	 *        collection will result in an {@code UnsupportedOperationException}
	 *        . The collection is not synchronized.
	 */
	void filterCollisions(int operationType, Bundle target, Collection<Bundle> collisionCandidates);
}
