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
package org.apache.felix.dependencymanager.test2.integration.api;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
public class AspectRaceTest extends TestBase {
	volatile ExecutorService _serviceExec;
	volatile ExecutorService _aspectExec;
	volatile DependencyManager _dm;
	final static int SERVICES = 3;
	final static int ASPECTS_PER_SERVICE = 10;

	@Test
	public void testConcurrentAspects() {
		try {
			warn("starting aspect race test");
			int cores = 10; // Runtime.getRuntime().availableProcessors();
			// Used to inject S services
			_serviceExec = Executors.newFixedThreadPool(cores);
			// Used to inject S Aspects
			_aspectExec = Executors.newFixedThreadPool(cores);

			// Setup test components using dependency manager.
			// We create a Controller which is injected with some S services,
			// and each S services has some aspects (SAspect).

			_dm = new DependencyManager(context);
			Controller controller = new Controller();
			Component c = _dm
					.createComponent()
					.setImplementation(controller)
					.setInterface(Controller.class.getName(), null)
					.setComposition("getComposition")
					.add(_dm.createServiceDependency().setService(S.class)
							.setCallbacks("bind", null, "unbind", "swap")
							.setRequired(true));

			_dm.add(c);

			for (int loop = 1; loop <= 10000; loop++) {
				// Perform concurrent injections of "S" service and S aspects
				// into the Controller component;
				Factory f = new Factory();
				f.register();

				controller.check();

				// unregister all services and aspects concurrently
				f.unregister();

				if ((loop) % 100 == 0) {
					warn("Performed " + loop + " tests.");
				}
			}

			if (super.errorsLogged()) {
				Assert.assertFalse("Test failed.", false);
			}
		}

		catch (Throwable t) {
			error("Test failed", t);
			Assert.fail("Test failed: " + t.getMessage());
		} finally {
			shutdown(_serviceExec);
			shutdown(_aspectExec);
		}
	}

	void shutdown(ExecutorService exec) {
		exec.shutdown();
		try {
			exec.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}

	public interface S {
		void invoke(Ensure e);
	}

	public static class SImpl implements S {
		final int _step;

		SImpl(int step) {
			_step = step;
		}

		public void invoke(Ensure e) {
			e.step(_step);
		}

		public String toString() {
			return "SImpl[" + _step + "]";
		}
	}

	public static class SAspect implements S {
		volatile S _next;
		final int _rank;
		final int _step;

		SAspect(int rank) {
			_rank = rank;
			_step = ASPECTS_PER_SERVICE - rank + 1;
		}

		public void added(S s) {
			_next = s;
		}

		public void swap(S oldS, S newS) {
			_next = newS;
		}

		public void invoke(Ensure e) {
			e.step(_step);
			_next.invoke(e);
		}

		public String toString() {
			return "SAspect[" + _rank + ", " + _step + "]";
		}
	}

	class Factory {
		final ConcurrentLinkedQueue<Component> m_services = new ConcurrentLinkedQueue<Component>();
		final ConcurrentLinkedQueue<Component> m_aspects = new ConcurrentLinkedQueue<Component>();

		public void register() throws InterruptedException {
			final CountDownLatch latch = new CountDownLatch(SERVICES
					+ (ASPECTS_PER_SERVICE * SERVICES));

			for (int i = 1; i <= SERVICES; i++) {
				final int serviceId = i;
				_serviceExec.execute(new Runnable() {
					public void run() {
						try {
							Component c = _dm.createComponent();
							Hashtable<String, String> props = new Hashtable<String, String>();
							props.put("id", String.valueOf(serviceId));
							c.setInterface(S.class.getName(), props)
									.setImplementation(
											new SImpl(ASPECTS_PER_SERVICE + 1));
							m_services.add(c);
							_dm.add(c);
							latch.countDown();
						} catch (Throwable e) {
							error(e);
						}
					}
				});

				for (int j = 1; j <= ASPECTS_PER_SERVICE; j++) {
					final int rank = j;
					_aspectExec.execute(new Runnable() {
						public void run() {
							try {
								SAspect sa = new SAspect(rank);
								Component aspect = _dm.createAspectService(
										S.class, "(id=" + serviceId + ")",
										rank, "added", null, null, "swap")
										.setImplementation(sa);
								debug("adding aspect " + sa);
								m_aspects.add(aspect);
								_dm.add(aspect);
								latch.countDown();
							} catch (Throwable e) {
								error(e);
							}
						}
					});
				}
			}

			if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException(
						"could not register services and aspects timely");
			}

			debug("all registered: aspects=" + m_aspects);
			// Thread.sleep(5000);
		}

		public void unregister() throws InterruptedException,
				InvalidSyntaxException {
			final CountDownLatch latch = new CountDownLatch(SERVICES
					+ (ASPECTS_PER_SERVICE * SERVICES));

			unregisterAspects(latch);
			unregisterServices(latch);

			if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException(
						"could not unregister services and aspects timely");
			}

			if (context.getServiceReference(S.class.getName()) != null) {
				error("could not unregister some services or aspects !");
			}
			debug("unregistered all aspects and services concurrently");
		}

		public void unregisterAspects(final CountDownLatch latch)
				throws InterruptedException, InvalidSyntaxException {
			Component c;
			debug("unregister: aspects=" + m_aspects);

			while ((c = m_aspects.poll()) != null) {
				final Component c$ = c;
				_serviceExec.execute(new Runnable() {
					public void run() {
						try {
							debug("removing service " + c$);
							_dm.remove(c$);
							latch.countDown();
						} catch (Throwable e) {
							error(e);
						}
					}
				});
			}
		}

		public void unregisterServices(final CountDownLatch latch)
				throws InterruptedException {
			Component c;
			debug("unregister: services=" + m_services);

			while ((c = m_services.poll()) != null) {
				final Component c$ = c;
				_serviceExec.execute(new Runnable() {
					public void run() {
						try {
							debug("removing service " + c$);
							_dm.remove(c$);
							latch.countDown();
						} catch (Throwable e) {
							error(e);
						}
					}
				});
			}

			debug("unregistered all services");
		}
	}

	public class Controller {
		final Composition m_compo = new Composition();
		final HashSet<S> _services = new HashSet<S>();

		Object[] getComposition() {
			return new Object[] { this, m_compo };
		}

		void bind(ServiceReference sr) {
			S s = (S) sr.getBundle().getBundleContext().getService(sr);
			if (s == null) {
				throw new IllegalStateException(
						"bindA: bundleContext.getService returned null");
			}
			debug("bind " + s);
			synchronized (this) {
				_services.add(s);
			}
		}

		void swap(S previous, S current) {
			debug("swap: " + previous + "," + current);
			synchronized (this) {
				if (!_services.remove(previous)) {
					error("swap: unknow previous service: " + previous);
				}
				_services.add(current);
			}
		}

		void unbind(S a) {
			debug("unbind " + a);
			synchronized (this) {
				_services.remove(a);
			}
		}

		void check() {
			synchronized (this) {
				for (S s : _services) {
					debug("checking service: " + s + " ...");
					Ensure ensure = new Ensure(false);
					s.invoke(ensure);
				}
			}
		}
	}

	public static class Composition {
	}
}
