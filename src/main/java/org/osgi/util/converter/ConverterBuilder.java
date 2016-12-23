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
package org.osgi.util.converter;

import java.lang.reflect.Type;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.function.Function;

/**
 * A builder to create a new converter with modified behaviour based on an
 * existing converter. The modified behaviour is specified by providing rules
 * and/or conversion functions.
 *
 * @author $Id$
 */
@ProviderType @Deprecated
public interface ConverterBuilder {
	/**
	 * Build the specified converter. Each time this method is called a new
	 * custom converter is produced based on the rules registered with the
	 * builder.
	 *
	 * @return A new converter with the rules provided to the builder.
	 */
	Converter build();

	/**
	 * Register a special rule for this converter.
	 *
	 * @param rule The rule
	 * @return This converter builder for further building.
	 */
	<F, T> ConverterBuilder rule(Rule<F,T> rule);

	/**
	 * Register a special rule for this converter.
	 *
	 * @param fromCls The class from which to convert.
	 * @param toCls The class to which to convert.
	 * @param toFun A function that handles the conversion.
	 * @param fromFun A function that handles the reverse conversion.
	 * @return This converter builder for further building.
	 */
	<F, T> ConverterBuilder rule(Class<F> fromCls, Class<T> toCls,
			Function<F,T> toFun, Function<T,F> fromFun);

	/**
	 * Register a special rule for this converter.
	 *
	 * @param fromRef A type reference representing the class to convert from.
	 * @param toRef A type reference representing the class to convert to.
	 * @param toFun A function that handles the conversion.
	 * @param fromFun A function that handles the reverse conversion.
	 * @return This converter builder for further building.
	 */
	<F, T> ConverterBuilder rule(TypeReference<F> fromRef,
			TypeReference<T> toRef, Function<F,T> toFun, Function<T,F> fromFun);

	/**
	 * Register a special rule for this converter.
	 *
	 * @param fromType A reflection type from which to convert.
	 * @param toType A reflection type to which to convert.
	 * @param toFun A function that handles the conversion.
	 * @param fromFun A function that handles the reverse conversion.
	 * @return This converter builder for further building.
	 */
	<F, T> ConverterBuilder rule(Type fromType, Type toType,
			Function<F,T> toFun, Function<T,F> fromFun);
}
