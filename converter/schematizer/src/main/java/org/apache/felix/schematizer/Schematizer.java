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
package org.apache.felix.schematizer;

import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.dto.DTO;
import org.osgi.util.converter.TypeReference;

@ProviderType
public interface Schematizer {
    Optional<Schema> get(String name);
    Optional<Schema> from(String name, Map<String, Node.DTO> map);

    <T extends DTO>Schematizer rule(String name, TypeRule<T> rule);
    <T extends DTO>Schematizer rule(String name, String path, TypeReference<T> type);
    <T>Schematizer rule(String name, String path, Class<T> cls);

    /**
     * Shortcut for rule(String name, String path, TypeReference<T> type) when path is "/".
     */
    <T extends DTO>Schematizer rule(String name, TypeReference<T> type);
}
