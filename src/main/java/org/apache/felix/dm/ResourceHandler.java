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

/** 
 * Service interface for anybody wanting to be notified of changes to resources. 
 */
public interface ResourceHandler {
    /** Name of the property that's used to describe the filter condition for a resource. */
    public static final String FILTER = "filter";
    
    /** The host part of the URL. */
    public static final String HOST = "host";
    /** The path part of the URL. */
    public static final String PATH = "path";
    /** The protocol part of the URL. */
    public static final String PROTOCOL = "protocol";
    /** The port part of the URL. */
    public static final String PORT = "port";

    /** Invoked whenever a new resource is added. */
	public void added(URL resource);
	/** Invoked whenever an existing resource changes. */
	public void changed(URL resource);
	/** Invoked whenever an existing resource is removed. */
	public void removed(URL resource);
}
