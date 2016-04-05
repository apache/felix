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

import java.util.function.Function;

/**
 * A rule is a data entity can hold all the information needed to specify a
 * custom conversion for use by an @{link Adapter}.
 *
 * @param <F> The type to convert from.
 * @param <T> The type to convert to.
 * @author $Id:$
 */
public class Rule<F, T> {
    private final Function<F, T> toFun;
    private final Function<T, F> fromFun;

	/**
	 * Specify the functions to do the conversions in both directions.
	 * 
	 * @param to The function that performs the conversion.
	 * @param from The function that performs the reverse conversion.
	 */
    public Rule(Function<F, T> to, Function<T, F> from) {
        toFun = to;
        fromFun = from;
    }

	/**
	 * Obtain the conversion function.
	 * 
	 * @return The conversion function.
	 */
    public Function<F, T> getToFunction() {
        return toFun;
    }

	/**
	 * Obtain the reverse conversion function.
	 * 
	 * @return The reverse conversion function.
	 */
    public Function<T, F> getFromFunction() {
        return fromFun;
    }
}
