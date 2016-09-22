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
package org.osgi.service.converter;

import org.apache.felix.converter.impl.ConverterBuilderImpl;
import org.apache.felix.converter.impl.ConverterImpl;
import org.apache.felix.converter.impl.InternalConverter;

public class StandardConverter implements Converter {
    private final InternalConverter converter;

    public StandardConverter() {
        ConverterImpl impl = new ConverterImpl();
        ConverterBuilderImpl cb = impl.newConverterBuilder();
        impl.addStandardRules(cb);
        converter = cb.build();
    }

    @Override
    public Converting convert(Object obj) {
        return converter.convert(obj);
    }

    @Override
    public ConverterBuilder newConverterBuilder() {
        return converter.newConverterBuilder();
    }
}
