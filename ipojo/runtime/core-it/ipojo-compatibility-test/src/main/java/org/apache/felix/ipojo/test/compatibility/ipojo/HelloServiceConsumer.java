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

package org.apache.felix.ipojo.test.compatibility.ipojo;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.test.compatibility.service.CheckService;
import org.apache.felix.ipojo.test.compatibility.service.HelloService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hello Service Provider with iPOJO.
 */
@Component
@Provides
@Instantiate
public class HelloServiceConsumer implements CheckService {

    @Requires
    private HelloService hello;

    @Override
    public Map<String, Object> data() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("result", hello.hello("john doe"));
        map.put("object", hello);
        return map;
    }
}
