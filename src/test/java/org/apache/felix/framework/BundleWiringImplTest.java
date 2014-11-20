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
package org.apache.felix.framework;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.junit.Before;
import org.junit.Test;

public class BundleWiringImplTest 
{

	private BundleWiringImpl bundleWiring;
	
	private BundleClassLoader bundleClassLoader;
	
	@Before
	public void setUp() throws Exception 
	{
		Logger logger = new Logger();
		Constructor ctor = BundleRevisionImpl.getSecureAction()
                .getConstructor(BundleClassLoader.class, new Class[] { BundleWiringImpl.class, ClassLoader.class, Logger.class });
            bundleClassLoader = (BundleClassLoader)
                BundleRevisionImpl.getSecureAction().invoke(ctor,
                new Object[] { bundleWiring, this.getClass().getClassLoader(), logger });
	}

	@Test
	public void testBundleClassLoader() 
	{
		assertNotNull(bundleClassLoader);
	}

}
