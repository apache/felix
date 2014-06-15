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
package org.apache.felix.scr.integration.components;

public class Felix4350Component {

	private static Felix4350Component m_instance;
	private static int m_activateCount;
	private static int m_deactivateCount;
	
    private SimpleComponent component1;
    private SimpleComponent2 component2;

    public void bindComponent1(SimpleComponent component1) {
        this.component1 = component1;
    }

    public void unbindComponent1(SimpleComponent component1) {
        this.component1 = null;
    }

    public void bindComponent2(SimpleComponent2 component2) {
        this.component2 = component2;
    }

    public void unbindComponent2(SimpleComponent2 component2) {
        this.component2 = null;
    }

    public void start() {
    	m_instance = this;
    	m_activateCount++;
    }

    public void stop() {
    	m_instance = null;
    	m_deactivateCount++;
    }
    
    public static void check(int activateCount, int deactivateCount, boolean activated)
    {
    	if (activateCount != m_activateCount ||
    			deactivateCount != m_deactivateCount ||
    			activated == (m_instance == null))
    	{
    		String message = "activation: expected " + activateCount + " actual " + m_activateCount +
    				" deactivation: expected " + deactivateCount + " actual " + m_deactivateCount +
    				" activated: expected " + activated + " actual " + (m_instance != null);
    		throw new IllegalStateException( message );
    		
    	}
    }
    
}
