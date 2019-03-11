/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
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

package org.osgi.util.converter;

import java.lang.reflect.Type;

/**
 * This interface is the same as the {@link Converting} interface with the
 * addition that the current converter (which may include custom rules) can be
 * set on it. This allows the converter to be re-entrant and use itself for
 * sub-conversions if applicable.
 *
 * @author $Id$
 */
interface InternalConverting extends Converting {
    /**
     * Invoke the conversion while passing the top-level converter. The
     * top-level converter is needed when performing embedded conversions such
     * as in map elements. When using a custom converter, the top converter
     * must be used for this.
     *
     * @param type A Type object to represent the target type to be converted
     *            to.
     * @param <T> The type to convert to.
     * @return The converted object.
     */
    <T> T to(Type type, InternalConverter c);
}
