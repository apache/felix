/*
 * Copyright (c) OSGi Alliance (2006, 2012). All Rights Reserved.
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

// This document is an experimental draft to enable interoperability
// between bundle repositories. There is currently no commitment to
// turn this draft into an official specification.

package org.osgi.service.resolver;

import java.util.List;
import java.util.Map;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

/**
 * A resolver service resolves the specified resources in the context supplied
 * by the caller.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: dfb89b8d09af62ecf62321b80d7e2310512f27a1 $
 */
public interface Resolver {
	/**
	 * Resolve the specified resolve context and return any new resources and
	 * wires to the caller.
	 * 
	 * <p>
	 * The resolver considers two groups of resources:
	 * <ul>
	 * <li>Mandatory - any resource in the
	 * {@link ResolveContext#getMandatoryResources() mandatory group} must be
	 * resolved. A failure to satisfy any mandatory requirement for these
	 * resources will result in throwing a {@link ResolutionException}</li>
	 * <li>Optional - any resource in the
	 * {@link ResolveContext#getOptionalResources() optional group} may be
	 * resolved. A failure to satisfy a mandatory requirement for a resource in
	 * this group will not fail the overall resolution but no resources or wires
	 * will be returned for that resource.</li>
	 * </ul>
	 * 
	 * <p>
	 * The resolve method returns the delta between the start state defined by
	 * {@link ResolveContext#getWirings()} and the end resolved state. That is,
	 * only new resources and wires are included.
	 * 
	 * <p>
	 * The behavior of the resolver is not defined if the specified resolve
	 * context supplies inconsistent information.
	 * 
	 * @param context The resolve context for the resolve operation. Must not be
	 *        {@code null}.
	 * @return The new resources and wires required to satisfy the specified
	 *         resolve context. The returned map is the property of the caller
	 *         and can be modified by the caller.
	 * @throws ResolutionException If the resolution cannot be satisfied.
	 */
	Map<Resource, List<Wire>> resolve(ResolveContext context) throws ResolutionException;
}
