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
package org.apache.felix.serializer.test.objects;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.felix.serializer.test.prevayler.Repository;

public interface ComplexManager
{
    List<String> keys();
    List<ComplexTop> list();
    Optional<ComplexTop> get( String key );

    void add( ComplexTop top );
    void addAll( Collection<ComplexTop> tops );
    void delete( String key );
    void clear();

    // This is only for testing. It would normally not be part of an API.
    Repository<ComplexTop> repository();
}
