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

import org.apache.felix.persister.test.objects.ComplexMiddle;
import org.apache.felix.persister.test.objects.ComplexTop;

public class ComplexTopEntity extends ComplexTop.ComplexTopDTO implements ComplexTop {

    public ComplexTopEntity(ComplexTopDTO dto) {
        this.id = dto.id;
        this.value1 = dto.value1;
        this.value2 = dto.value2;
        this.embedded = dto.embedded;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String directValue() {
        return value1;
    }

    @Override
    public String calculatedValue() {
        return value2 == null ? null : value2.toUpperCase();
    }

    @Override
    public ComplexMiddle embeddedValue() {
        return new ComplexMiddleEntity(embedded);
    }
}
