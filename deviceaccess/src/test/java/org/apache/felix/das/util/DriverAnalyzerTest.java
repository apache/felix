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
package org.apache.felix.das.util;


import java.util.Properties;

import org.apache.felix.das.OSGiMock;
import org.apache.felix.das.Utils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Constants;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;


/**
 * 
 * Tests the Driver Analyzer.
 * 
 * Nothing fancy is being tested, but if something is changed this
 * validates that at least the most basic feedback can be expected. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class DriverAnalyzerTest {

	
	
	private DriverAnalyzer m_analyzer;
	
	
	@Mock
	private LogService m_log;
	
	private OSGiMock m_osgi;
	
	@Before
	public void setUp() throws Exception {
		
		MockitoAnnotations.initMocks(this);
		
		m_osgi = new OSGiMock();
		m_analyzer = new DriverAnalyzer();

		Utils.inject(m_analyzer, LogService.class, m_log);
	}
	
	
	@Test
	public void VerifyCorrectDriverIsIgnored() {
		
		
		Properties p = new Properties();
		p.put(Constants.DRIVER_ID, "a-driver-id");
		
		
		ServiceReference ref = m_osgi.createReference(new String[]{Driver.class.getName()}, p); 
		
		m_analyzer.driverAdded(ref);
		
		Mockito.verifyZeroInteractions(m_log);
		
	}

	@Test
	public void VerifyIncorrectDriverNoDriverId() {
		
		
		Properties p = new Properties();
		
		ServiceReference ref = m_osgi.createReference(new String[]{Driver.class.getName()}, p); 
		
		m_analyzer.driverAdded(ref);
		
		Mockito.verify(m_log).log(Mockito.eq(LogService.LOG_ERROR), Mockito.anyString());
		Mockito.verifyNoMoreInteractions(m_log);
		
	}
	
	@Test
	public void VerifyIncorrectDriverInvalidDriverId() {
		
		Properties p = new Properties();
		p.put(Constants.DRIVER_ID, new Object());

		ServiceReference ref = m_osgi.createReference(new String[]{Driver.class.getName()}, p); 
		
		m_analyzer.driverAdded(ref);
		
		Mockito.verify(m_log).log(Mockito.eq(LogService.LOG_ERROR), Mockito.anyString());
		Mockito.verifyNoMoreInteractions(m_log);
		
	}

	@Test
	public void VerifyIncorrectDriverEmptyDriverId() {
		
		Properties p = new Properties();
		p.put(Constants.DRIVER_ID, "");

		ServiceReference ref = m_osgi.createReference(new String[]{Driver.class.getName()}, p); 
		
		m_analyzer.driverAdded(ref);
		
		Mockito.verify(m_log).log(Mockito.eq(LogService.LOG_ERROR), Mockito.anyString());
		Mockito.verifyNoMoreInteractions(m_log);
		
	}
}
