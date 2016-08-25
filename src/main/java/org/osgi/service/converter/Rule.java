/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
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
package org.osgi.service.converter;

/**
 * A rule is a data entity can hold all the information needed to specify a
 * custom conversion for use by an @{link Adapter}.
 *
 * @param <F> The type to convert from.
 * @param <T> The type to convert to.
 * @author $Id: 7f624253be48fc23d8a793b38673305dbfd5ff9a $
 * @Immutable
 */
public class Rule<F, T> {
	private final Class<F>				fromClass;
	private final Class<T>				toClass;
	private final ConvertFunction<T,F>	fromFun;
	private final ConvertFunction<F,T>	toFun;

	/**
	 * Create a bidirectional rule.
	 * 
	 * @param fromCls The class from which to convert. If {@link Object} is
	 *            specified then this functions as a wildcard for generic
	 *            conversions.
	 * @param toCls The class to which to convert. If {@link Object} is
	 *            specified then this functions as a wildcard for generic
	 *            conversions.
	 * @param to The conversion function for this rule.
	 * @param from The reverse conversion for this rule.
	 */
	public Rule(Class<F> fromCls, Class<T> toCls, ConvertFunction<F,T> to,
			ConvertFunction<T,F> from) {
		if (fromCls.equals(toCls)) {
			if (fromCls.equals(Object.class)) {
				if (from != null) {
					throw new IllegalStateException(
							"Can only register one catchall converter");
				}
			} else {
				throw new IllegalStateException(
						"Cannot register a convert to itself");
			}
		}

		fromClass = fromCls;
		toClass = toCls;
		toFun = to;
		fromFun = from;
	}

	/**
	 * Create a single-direction rule.
	 * 
	 * @param fromCls The class from which to convert. If {@link Object} is
	 *            specified then this functions as a wildcard for generic
	 *            conversions.
	 * @param toCls The class to which to convert. If {@link Object} is
	 *            specified then this functions as a wildcard for generic
	 *            conversions.
	 * @param to The conversion function for this rule.
	 */
	public Rule(Class<F> fromCls, Class<T> toCls, ConvertFunction<F,T> to) {
		this(fromCls, toCls, to, null);
	}

	/**
	 * Accessor for the class to convert from.
	 * 
	 * @return The class to convert from.
	 */
	public Class<F> getFromClass() {
		return fromClass;
	}

	/**
	 * Accessor for the class to convert to.
	 * 
	 * @return The class to convert to.
	 */
	public Class<T> getToClass() {
		return toClass;
	}

	/**
	 * Obtain the conversion function.
	 *
	 * @return The conversion function.
	 */
	public ConvertFunction<F,T> getToFunction() {
		return toFun;
	}

	/**
	 * Obtain the reverse conversion function.
	 *
	 * @return The reverse conversion function.
	 */
	public ConvertFunction<T,F> getFromFunction() {
		return fromFun;
	}
}
