/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.felix.converter.impl.ConverterImpl;

/**
 * Factory class to obtain the standard converter or a new converter builder.
 *
 * @author $Id$
 * @ThreadSafe
 */
public class Converters {
    private static final Converter CONVERTER;

    static {
        ConverterImpl impl = new ConverterImpl();
        ConverterBuilder cb = impl.newConverterBuilder();
        impl.addStandardRules(cb);
        CONVERTER = cb.build();
    }

    private Converters() {
        // Do not instantiate this factory class
    }

    /**
     * Obtain the standard converter.
     * @return The standard converter.
     */
    public static Converter standardConverter() {
        return CONVERTER;
    }

    /**
     * Obtain a converter builder based on the standard converter.
     * @return A new converter builder.
     */
    public static ConverterBuilder newConverterBuilder() {
        return CONVERTER.newConverterBuilder();
    }
}
