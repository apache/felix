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

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ComplexConfiguration implements CheckService {
    
    private List m_list;
    private Map m_map;
    private Dictionary m_dict;
    private String[] m_array;
    
    private List m_complexList;
    private Map m_complexMap;
    private Object[] m_complexArray;

    public boolean check() {
        return true;
    }
    

    public Properties getProps() {
        Properties props = new Properties();
        props.put("list", m_list);
        props.put("map", m_map);
        props.put("dict", m_dict);
        props.put("array", m_array);
        props.put("complex-list", m_complexList);
        props.put("complex-map", m_complexMap);
        props.put("complex-array", m_complexArray);
        return props;
    }

}
