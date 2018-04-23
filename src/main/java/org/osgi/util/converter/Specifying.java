/*
 * Copyright (c) OSGi Alliance (2017, 2018). All Rights Reserved.
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

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is the base interface for the {@link Converting} and {@link Functioning}
 * interfaces and defines the common modifiers that can be applied to these.
 *
 * @param <T> Either {@link Converting} or {@link Specifying}.
 * @author $Id$
 */
@ProviderType
public interface Specifying<T extends Specifying<T>> {
	/**
	 * The default value to use when the object cannot be converted or in case
	 * of conversion from a {@code null} value.
	 *
	 * @param defVal The default value.
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T defaultValue(Object defVal);

	/**
	 * When converting between map-like types use case-insensitive mapping of
	 * keys.
	 *
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T keysIgnoreCase();

	/**
	 * Treat the source object as the specified class. This can be used to
	 * disambiguate a type if it implements multiple interfaces or extends
	 * multiple classes.
	 *
	 * @param cls The class to treat the object as.
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T sourceAs(Class< ? > cls);

	/**
	 * Treat the source object as a JavaBean. By default objects will not be
	 * treated as JavaBeans, this has to be specified using this method.
	 *
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T sourceAsBean();

	/**
	 * Treat the source object as a DTO even if the source object has methods or
	 * is otherwise not recognized as a DTO.
	 *
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T sourceAsDTO();

	/**
	 * Treat the target object as the specified class. This can be used to
	 * disambiguate a type if it implements multiple interfaces or extends
	 * multiple classes.
	 *
	 * @param cls The class to treat the object as.
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T targetAs(Class< ? > cls);

	/**
	 * Treat the target object as a JavaBean. By default objects will not be
	 * treated as JavaBeans, this has to be specified using this method.
	 *
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T targetAsBean();

	/**
	 * Treat the target object as a DTO even if it has methods or is otherwise
	 * not recognized as a DTO.
	 *
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T targetAsDTO();

	/**
	 * Return a live view over the backing object that reflects any changes to
	 * the original object. This is only possible with conversions to
	 * {@link java.util.Map}, {@link java.util.Collection},
	 * {@link java.util.List} and {@link java.util.Set}. The live view object
	 * will cease to be live as soon as modifications are made to it. Note that
	 * conversions to an interface or annotation will always produce a live view
	 * that cannot be modified. This modifier has no effect with conversions to
	 * other types.
	 *
	 * @return The current {@code Converting} object so that additional calls
	 *         can be chained.
	 */
	T view();
}
