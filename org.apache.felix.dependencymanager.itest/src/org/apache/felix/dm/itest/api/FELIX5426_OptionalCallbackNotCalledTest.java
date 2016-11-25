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
package org.apache.felix.dm.itest.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * Validates that optional dependency callbacks are properly called in a special use case, where
 * a circular dependency exists between two components.
 */
public class FELIX5426_OptionalCallbackNotCalledTest extends TestBase {

	final Ensure m_ensure = new Ensure();
	
	public void testCleanupDependenciesDuringComponentRemove() {
		DependencyManager m = getDM();
		
		BookStore store = new BookStore();
		Component bookStore = m.createComponent()
				.setImplementation(store).setInterface(BookStore.class.getName(), null)
				.add(m.createServiceDependency().setService(Book.class).setCallbacks("added", "removed").setRequired(false));

		Component book1 = m.createComponent()
				.setImplementation(new Book()).setInterface(Book.class.getName(), null)
				.add(m.createServiceDependency().setService(BookStore.class).setRequired(true));

		Component book2 = m.createComponent()
				.setImplementation(new Book()).setInterface(Book.class.getName(), null)
				.add(m.createServiceDependency().setService(BookStore.class).setRequired(true));		
		
		m.add(bookStore);
		m.add(book1);
		m.add(book2);
		
		m.remove(bookStore);		
		Assert.assertEquals(0, store.getBooksCount());
	}
		
	class BookStore {
		final List<Book> m_books = new ArrayList<>();
		
		private void added(Book book) { // injected, optional
			m_books.add(book);
		}
		
		private void removed(Book book)  {
			m_books.remove(book);
		}
		
		public int getBooksCount() {
			return m_books.size();
		}
	}
	
	class Book {
		private BookStore m_shop; // injected, required
		
		public void start() {
			Assert.assertNotNull(m_shop);
		}
	}

}
