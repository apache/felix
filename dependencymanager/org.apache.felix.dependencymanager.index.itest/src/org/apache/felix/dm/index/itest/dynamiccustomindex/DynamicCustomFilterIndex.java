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
package org.apache.felix.dm.index.itest.dynamiccustomindex;

import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyFilterIndex;
import org.osgi.framework.BundleContext;

/**
 * A 
 * @author nxuser
 *
 */
@SuppressWarnings("restriction")
public class DynamicCustomFilterIndex extends MultiPropertyFilterIndex {
	
	/**
	 * System property set to true when our DynamicCustomFilterIndex has been opened
	 */
	private final static String OPENED = "org.apache.felix.dm.index.itest.dynamiccustomindex.CustomFilterIndex.opened";

	public DynamicCustomFilterIndex(String configString) {
		super(configString);
	}

	@Override
    public void open(BundleContext context) {
    	super.open(context);
    	// indicate to our StaticCustomIndexTest that we have been used
    	System.setProperty(OPENED, "true");
    }
	
	@Override
    public void close() {
    	super.close();
    	System.getProperties().remove(OPENED);
    }

}
