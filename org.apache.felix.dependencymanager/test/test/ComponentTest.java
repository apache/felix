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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.impl.ComponentImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("unused")
public class ComponentTest {
	static class MyComponent {
		public MyComponent() {
		}
	}
	
	@Test
	public void createStartAndStopComponent() {
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(MyComponent.class);
		Assert.assertEquals("should not be available until started", false, c.isAvailable());
		c.start();
		Assert.assertEquals("should be available", true, c.isAvailable());
		c.stop();
		Assert.assertEquals("should no longer be available when stopped", false, c.isAvailable());
	}
	
	@Test
	public void testInitCallbackOfComponent() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			void init() {
				e.step(2);
			}
            void start() {
				e.step(3);
			}
			void stop() {
				e.step(5);
			}
			void destroy() {
				e.step(6);
			}
		});
		e.step(1);
		c.start();
		e.step(4);
		c.stop();
		e.step(7);
	}

	@Test
	public void testAddDependencyFromInitCallback() {
		final Ensure e = new Ensure();
		final SimpleServiceDependency d = new SimpleServiceDependency();
		d.setRequired(true);
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			void init(Component c) {
				e.step(2);
				c.add(d);
			}
			void start() {
				e.step(4);
			}
			void stop() {
				e.step(6);
			}
			void destroy() {
				e.step(7);
			}
		});
		e.step(1);
		c.start();
		e.step(3);
		d.add(new EventImpl()); // NPE?!
		e.step(5);
		d.remove(new EventImpl());
		c.stop();
		e.step(8);
	}
	
    @Test
    public void testAddAvailableDependencyFromInitCallback() {
        final Ensure e = new Ensure();
        final SimpleServiceDependency d = new SimpleServiceDependency();
        d.setRequired(true);
        final SimpleServiceDependency d2 = new SimpleServiceDependency();
        d2.setRequired(true);
        ComponentImpl c = new ComponentImpl();
        c.setImplementation(new Object() {
            void init(Component c) {
                System.out.println("init");
                e.step(2);
                c.add(d);
                d.add(new EventImpl());
                c.add(d2);
            }
            void start() {
                System.out.println("start");
                e.step();
            }
            void stop() {
                System.out.println("stop");
                e.step();
            }
            void destroy() {
                System.out.println("destroy");
                e.step(9);
            }
        });
        e.step(1);
        c.start();
        e.step(5);
        d2.add(new EventImpl());
        e.step(7);
        d.remove(new EventImpl());
        c.stop();
        e.step(10);
    }
    
	@Test
	public void testAtomicallyAddMultipleDependenciesFromInitCallback() {
		final Ensure e = new Ensure();
		final SimpleServiceDependency d = new SimpleServiceDependency();
		d.setRequired(true);

		final SimpleServiceDependency d2 = new SimpleServiceDependency();
		d2.setRequired(true);

		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			void init(Component c) {
				System.out.println("init");
				e.step(2);
				c.add(d, d2); 
				d.add(new EventImpl()); // won't trigger start because d2 is not yet available
			}
			void start() {
				System.out.println("start");
				e.step(4);
			}
			void stop() {
				System.out.println("stop");
				e.step(6);
			}
			void destroy() {
				System.out.println("destroy");
				e.step(7);
			}
		});
		e.step(1);
		c.start();
		e.step(3);
		d2.add(new EventImpl());
		e.step(5);
		d.remove(new EventImpl());
		c.stop();
		e.step(8);
	}

	@Test
	public void createComponentAddDependencyAndStartComponent() {
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(MyComponent.class);
		SimpleServiceDependency d = new SimpleServiceDependency();
		d.setRequired(true);
		c.add(d);
		c.start();
		Assert.assertEquals("should not be available when started because of missing dependency", false, c.isAvailable());
		c.stop();
		c.remove(d);
	}
	
	@Test
	public void createComponentStartItAndAddDependency() {
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(MyComponent.class);
		SimpleServiceDependency d = new SimpleServiceDependency();
		d.setRequired(true);
		c.start();
		Assert.assertEquals("should be available when started", true, c.isAvailable());
		c.add(d);
		Assert.assertEquals("dependency should not be available", false, d.isAvailable());
		Assert.assertEquals("Component should not be available", false, c.isAvailable());
		c.remove(d);
		c.stop();
	}
	
	@Test
	public void createComponentStartItAddDependencyAndMakeDependencyAvailable() {
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(MyComponent.class);
		SimpleServiceDependency d = new SimpleServiceDependency();
		d.setRequired(true);
		c.start();
		c.add(d);
		Assert.assertEquals("Component should not be available: it is started but the dependency is not available", false, c.isAvailable());
		d.add(new EventImpl());
		Assert.assertEquals("dependency is available, component should be too", true, c.isAvailable());
		d.remove(new EventImpl());
		Assert.assertEquals("dependency is no longer available, component should not be either", false, c.isAvailable());
		c.remove(d);
		Assert.assertEquals("dependency is removed, component should be available again", true, c.isAvailable());
		c.stop();
		Assert.assertEquals("Component is stopped, should be unavailable now", false, c.isAvailable());
	}
	
	@Test
	public void createComponentStartItAddDependencyAndListenerMakeDependencyAvailableAndUnavailableImmediately() {
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(MyComponent.class);
		final SimpleServiceDependency d = new SimpleServiceDependency();
		d.setRequired(true);
		ComponentStateListener l = new ComponentStateListener() {
			@Override
			public void changed(Component c, ComponentState state) {
				// make the dependency unavailable
				d.remove(new EventImpl());
			}
		};
		c.start();
		c.add(d);
		// we add a listener here which immediately triggers an 'external event' that
		// makes the dependency unavailable again as soon as it's invoked
		c.add(l);
		Assert.assertEquals("Component unavailable, dependency unavailable", false, c.isAvailable());
		// so even though we make the dependency available here, before our call returns it
		// is made unavailable again
		d.add(new EventImpl());
		Assert.assertEquals("Component *still* unavailable, because the listener immediately makes the dependency unavailable", false, c.isAvailable());
		c.remove(l);
		Assert.assertEquals("listener removed, component still unavailable", false, c.isAvailable());
		c.remove(d);
		Assert.assertEquals("dependency removed, component available", true, c.isAvailable());
		c.stop();
		Assert.assertEquals("Component stopped, should be unavailable", false, c.isAvailable());
	}
	
	@Test
	public void createComponentAddTwoDependenciesMakeBothAvailableAndUnavailable() {
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(MyComponent.class);
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setRequired(true);
		SimpleServiceDependency d2 = new SimpleServiceDependency();
		d2.setRequired(true);
		c.start();
		c.add(d1);
		c.add(d2);
		Assert.assertEquals("Component should be unavailable, both dependencies are too", false, c.isAvailable());
		d1.add(new EventImpl());
		Assert.assertEquals("one dependency available, component should still be unavailable", false, c.isAvailable());
		d2.add(new EventImpl());
		Assert.assertEquals("both dependencies available, component should be available", true, c.isAvailable());
		d1.remove(new EventImpl());
		Assert.assertEquals("one dependency unavailable again, component should be unavailable too", false, c.isAvailable());
		d2.remove(new EventImpl());
		Assert.assertEquals("both dependencies unavailable, component should be too", false, c.isAvailable());
		c.remove(d2);
		Assert.assertEquals("removed one dependency, still unavailable", false, c.isAvailable());
		c.remove(d1);
		Assert.assertEquals("removed the other dependency, component should be available now", true, c.isAvailable());
		c.stop();
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}

	@Test
	public void createComponentAddDependencyMakeAvailableAndUnavailableWithCallbacks() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
            public void add() {
				e.step(1);
			}
            public void remove() {
				e.step(3);
			}
		});
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "remove");
		d1.setRequired(true);
		// add the dependency to the component
		c.add(d1);
		// start the component
		c.start();
		// make the dependency available, we expect the add callback
		// to be invoked here
		d1.add(new EventImpl());
		e.step(2);
		// remove the dependency, should trigger the remove callback
		d1.remove(new EventImpl());
		e.step(4);
		c.stop();
		c.remove(d1);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}
	
	@Test
	public void createAndStartComponentAddDependencyMakeAvailableAndUnavailableWithCallbacks() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step(1);
			}
			public void remove() {
				e.step(3);
			}
		});
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "remove");
		d1.setRequired(true);
		// start the ComponentImpl (it should become available)
		c.start();
		// add the dependency (it should become unavailable)
		c.add(d1);
		// make the dependency available, which should invoke the
		// add callback
		d1.add(new EventImpl());
		e.step(2);
		// make the dependency unavailable, should trigger the
		// remove callback
		d1.remove(new EventImpl());
		e.step(4);
		c.remove(d1);
		c.stop();
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}

	@Test
	public void createComponentAddTwoDependenciesMakeBothAvailableAndUnavailableWithCallbacks() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step();
			}
			public void remove() {
				e.step();
			}
		});
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "remove");
		d1.setRequired(true);
		SimpleServiceDependency d2 = new SimpleServiceDependency();
		d2.setCallbacks("add", "remove");
		d2.setRequired(true);
		// start the component, which should become active because there are no
		// dependencies yet
		c.start();
		// now add the dependencies, making the ComponentImpl unavailable
		c.add(d1);
		c.add(d2);
		// make the first dependency available, should have no effect on the
		// component
		d1.add(new EventImpl());
		e.step(1);
		// second dependency available, now all the add callbacks should be
		// invoked
		d2.add(new EventImpl());
		e.step(4);
		// remove the first dependency, triggering the remove callbacks
		d1.remove(new EventImpl());
		e.step(7);
		// remove the second dependency, should not trigger more callbacks
		d2.remove(new EventImpl());
		e.step(8);
		c.remove(d2);
		c.remove(d1);
		c.stop();
		// still, no more callbacks should have been invoked
		e.step(9);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}
	
	@Test
	public void createAndStartComponentAddTwoDependenciesMakeBothAvailableAndUnavailableWithCallbacks() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step();
			}
			public void remove() {
				e.step();
			}
		});
		// start the component, it should become available
		c.start();
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "remove");
		d1.setRequired(true);
		SimpleServiceDependency d2 = new SimpleServiceDependency();
		d2.setCallbacks("add", "remove");
		d2.setRequired(true);
		// add the first dependency, ComponentImpl should be unavailable
		c.add(d1);
		c.add(d2);
		// make first dependency available, ComponentImpl should still be unavailable
		d1.add(new EventImpl());
		e.step(1);
		// make second dependency available, ComponentImpl available, callbacks should
		// be invoked
		d2.add(new EventImpl());
		e.step(4);
		// remove the first dependency, callbacks should be invoked
		d1.remove(new EventImpl());
		e.step(7);
		// remove second dependency, no callbacks should be invoked
		d2.remove(new EventImpl());
		e.step(8);
		c.remove(d2);
		c.remove(d1);
		c.stop();
		e.step(9);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}

	@Test
	public void createAndStartComponentAddTwoDependenciesWithMultipleServicesWithCallbacks() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step();
			}
			public void remove() {
				e.step();
			}
		});
		// start component
		c.start();
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "remove");
		d1.setRequired(true);
		SimpleServiceDependency d2 = new SimpleServiceDependency();
		d2.setCallbacks("add", "remove");
		d2.setRequired(true);
		c.add(d1);
		c.add(d2);
		// add three instances to first dependency, no callbacks should
		// be triggered
		d1.add(new EventImpl(1));
		d1.add(new EventImpl(2));
		d1.add(new EventImpl(3));
		e.step(1);
		// add two instances to the second dependency, callbacks should
		// be invoked (4x)
		d2.add(new EventImpl(1));
		e.step(6);
		// add another dependency, triggering another callback
		d2.add(new EventImpl(2));
		e.step(8);
		// remove first dependency (all three of them) which makes the 
		// ComponentImpl unavailable so it should trigger calling remove for
		// all of them (so 5x)
		d1.remove(new EventImpl(1));
		d1.remove(new EventImpl(2));
		d1.remove(new EventImpl(3));
		e.step(14);
		// remove second dependency, should not trigger further callbacks
		d2.remove(new EventImpl(1));
		d2.remove(new EventImpl(2));
		e.step(15);
		c.remove(d2);
		c.remove(d1);
		c.stop();
		e.step(16);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}
	
	@Test
	public void createComponentAddDependencyMakeAvailableChangeAndUnavailableWithCallbacks() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step(1);
			}
			public void change() {
				e.step(3);
			}
			public void remove() {
				e.step(5);
			}
		});
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "change", "remove");
		d1.setRequired(true);
		// add the dependency to the component
		c.add(d1);
		// start the component
		c.start();
		// make the dependency available, we expect the add callback
		// to be invoked here
		d1.add(new EventImpl());
		e.step(2);
		// change the dependency
		d1.change(new EventImpl());
		e.step(4);
		// remove the dependency, should trigger the remove callback
		d1.remove(new EventImpl());
		e.step(6);
		c.stop();
		c.remove(d1);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}

	@Test
	public void createComponentWithOptionalDependency() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step(1);
			}
			public void change() {
				e.step(3);
			}
			public void remove() {
				e.step(5);
			}
		});
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "change", "remove");
		d1.setRequired(false);
		// add the dependency to the component
		c.add(d1);
		// start the component
		c.start();
		Assert.assertEquals("Component started with an optional dependency, should be available", true, c.isAvailable());
		// make the dependency available, we expect the add callback
		// to be invoked here
		d1.add(new EventImpl());
		e.step(2);
		Assert.assertEquals("Component started with an optional dependency, should be available", true, c.isAvailable());
		// change the dependency
		d1.change(new EventImpl());
		e.step(4);
		Assert.assertEquals("Component started with an optional dependency, should be available", true, c.isAvailable());
		// remove the dependency, should trigger the remove callback
		d1.remove(new EventImpl());
		Assert.assertEquals("Component started with an optional dependency, should be available", true, c.isAvailable());
		e.step(6);
		c.stop();
		c.remove(d1);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}

	@Test
	public void createComponentWithOptionalAndRequiredDependency() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			public void add() {
				e.step();
			}
			public void remove() {
				e.step();
			}
		});
		SimpleServiceDependency d1 = new SimpleServiceDependency();
		d1.setCallbacks("add", "remove");
		d1.setRequired(false);
		SimpleServiceDependency d2 = new SimpleServiceDependency();
		d2.setCallbacks("add", "remove");
		d2.setRequired(true);
		// add the dependencies to the component
		c.add(d1);
		c.add(d2);
		// start the component
		c.start();
		Assert.assertEquals("Component started with a required and optional dependency, should not be available", false, c.isAvailable());
		// make the optional dependency available
		d1.add(new EventImpl());
		e.step(1);
		Assert.assertEquals("Component should not be available", false, c.isAvailable());
		// make the required dependency available
		d2.add(new EventImpl());
		e.step(4);
		Assert.assertEquals("Component should be available", true, c.isAvailable());
		// remove the optional dependency
		d1.remove(new EventImpl());
		e.step(6);
		Assert.assertEquals("Component should be available", true, c.isAvailable());
		// remove the required dependency
		d1.remove(new EventImpl());
		e.step(8);
		Assert.assertEquals("Component should be available", true, c.isAvailable());
		c.stop();
		c.remove(d1);
		Assert.assertEquals("Component stopped, should be unavailable again", false, c.isAvailable());
	}
	
	@Test
	public void createComponentAddAvailableDependencyRemoveDependencyCheckStopCalledBeforeUnbind() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			void add() {
				e.step(1);
			}
			void start() {
				e.step(2);
			}
			void stop() {
				e.step(4);
			}
			void remove() {
				e.step(5);
			}
		});
		SimpleServiceDependency d = new SimpleServiceDependency();
		d.setCallbacks("add", "remove");
		d.setRequired(true);
		// add the dependency to the component
		c.add(d);
		// start the component
		c.start();
		// make the dependency available, we expect the add callback
		// to be invoked here, then start is called.
		d.add(new EventImpl());
		e.step(3);
		// remove the dependency, should trigger the stop, then remove callback
		d.remove(new EventImpl());
		e.step(6);
		c.stop();
		c.remove(d);
		Assert.assertEquals("Component stopped, should be unavailable", false, c.isAvailable());
	}
	
	@Test
	public void createDependenciesWithCallbackInstance() {
		final Ensure e = new Ensure();
		ComponentImpl c = new ComponentImpl();
		c.setImplementation(new Object() {
			void start() {
				e.step(2);
			}
			
			void stop() {
				e.step(4);
			}
		});
		
		Object callbackInstance = new Object() {
			void add() {
				e.step(1);
			}
			
			void remove() {
				e.step(5);
			}
		};		
		
		SimpleServiceDependency d = new SimpleServiceDependency();
		d.setCallbacks(callbackInstance, "add", "remove");
		d.setRequired(true);
		// add the dependency to the component
		c.add(d);
		// start the component
		c.start();
		// make the dependency available, we expect the add callback
		// to be invoked here, then start is called.
		d.add(new EventImpl());
		e.step(3);
		// remove the dependency, should trigger the stop, then remove callback
		d.remove(new EventImpl());
		e.step(6);
		c.stop();
		c.remove(d);
		Assert.assertEquals("Component stopped, should be unavailable", false, c.isAvailable());
	}
}
