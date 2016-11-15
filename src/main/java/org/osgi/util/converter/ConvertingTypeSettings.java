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
package org.osgi.util.converter;

/**
 * Interface that can be used to provide additional information on how to
 * treat a source or target type.
 *
 */
public interface ConvertingTypeSettings {
    /**
     * Treat the object as the specified class. This can be used to disambiguate a type
     * if it implements multiple interfaces or extends multiple classes.
     * @param cls The class to treat the object as.
     * @return The current {@code Converting} object so that additional calls
     *         can be chained.
     */
    Converting as(Class<?> cls);

    /**
     * Treat the object as a JavaBean. By default objects will not be treated as JavaBeans,
     * this has to be specified using this method.
     * @return The current {@code Converting} object so that additional calls
     *         can be chained.
     */
    Converting asJavaBean();
}
