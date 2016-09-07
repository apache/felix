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
 * The result of a conversion, which may indicate that the conversion was not
 * possible and that the next handler in the chain should try to process it.
 * @param <T> The target of the conversion.
 */
public class ConvertResult<T> {
    /**
     * The status of the conversion result.
     */
	public enum Status {
		/**
		 * Status to indicate that the value was converted and the result
		 * contains the converted value.
		 */
		CONVERTED,

		/**
		 * Status to indicate that the value cannot be handled by the
		 * {@link ConvertFunction} and should be handled by another handler if
		 * available.
		 */
		CANNOT_CONVERT
	}

    private final Status status;
    private final T result;

    /**
     * Return a instance that indicates that conversion cannot be done.
     * @return A convert result with status {@link ConvertResult.Status#CANNOT_CONVERT}.
     */
    public static <T> ConvertResult<T> cannotConvert() {
        return new ConvertResult<T>(null, Status.CANNOT_CONVERT);
    }

    /**
     * Create a conversion result for a successfully converted object.
     * @param res The conversion result.
     */
    public ConvertResult(T res) {
        this(res, Status.CONVERTED);
    }

    /**
     * Create a conversion result with a specific status.
     * @param res The conversion result.
     * @param s The status.
     */
    public ConvertResult(T res, Status s) {
        status = s;
        result = res;
    }

	/**
	 * Accessor for the conversion result.
	 *
	 * @return The conversion result.
	 */
    public T getResult() {
        return result;
    }

	/**
	 * Accessor for the conversion status.
	 *
	 * @return The conversion status.
	 */
    public Status getStatus() {
        return status;
    }
}
