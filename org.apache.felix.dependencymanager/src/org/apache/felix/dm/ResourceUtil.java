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
package org.apache.felix.dm;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Utility class for resource handling.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceUtil {
	/**
	 * Creates a set of properties for a resource based on its URL.
	 * 
	 * @param url the URL
	 * @return a set of properties
	 */
    public static Dictionary<?, ?> createProperties(URL url) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(ResourceHandler.PROTOCOL, url.getProtocol());
        props.put(ResourceHandler.HOST, url.getHost());
        props.put(ResourceHandler.PORT, Integer.toString(url.getPort()));
        props.put(ResourceHandler.PATH, url.getPath());
        return props;
    }
}
