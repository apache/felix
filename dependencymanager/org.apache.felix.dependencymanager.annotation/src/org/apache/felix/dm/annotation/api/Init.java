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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method used to configure dynamic dependencies.
 * When this method is invoked, all required dependencies (except the ones declared with a <code>name</code> 
 * attribute) are already injected, and optional dependencies on class fields 
 * are also already injected (possibly with NullObjects).
 * 
 * The purpose of the @Init method is to either declare more dynamic dependencies using the DM API, or to
 * return a Map used to dynamically configure dependencies that are annotated using a <code>name</code> attribute. 
 * 
 * After the init method returns, the added or configured dependencies are then tracked, and when all dynamic 
 * dependencies are injected, then the start method (annotated with @Start) is then invoked.
 * 
 * <h3>Usage Examples</h3>
 * In this sample, the "PersistenceImpl" component dynamically configures the "storage" dependency from the "init" method. 
 * The dependency "required" flag and filter string are derived from an xml configuration that is already injected before the init 
 * method.
 * 
 * <blockquote>
 * <pre>
 * 
 * &#64;Component
 * public class PersistenceImpl implements Persistence {
 *     // Injected before init.
 *     &#64;ServiceDependency
 *     LogService log;
 *     
 *     // Injected before init.
 *     &#64;ConfigurationDependency
 *     void updated(Dictionary conf) {
 *        if (conf != null) {
 *           _xmlConfiguration = parseXmlConfiguration(conf.get("xmlConfiguration"));
 *        }
 *     }
 *     
 *     // Parsed xml configuration, where we'll get our storage service filter and required dependency flag.
 *     XmlConfiguration _xmlConfiguration;
 *  
 *     // Injected after init (dependency filter is defined dynamically from our init method).
 *     &#64;ServiceDependency(name="storage")
 *     Storage storage;
 * 
 *     // Dynamically configure the dependency declared with a "storage" name.
 *     &#64;Init
 *     Map&#60;String, String&#62; init() {
 *        log.log(LogService.LOG_WARNING, "init: storage type=" + storageType + ", storageRequired=" + storageRequired);
 *        Map&#60;String, String&#62; props = new HashMap&#60;&#62;();
 *        props.put("storage.required", Boolean.toString(_xmlConfiguration.isStorageRequired()))
 *        props.put("storage.filter", "(type=" + _xmlConfiguration.getStorageType() + ")");
 *        return props;       
 *     }
 *     
 *     // All dependencies injected, including dynamic dependencies defined from init method.
 *     &#64;Start
 *     void start() {
 *        log.log(LogService.LOG_WARNING, "start");
 *     }
 * 
 *     &#64;Override
 *     void store(String key, String value) {
 *        storage.store(key, value);
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Init
{
}
