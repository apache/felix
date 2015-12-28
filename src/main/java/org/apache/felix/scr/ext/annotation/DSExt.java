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
package org.apache.felix.scr.ext.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.xml.XMLAttribute;


public interface DSExt {
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=configurableServiceProperties")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface ConfigurableServiceProperties {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=persistentFactoryComponent")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface PersistentFactoryComponent {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=deleteCallsModify")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface DeleteCallsModify {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=obsoleteFactoryComponentFactory")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface ObsoleteFactoryComponentFactory {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=configureWithInterfaces")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface ConfigureWithInterfaces {
		boolean value() default true;
	}
	
	@XMLAttribute(namespace = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0", prefix = "felix", mapping="value=delayedKeepInstances")
	@Retention(RetentionPolicy.CLASS)
	@Target(ElementType.TYPE)
	@interface DelayedKeepInstances {
		boolean value() default true;
	}

}
