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

package org.osgi.framework.startlevel.dto;

import org.osgi.dto.DTO;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 * Data Transfer Object for a BundleStartLevel.
 * 
 * <p>
 * An installed Bundle can be adapted to provide a {@code BundleStartLevelDTO}
 * for the Bundle.
 * 
 * @author $Id$
 * @NotThreadSafe
 */
public class BundleStartLevelDTO extends DTO {
    /**
	 * The id of the bundle associated with this start level.
	 * 
	 * @see BundleStartLevel#getBundle()
	 */
    public long    bundle;

    /**
	 * The assigned start level value for the bundle.
	 * 
	 * @see BundleStartLevel#getStartLevel()
	 */
    public int     startLevel;

    /**
	 * The bundle's autostart setting indicates that the activation policy
	 * declared in the bundle manifest must be used.
	 * 
	 * @see BundleStartLevel#isActivationPolicyUsed()
	 */
    public boolean activationPolicyUsed;

    /**
	 * The bundle's autostart setting indicates it must be started.
	 * 
	 * @see BundleStartLevel#isPersistentlyStarted()
	 */
    public boolean persistentlyStarted;
}
