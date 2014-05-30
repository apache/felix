package dm.it;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import dm.Component;
import dm.DependencyManager;
import dm.ServiceDependency;

public class AspectRaceTest extends TestBase {
	volatile ExecutorService m_serviceExec;
	volatile ExecutorService m_aspectExec;
	volatile DependencyManager m_dm;
	final static int SERVICES = 3;
	final static int ASPECTS_PER_SERVICE = 3;
	final static int ITERATIONS = 3000;

	public void testConcurrentAspects() {
		try {
			warn("starting aspect race test");
			int cores = Math.max(4, Runtime.getRuntime().availableProcessors());
			// Used to inject S services
			m_serviceExec = Executors.newFixedThreadPool(cores);
			// Used to inject S Aspects
			m_aspectExec = Executors.newFixedThreadPool(cores);

			// Setup test components using dependency manager.
			// We create a Controller which is injected with some S services,
			// and each S services has some aspects (SAspect).

			m_dm = new DependencyManager(context);
			Controller controller = new Controller();
			ServiceDependency dependency = m_dm.createServiceDependency().setService(S.class)
					.setCallbacks("bind", null, "unbind", "swap")
					.setRequired(true);
//			dependency.setDebug("controller");
			Component c = m_dm
					.createComponent()
					.setImplementation(controller)
					.setInterface(Controller.class.getName(), null)
					.setComposition("getComposition")
					.add(dependency);

			m_dm.add(c);

			for (int loop = 1; loop <= ITERATIONS; loop++) {
				//System.out.println("\niteration: " + loop);
				// Perform concurrent injections of "S" service and S aspects
				// into the Controller component;
				debug("Iteration: " + loop);
				Factory f = new Factory();
				f.register();
				
//				System.out.println("Checking...");

				controller.check();
				
//				System.out.println("Done checking...");

				// unregister all services and aspects concurrently
				f.unregister();

				if ((loop) % 100 == 0) {
					warn("Performed " + loop + " tests.");
				}				

	            if (super.errorsLogged()) {
	                throw new IllegalStateException("Race test interrupted (some error occured, see previous logs)");
	            }
			}
		}

		catch (Throwable t) {
			error("Test failed", t);
			Assert.fail("Test failed: " + t.getMessage());
		} finally {
			shutdown(m_serviceExec);
			shutdown(m_aspectExec);
			m_dm.clear();
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
		final int m_step;

		SImpl(int step) {
			m_step = step;
		}

		public void invoke(Ensure e) {
			e.step(m_step);
		}

		public String toString() {
			return "SImpl[" + m_step + "]";
		}
	}

	public static class SAspect implements S {
		volatile S m_next;
		final int m_rank;
		final int m_step;
		final int m_id;
		volatile Component m_component;

		SAspect(int id, int rank) {
			m_rank = rank;
			m_id = id;
			m_step = ASPECTS_PER_SERVICE - rank + 1;
		}

		public synchronized void added(S s) {
			//System.out.println(Thread.currentThread().getId() + " ARTA aspect added " + m_rank + ": " + s );
			if (m_next != null) {
				fail("Adding while expected swap... " + m_rank);
			}
			m_next = s;
		}

		public synchronized void swap(S oldS, S newS) {
			//System.out.println(Thread.currentThread().getId() + " ARTA aspect swap " + m_rank + ": " + oldS + " with " + newS);
			m_next = newS;
		}
		
		public synchronized void removed(S s) {
			//System.out.println(Thread.currentThread().getId() + " ARTA aspect removed " + m_rank + ": " + s );
			m_next = null;
		}

		public synchronized void invoke(Ensure e) {
//			System.out.println("config " + m_rank + ": " + toString() + " " + m_component.getExecutor() + " " + m_component);
			e.step(m_step);
			m_next.invoke(e);
		}

		public String toString() {
			return "SAspect[" + m_id + ", " + m_rank + ", " + m_step + "], " + ((m_next != null) ? m_next.toString() : "null");
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
				m_serviceExec.execute(new Runnable() {
					public void run() {
						try {
							Component c = m_dm.createComponent();
							Hashtable<String, String> props = new Hashtable<String, String>();
							props.put("id", String.valueOf(serviceId));
							c.setInterface(S.class.getName(), props)
									.setImplementation(
											new SImpl(ASPECTS_PER_SERVICE + 1));
							m_services.add(c);
							m_dm.add(c);
							latch.countDown();
						} catch (Throwable e) {
							error(e);
						}
					}
				});

				for (int j = 1; j <= ASPECTS_PER_SERVICE; j++) {
					final int rank = j;
					m_aspectExec.execute(new Runnable() {
						public void run() {
							try {
								SAspect sa = new SAspect(serviceId, rank);
								Component aspect = m_dm.createAspectService(
										S.class, "(id=" + serviceId + ")",
										rank, "added", null, "removed", "swap")
										.setImplementation(sa);
								m_aspects.add(aspect);
								m_dm.add(aspect);
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
//			System.out.println("all registered: aspects=" + m_aspects);
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
				m_serviceExec.execute(new Runnable() {
					public void run() {
						try {
							debug("removing service " + c$);
							m_dm.remove(c$);
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
				m_serviceExec.execute(new Runnable() {
					public void run() {
						try {
							debug("removing service " + c$);
							m_dm.remove(c$);
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
		final HashSet<S> m_services = new HashSet<S>();

		Object[] getComposition() {
			return new Object[] { this, m_compo };
		}

		void bind(ServiceReference sr, Object service) {
//			System.out.println("ARTC bind... " + service);
			S s = (S) sr.getBundle().getBundleContext().getService(sr);
			if (s == null) {
				throw new IllegalStateException(
						"bindA: bundleContext.getService returned null");
			}
			debug("bind " + s);
			synchronized (this) {
				m_services.add(s);
//				System.out.println("service count after bind: " + m_services.size());
			}
		}

		void swap(S previous, S current) {
//			System.out.println("swap...");
//			System.out.println("ARTC swap: " + previous + " with " + current);
			synchronized (this) {
				if (!m_services.remove(previous)) {
					System.out.println("swap: unknow previous service: " + previous);
				}
				m_services.add(current);
//				System.out.println("service count after swap: " + m_services.size());
			}
		}

		void unbind(S a) {
//			System.out.println("ARTC unbind...");
			debug("unbind " + a);
			synchronized (this) {
				m_services.remove(a);
//				System.out.println("service count after unbind: " + m_services.size());
			}
		}

		void check() {
			synchronized (this) {
//				System.out.println("service count: " + m_services.size());
				for (S s : m_services) {
//					System.out.println("checking service: " + s);
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