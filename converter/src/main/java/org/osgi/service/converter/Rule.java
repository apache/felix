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
	private final FunctionThrowsException<F,T>	toFun;
	private final FunctionThrowsException<T,F>	fromFun;

	/**
	 * Specify the functions to do the conversions in both directions.
	 *
	 * @param to The function that performs the conversion.
	 * @param from The function that performs the reverse conversion.
	 */
	public Rule(FunctionThrowsException<F,T> to, FunctionThrowsException<T,F> from) {
		toFun = to;
		fromFun = from;
	}

	/**
	 * Obtain the conversion function.
	 *
	 * @return The conversion function.
	 */
	public FunctionThrowsException<F,T> getToFunction() {
		return toFun;
	}

	/**
	 * Obtain the reverse conversion function.
	 *
	 * @return The reverse conversion function.
	 */
	public FunctionThrowsException<T,F> getFromFunction() {
		return fromFun;
	}
}
