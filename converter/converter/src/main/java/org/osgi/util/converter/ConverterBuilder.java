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

import java.lang.reflect.Type;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A builder to create a new converter with modified behavior based on an
 * existing converter. The modified behavior is specified by providing rules
 * and/or conversion functions. If multiple rules match they will be visited in
 * sequence of registration. If a rule's function returns {@code null} the next
 * rule found will be visited. If none of the rules can handle the conversion,
 * the original converter will be used to perform the conversion.
 *
 * @author $Id$
 */
@ProviderType
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
	 * Register a custom error handler. The custom error handler will be called
	 * when the conversion would otherwise throw an exception. The error handler
	 * can either throw a different exception or return a value to be used for
	 * the failed conversion.
	 *
	 * @param func The function to be used to handle errors.
	 * @return This converter builder for further building.
	 */
	ConverterBuilder errorHandler(ConverterFunction func);

	/**
	 * Register a conversion rule for this converter. Note that only the target
	 * type is specified, so the rule will be visited for every conversion to
	 * the target type.
	 *
	 * @param type The type that this rule will produce.
	 * @param func The function that will handle the conversion.
	 * @return This converter builder for further building.
	 */
	ConverterBuilder rule(Type type, ConverterFunction func);

	/**
	 * Register a conversion rule for this converter.
	 *
	 * @param rule A rule implementation.
	 * @return This converter builder for further building.
	 */
	ConverterBuilder rule(TargetRule rule);

	/**
	 * Register a catch-all rule, will be called of no other rule matches.
	 *
	 * @param func The function that will handle the conversion.
	 * @return This converter builder for further building.
	 */
	ConverterBuilder rule(ConverterFunction func);
}
