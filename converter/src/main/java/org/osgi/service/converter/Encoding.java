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

import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Interface to specify the target of the encoding operation.
 *
 * @author $Id:$
 */
public interface Encoding {
	/**
	 * Specify that the encoded output should be formatted to look 'pretty',
	 * which may make it easier for humans to read. If not specified, the
	 * encoded output should be formatted to be compact, so save space.
	 *
	 * @return This Encoding object to allow further invocations on it.
	 */
	Encoding pretty();

	/**
	 * Use an output stream as the target of the encoding operation. UTF-8 will
	 * be used, if applicable.
	 *
	 * @param out The output stream to use.
	 */
	void to(OutputStream out);

	/**
	 * Use an output stream as the target of the encoding operation.
	 *
	 * @param out The output stream to use.
	 * @param charset The character set to use.
	 */
	void to(OutputStream out, Charset charset);

	/**
	 * Encode the object and append the result to an appendable.
	 *
	 * @param out The appendable object to use.
	 * @return The appendable object provided in, which allows further appends
	 *         to it be done in a fluent programming style.
	 */
	Appendable to(Appendable out);

	/**
	 * Encode the object and return the result as a string.
	 *
	 * @return The encoded object.
	 */
	@Override
	String toString();
}
