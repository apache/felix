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

import org.osgi.util.function.Function;

/**
 * Rule implementation that works by passing in type arguments rather than
 * subclassing. The rule supports specifying both <em>from</em> and <em>to</em>
 * types. Filtering on the <em>from</em> by the {@code Rule} implementation.
 * Filtering on the <em>to</em> is done by the converter customization
 * mechanism.
 *
 * @author $Id$
 * @param <F> The type to convert from.
 * @param <T> The type to convert to.
 */
public class TypeRule<F, T> implements TargetRule {
	private final ConverterFunction	function;
	private final Type				toType;

	/**
	 * Create an instance based on source, target types and a conversion
	 * function.
	 *
	 * @param from The type to convert from.
	 * @param to The type to convert to.
	 * @param func The conversion function to use.
	 */
	public TypeRule(Type from, Type to, Function<F,T> func) {
		function = getFunction(from, func);
		toType = to;
	}

	private static <F, T> ConverterFunction getFunction(final Type from,
			final Function<F,T> func) {
		return new ConverterFunction() {
			@Override
			@SuppressWarnings("unchecked")
			public Object apply(Object obj, Type targetType) throws Exception {
				if (from instanceof Class) {
					Class< ? > cls = (Class< ? >) from;
					if (cls.isInstance(obj)) {
						T res = func.apply((F) obj);
						if (res != null)
							return res;
						else
							return ConverterFunction.CANNOT_HANDLE;
					}
				}
				return ConverterFunction.CANNOT_HANDLE;
			}
		};
	}

	@Override
	public ConverterFunction getFunction() {
		return function;
	}

	@Override
	public Type getTargetType() {
		return toType;
	}
}
