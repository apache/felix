/*
 * Copyright (c) OSGi Alliance (2016, 2017). All Rights Reserved.
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
package org.osgi.service.log;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is a stripped down copy of the LogService 1.4 API (R7). It's
 * trimmed down to the methods used by the optional support for
 * R7 logging.
 */
@ProviderType
public interface Logger {

	boolean isDebugEnabled();

	void debug(String message);

	void debug(String format, Object arg);

	boolean isInfoEnabled();

	void info(String message);

	void info(String format, Object arg);

	boolean isWarnEnabled();

	void warn(String message);

	void warn(String format, Object arg);

	boolean isErrorEnabled();

	void error(String message);

	void error(String format, Object arg);
}
