/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.util.List;
import java.util.Properties;

public class TestTypedList implements CheckService {

    private List<FooService> list;

    public boolean check() {
        return !list.isEmpty();
    }

    public Properties getProps() {
        Properties props = new Properties();
        if (list != null) {
            props.put("list", list);

            int i = 0;
            for (FooService fs : list) {
                props.put(i, fs.foo());
                i++;
            }
        }

        return props;
    }

}
