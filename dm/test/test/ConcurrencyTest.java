package test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import dm.Component;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.context.ComponentState;
import dm.impl.ComponentImpl;
import dm.impl.DependencyImpl;
import dm.impl.EventImpl;

public class ConcurrencyTest {
	
	/**
	 * Ensure actions from another thread than the current thread executing in the SerialExecutor are being
	 * scheduled (added to the queue) rather than being executed immediately.  
	 */
	@Test
	public void createComponentAddDependencyAndListenerAndAddAnotherDependencyInAParallelThread() {
		final Semaphore s = new Semaphore(0);
		final ComponentImpl c = new ComponentImpl();
		final DependencyImpl d = new DependencyImpl();
		d.setRequired(true);
		final Dependency d2 = new DependencyImpl();
		d2.setRequired(true);
		final Thread t = new Thread() {
			public void run() {
				c.add(d2);
				s.release();
			}
		};
		ComponentStateListener l = new ComponentStateListener() {
			@Override
			public void changed(ComponentState state) {
				try {
				    c.remove(this);
				    // launch a second thread interacting with our ComponentImpl and block this thread until the
				    // second thread finished its interaction with our component. We want to ensure the work of 
				    // the second thread is scheduled after our current job in the serial executor and does not
				    // get executed immediately.
				    t.start();
				    s.acquire();
				    Assert.assertEquals("dependency count should be 1", 1, c.getDependencies().size());
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		c.setImplementation(new Object()); // If not, we may see NullPointers when invoking lifecycle callbacks
		c.start();
		c.add(d);
		c.add(l);
		Assert.assertEquals("component should not be available", false, c.isAvailable());
		d.add(new EventImpl()); // sets dependency d to available and triggers our ComponentStateListener
		
		// due to the dependency added by the second thread in the serial executor we still expect our component
		// to be unavailable. This work was scheduled in the serial executor and will be executed by the current
		// thread after it finished handling the job for handling the changed() method.
		Assert.assertEquals("component should not be available", false, c.isAvailable());
		c.remove(l);
		Assert.assertEquals("component should not be available", false, c.isAvailable());
		c.remove(d);
		Assert.assertEquals("component should not be available", false, c.isAvailable());
		c.remove(d2);
		Assert.assertEquals("component should be available", true, c.isAvailable());
		c.stop();
		Assert.assertEquals("component should not be available", false, c.isAvailable());
	}

	@Test
	public void createComponentAddAndRemoveDependenciesInParallelThreads() throws Exception {
		final ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object()); // If not, we may see NullPointers when invoking lifecycle callbacks
		ExecutorService e = Executors.newFixedThreadPool(16); 
		c.start();
		for (int i = 0; i < 1000; i++) {
			e.execute(new Runnable() {
				@Override
				public void run() {
					Dependency d = new DependencyImpl();
					d.setRequired(true);
					c.add(d);
//					d.changed(new EventImpl(true));
//					d.changed(new EventImpl(false));
					c.remove(d);
				}});
		}
		e.shutdown();
		e.awaitTermination(10, TimeUnit.SECONDS);
//		Assert.assertEquals("component should not be available", false, c.isAvailable());
		c.stop();
		Assert.assertEquals("component should not be available", false, c.isAvailable());
	}
}
