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
package org.apache.felix.serializer.impl.yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.serializer.Parser;
import org.yaml.snakeyaml.Yaml;

public class DefaultParser implements Parser {

    @Override
    public Map<String, Object> parse(InputStream in)
    {
        Yaml yaml = new Yaml();
        return toMap(yaml.load(in));
    }

    @Override
    public Map<String, Object> parse(CharSequence in) {
        Yaml yaml = new Yaml();
        return toMap(yaml.load(in.toString()));
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map)
            return (Map)obj;

        Map<String, Object> map = new HashMap<>();
        map.put("parsed", obj);
        return map;
    }
}
