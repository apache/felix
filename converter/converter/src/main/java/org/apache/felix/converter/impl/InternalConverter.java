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
package org.apache.felix.converter.impl;

import org.osgi.util.converter.Converter;

/**
 * This interface specifies a {@link #convert(Object)} method that
 * returns an {@link InternalConverting} rather than a normal Converting
 * instance.
 */
interface InternalConverter extends Converter {
    /**
     * Start a conversion for the given object. This method overrides the
     * {@link Converter#convert(Object)} method with a co-variant return type.
     *
     * @param obj The object that should be converted.
     * @return An {@link InternalConverting} object to complete the conversion.
     */
    @Override
    InternalConverting convert(Object obj);
}
