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
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 * Data Transfer Object for a FrameworkStartLevel.
 * 
 * <p>
 * The System Bundle can be adapted to provide a {@code FrameworkStartLevelDTO}
 * for the framework of the Bundle.
 * 
 * @author $Id$
 * @NotThreadSafe
 */
public class FrameworkStartLevelDTO extends DTO {
    /**
	 * The active start level value for the framework.
	 * 
	 * @see FrameworkStartLevel#getStartLevel()
	 */
    public int startLevel;

    /**
	 * The initial start level value that is assigned to a bundle when it is
	 * first installed.
	 * 
	 * @see FrameworkStartLevel#getInitialBundleStartLevel()
	 */
    public int initialBundleStartLevel;
}
