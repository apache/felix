/*
 * Copyright (c) OSGi Alliance (2001, 2016). All Rights Reserved.
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

package org.osgi.service.cm;

/**
 * An {@code Exception} class to inform the client of a {@code Configuration}
 * about the locked state of a configuration object.
 *
 * @author $Id: LockedConfigurationException.java 1750478 2016-06-28 11:34:40Z
 *         cziegeler $
 * @since 1.6
 */
public class ReadOnlyConfigurationException extends RuntimeException {
    static final long       serialVersionUID    = 1898442024230518832L;

    /**
	 * Create a {@code LockedConfigurationException} object.
	 *
	 * @param reason reason for failure
	 */
	public ReadOnlyConfigurationException(String reason) {
		super(reason);
	}
}
