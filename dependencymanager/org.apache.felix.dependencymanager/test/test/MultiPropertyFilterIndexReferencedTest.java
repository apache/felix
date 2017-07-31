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
package test;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyFilterIndex;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class MultiPropertyFilterIndexReferencedTest {

	
	@SuppressWarnings("rawtypes")
	@Test
	public void noContext() {
		MultiPropertyFilterIndex singleValueFilterIndex = new MultiPropertyFilterIndex("objectClass,!context");

		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		ref1.addProperty("context", "context");
		
		TestReference ref2 = new TestReference();
		ref2.addProperty("service.id", 1000);
		ref2.addProperty("objectclass", "java.lang.String");
				
		singleValueFilterIndex.addedService(ref1, new String("Service1"));
		
		singleValueFilterIndex.addedService(ref2, new String("Service2"));
		
		List<ServiceReference> allServiceReferencesByClass = singleValueFilterIndex.getAllServiceReferences("java.lang.String", null);
		List<ServiceReference> allServiceReferencesByFilter = singleValueFilterIndex.getAllServiceReferences(null, "(objectClass=java.lang.String)");
		
		assertTrue(allServiceReferencesByClass.size() == allServiceReferencesByFilter.size());
		assertTrue(allServiceReferencesByFilter.size() == 1);
	}
	
	
	@SuppressWarnings("rawtypes")
	@Test
	public void noContextCid() {
		MultiPropertyFilterIndex multiPropertyIndex_new = new MultiPropertyFilterIndex("objectClass,cid,!context");

		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		ref1.addProperty("context", "context");
		
		TestReference ref2 = new TestReference();
		ref2.addProperty("service.id", 1000);
		ref2.addProperty("cid", "cid2");
		ref2.addProperty("objectclass", "java.lang.String");
		
		TestReference ref3 = new TestReference();
		ref3.addProperty("cid", "cid2");
		ref3.addProperty("rank", "1");
		ref3.addProperty("service.id", 1000);
		ref3.addProperty("objectclass", "java.lang.String");
		
		multiPropertyIndex_new.addedService(ref1, new String("Service1"));
		
		multiPropertyIndex_new.addedService(ref2, new String("Service2"));
		
		multiPropertyIndex_new.addedService(ref3, new String("Service3"));
		
		List<ServiceReference> result_new = multiPropertyIndex_new.getAllServiceReferences("java.lang.String", "(cid=cid2)");
		
		assertTrue(result_new.size() == 2);
	}
	
	
	

	@SuppressWarnings("rawtypes")
	@Test
	public void singleKeyfilterIndex() {
		MultiPropertyFilterIndex multiPropertyIndex = new MultiPropertyFilterIndex("objectClass");
		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		
		TestReference ref2 = new TestReference();
		ref2.addProperty("service.id", 4711);
		ref2.addProperty("objectclass", "java.lang.Object");
		
		multiPropertyIndex.addedService(ref1, new String("Service1"));
		multiPropertyIndex.addedService(ref2, new Object());

		//find by classname;
		assertTrue(multiPropertyIndex.isApplicable("java.lang.String", ""));
		List<ServiceReference> byClazzName = multiPropertyIndex.getAllServiceReferences("java.lang.String", "");
		assertTrue(byClazzName.size() == 1);
		assertTrue(byClazzName.get(0).equals(ref1));
		
		//find by filter
		assertTrue(multiPropertyIndex.isApplicable(null, "(objectClass=java.lang.String)"));
		byClazzName = multiPropertyIndex.getAllServiceReferences(null, "(objectClass=java.lang.String)");
		assertTrue(byClazzName.size() == 1);
		assertTrue(byClazzName.get(0).equals(ref1));
		
		//Add extra service
		TestReference ref3 = new TestReference();
		ref3.addProperty("service.id", 4712);
		ref3.addProperty("objectclass", "java.lang.String");
		multiPropertyIndex.addedService(ref3, new String("Service3"));
		
		byClazzName = multiPropertyIndex.getAllServiceReferences("java.lang.String", null);
		assertTrue(byClazzName.size() == 2);
		assertTrue(byClazzName.get(0).equals(ref1));
		assertTrue(byClazzName.get(1).equals(ref3));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void propertyIndexWithDoubleNoPermutationKeys() {
		String filterConfig =  "objectClass,#related-concept-absoluteidentifier,#context-concept-absoluteidentifier,StoreClass";
	
		MultiPropertyFilterIndex multiPropertyIndex_new = new MultiPropertyFilterIndex(filterConfig);
	
		String[] relatedConcepts = {"rel-a", "rel-b"};
		String[] contextConcepts = {"cca-a", "cca-b"};
		 
		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		ref1.addProperty("related-concept-absoluteidentifier", relatedConcepts);
		ref1.addProperty("context-concept-absoluteidentifier", contextConcepts);
		ref1.addProperty("StoreClass", "NoteStore");
		
		multiPropertyIndex_new.addedService(ref1, new String("Service1"));

		List<ServiceReference> result_new = multiPropertyIndex_new.getAllServiceReferences("java.lang.String", "(&(context-concept-absoluteidentifier=cca-a)(related-concept-absoluteidentifier=rel-b)(storeclass=NoteStore))");

		assertTrue(result_new.size() == 1);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void propertyIndexWithDoubleMultiProperty() {
		String filterConfig =  "objectClass,related-concept-absoluteidentifier,context-concept-absoluteidentifier,StoreClass";
		MultiPropertyFilterIndex multiPropertyIndex_new = new MultiPropertyFilterIndex(filterConfig);
		
	
		String[] relatedConcepts = {"rel-a", "rel-b"};
		String[] contextConcepts = {"cca-a", "cca-b"};
		 
		
		TestReference ref1 = new TestReference();
		ref1.addProperty("service.id", 4711);
		ref1.addProperty("objectclass", "java.lang.String");
		ref1.addProperty("related-concept-absoluteidentifier", relatedConcepts);
		ref1.addProperty("context-concept-absoluteidentifier", contextConcepts);
		ref1.addProperty("StoreClass", "NoteStore");
		
		multiPropertyIndex_new.addedService(ref1, new String("Service1"));

		List<ServiceReference> result_new = multiPropertyIndex_new.getAllServiceReferences("java.lang.String", "(&(context-concept-absoluteidentifier=cca-a)(related-concept-absoluteidentifier=rel-b)(storeclass=NoteStore))");

		assertTrue(result_new.size() == 1);
	}
	
	
	@SuppressWarnings("rawtypes")
	@Test
	public void MultiPropertyFilterIndexTypes() {
		
		long serviceId = 4711;
		int ranking = 46;
		boolean isSomeBoolean = true;
		String[] interfaces = {"A", "B", "C"};
		
		MultiPropertyFilterIndex multiPropertyIndexSingleFilter = new MultiPropertyFilterIndex("objectClass");
		MultiPropertyFilterIndex multiPropertyIndexMultiple = new MultiPropertyFilterIndex("objectClass,service.id,ranking,interfaces");
		
		TestReference newReference = new TestReference();
		newReference.addProperty("service.id", serviceId);
		newReference.addProperty("objectclass", "java.lang.Object");
		newReference.addProperty("ranking", ranking);
		newReference.addProperty("someboolvalue", isSomeBoolean);
		newReference.addProperty("interfaces", interfaces);
		
		multiPropertyIndexMultiple.addedService(newReference, new Object());
		multiPropertyIndexSingleFilter.addedService(newReference, new Object());
	
		List<ServiceReference> noFilter = multiPropertyIndexSingleFilter.getAllServiceReferences("java.lang.Object", null);
		assertTrue(noFilter.size() == 1);
		
		List<ServiceReference> noClazz = multiPropertyIndexMultiple.getAllServiceReferences(null, "(&(objectClass=java.lang.Object)(&(service.id=4711)(ranking=46)(interfaces=B)))");
		assertTrue(noClazz.size() == 1);
		
		List<ServiceReference> combi = multiPropertyIndexMultiple.getAllServiceReferences("java.lang.Object", "(&(ranking=46)(interfaces=C)(service.id=4711))");
		assertTrue(combi.size() == 1);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void MultiPropertyFilterIndexKeyGen() {
		String key = "(&(objectClass=org.acme.xyz.framework.internationalization.Translatable)(component-identifier=org.acme.xyz.framework.webui.engine.impl.CompoundProcessContextGroupingPanelFactory))";
		
		long serviceId = 4711;
		int ranking = 46;
		
		MultiPropertyFilterIndex biIndex = new MultiPropertyFilterIndex("objectClass,component-identifier");
		TestReference newReference = new TestReference();
		newReference.addProperty("service.id", serviceId);
		newReference.addProperty("ranking", ranking);
		newReference.addProperty("component-identifier", "org.acme.xyz.framework.webui.engine.impl.CompoundProcessContextGroupingPanelFactory");
		newReference.addProperty("objectclass", "org.acme.xyz.framework.internationalization.Translatable");
		
		biIndex.addedService(newReference, new Object());
	
		
		List<ServiceReference> noClazz = biIndex.getAllServiceReferences(null, key);
		
		assertTrue(noClazz.size() == 1);
	}
	
	
	@SuppressWarnings("rawtypes")
	class TestReference implements ServiceReference {
		Properties props = new Properties();
	
		TestReference() {
		}
	
		public void addProperty(String key, String value) {
			/* Property keys are case-insensitive. -> see @ org.osgi.framework.ServiceReference */
			props.put(key.toLowerCase(), value);
		}
		
		public void addProperty(String key, long value) {
			props.put(key, value);
		}
		
		public void addProperty(String key, int value) {
			props.put(key, value);
		}
		
		public void addProperty(String key, boolean value) {
			props.put(key, value);
		}
	
		public void addProperty(String key, String[] multiValue) {
			props.put(key, multiValue);
		}
		
		
		@Override
		public Object getProperty(String key) {
			return props.get(key);
		}

		@Override
		public String[] getPropertyKeys() {
			return props.keySet().toArray(new String[]{});
		}

		@Override
		public Bundle getBundle() {
			return null;
		}

		@Override
		public Bundle[] getUsingBundles() {
			return null;
		}

		@Override
		public boolean isAssignableTo(Bundle bundle, String className) {
			return false;
		}

		@Override
		public int compareTo(Object reference) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
}
