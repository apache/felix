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
package org.apache.felix.dm.lambda.samples.hello;

import org.osgi.service.log.LogService;

/**
 * Our HelloClient, which has a required dependency on the HelloService service, as well as an optional
 * dependency on the OSGi LogService. The LogService is injected on compatible class fields and a NullObject
 * will be injected in case there is no LogService currently available.
 */
public class HelloClient {
	
	/**
	 * Injected by Dependency Manager, possibly as a null object in case the dependency is unavailable.
	 */
	volatile LogService m_log;
	
	/**
	 * Injected by Dependency Manager.
	 * @param hello the required HelloService instance
	 */
	void bind(HelloService hello) {
		m_log.log(LogService.LOG_DEBUG, "bound hello service.");
		System.out.println("HelloClient bound with HelloService: saying hello: " + hello.sayHello());
	}
	
	/**
	 * Our component is starting, it has been injected with the required HelloService, as well as the LogService,
	 * which is injected in the m_log class field (possibly with a NullObject in case the LogService is unavailable).
	 */
	void start() {
		System.out.println("HelliClient started");
	}
	
	/**
	 * Our component is stopping, it has possibly lost the required HelloService, or our bundle is being stopped. 
	 */
	void stop() {
		System.out.println("HelloClient stopped");
	}

}
