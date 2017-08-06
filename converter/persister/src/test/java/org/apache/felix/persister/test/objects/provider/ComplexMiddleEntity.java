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
package org.apache.felix.persister.test.objects.provider;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.felix.persister.test.objects.Bottom;
import org.apache.felix.persister.test.objects.ComplexMiddle;

public class ComplexMiddleEntity extends ComplexMiddle.ComplexMiddleDTO implements ComplexMiddle {

    public ComplexMiddleEntity(ComplexMiddleDTO dto) {
        this.id = dto.id;
        this.value = dto.value;
        this.embedded = dto.embedded;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Collection<Bottom> embeddedValue() {
        return embedded.stream()
                .map(e -> new BottomEntity(e))
                .collect(Collectors.toList());
    }
}
