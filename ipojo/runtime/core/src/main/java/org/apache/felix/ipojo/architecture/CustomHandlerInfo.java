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
package org.apache.felix.ipojo.architecture;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Information slot for custom {@link Handler}s to put their own custom 
 * information into {@link ComponentTypeDescription} 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface CustomHandlerInfo 
{
	/**
	 * Returns the custom handler information in readable
	 * format to be displayed in ComponentTypeDescription.
	 * @return Custom Handler information
	 */
	Element getDescription();
}
