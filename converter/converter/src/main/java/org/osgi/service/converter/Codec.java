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

import java.lang.reflect.Type;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The Codec service can be used to encode a given object in a certain
 * representation, for example JSON, YAML or XML. The Codec service can also
 * decode the representation it produced. A single Codec service can
 * encode/decode only a single format. To support multiple encoding formats
 * register multiple services.
 * 
 * @author $Id$
 * @ThreadSafe
 */
@ProviderType
public interface Codec {
	/**
	 * Start specifying a decode operation.
	 * 
	 * @param <T> The type to decode to.
	 * @param cls The class to decode to.
	 * @return A {@link Decoding} object to specify the source for the decode
	 *         operation.
	 */
	<T> Decoding<T> decode(Class<T> cls);

	/**
	 * Start specifying a decode operation.
	 * 
	 * @param <T> The type to decode to.
	 * @param ref A type reference for the target type.
	 * @return A {@link Decoding} object to specify the source for the decode
	 *         operation.
	 */
	<T> Decoding<T> decode(TypeReference<T> ref);

	/**
	 * Start specifying a decode operation.
	 * 
	 * @param type The type to convert to.
	 * @return A {@link Decoding} object to specify the source for the decode
	 *         operation.
	 */
	Decoding< ? > decode(Type type);

	/**
	 * Start specifying an encode operation.
	 * 
	 * @param obj The object to encode.
	 * @return an Encoding object to specify the target for the decode
	 *         operation.
	 */
	Encoding encode(Object obj);

	/**
	 * Specify the converter to be used by the code, if an alternative, adapted,
	 * converter is to be used.
	 * 
	 * @param converter The converter to use.
	 * @return A codec that uses the converter as specified.
	 */
	Codec with(Converter converter);
}
