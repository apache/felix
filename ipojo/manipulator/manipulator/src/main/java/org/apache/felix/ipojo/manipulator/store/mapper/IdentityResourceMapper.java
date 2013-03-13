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

package org.apache.felix.ipojo.manipulator.store.mapper;

import org.apache.felix.ipojo.manipulator.store.ResourceMapper;

/**
 * Identity mapper.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class IdentityResourceMapper implements ResourceMapper {
    public String internalize(String name) {
        return name;
    }

    public String externalize(String name) {
        return name;
    }
}
