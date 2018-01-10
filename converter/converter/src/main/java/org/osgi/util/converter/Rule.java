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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.osgi.util.function.Function;

/**
 * A rule implementation that works by capturing the type arguments via
 * subclassing. The rule supports specifying both <em>from</em> and <em>to</em>
 * types. Filtering on the <em>from</em> by the {@code Rule} implementation.
 * Filtering on the <em>to</em> is done by the converter customization
 * mechanism.
 *
 * @author $Id$
 * @param <F> The type to convert from.
 * @param <T> The type to convert to.
 */
public abstract class Rule<F, T> implements TargetRule {
	private final ConverterFunction function;

	/**
	 * Create an instance with a conversion function.
	 *
	 * @param func The conversion function to use.
	 */
	public Rule(Function<F,T> func) {
		function = getGenericFunction(func);
	}

	private ConverterFunction getGenericFunction(final Function<F,T> func) {
		return new ConverterFunction() {
			@Override
			@SuppressWarnings("unchecked")
			public Object apply(Object obj, Type targetType) throws Exception {
				Rule< ? , ? > r = Rule.this;
				Type type = ((ParameterizedType) r.getClass()
						.getGenericSuperclass()).getActualTypeArguments()[0];

				if (type instanceof ParameterizedType) {
					type = ((ParameterizedType) type).getRawType();
				}

				Class< ? > cls = null;
				if (type instanceof Class) {
					cls = ((Class< ? >) type);
				} else {
					return ConverterFunction.CANNOT_HANDLE;
				}

				if (cls.isInstance(obj)) {
					return func.apply((F) obj);
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
		Type type = ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[1];
		return type;
	}
}
