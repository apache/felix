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
package org.apache.felix.ipojo.runtime.core.components.constructor;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.services.CheckService;

import java.util.Properties;

@Component(immediate = true)
@Provides
public class CheckServiceProviderWithTwoProperties implements CheckService {


    private final String message;
    private final String product;

    public CheckServiceProviderWithTwoProperties(@Property String message, @Property(value="ipojo") String product) {
        this.message = message;
        this.product = product;
    }


    public boolean check() {
        return message != null;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("message", message);
        props.put("product", product);
        
        return props;
    }

}
