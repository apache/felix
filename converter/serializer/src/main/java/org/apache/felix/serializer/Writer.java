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
package org.apache.felix.serializer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Common interface for a writer, which can be provided by the client.
 */
public interface Writer {
    String write(Object obj);
    Map<String,List<String>> mapOrderingRules();
    Map<String,Comparator<?>> arrayOrderingRules();
}
