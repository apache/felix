package org.apache.felix.ipojo.manipulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.manipulation.annotations.MetadataCollector;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;

public class ManipulatorTest extends TestCase {

	public void testManipulatingTheSimplePojo() throws Exception {
		Manipulator manipulator = new Manipulator();
		byte[] clazz = manipulator.manipulate(getBytesFromFile(new File("target/test-classes/test/SimplePojo.class")));
		TestClassLoader classloader = new TestClassLoader("test.SimplePojo", clazz);
		Class cl = classloader.findClass("test.SimplePojo");
		Assert.assertNotNull(cl);
		Assert.assertNotNull(manipulator.getManipulationMetadata());

		System.out.println(manipulator.getManipulationMetadata());

		// The manipulation add stuff to the class.
		Assert.assertTrue(clazz.length > getBytesFromFile(new File("target/test-classes/test/SimplePojo.class")).length);


		boolean found = false;
		Constructor cst = null;
		Constructor[] csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 1  &&
					csts[i].getParameterTypes()[0].equals(InstanceManager.class)) {
				found = true;
				cst = csts[i];
			}
		}
		Assert.assertTrue(found);

		// We still have the empty constructor
		found = false;
		csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 0) {
				found = true;
			}
		}
		Assert.assertTrue(found);

		// Check the POJO interface
		Assert.assertTrue(Arrays.asList(cl.getInterfaces()).contains(Pojo.class));

		cst.setAccessible(true);
		Object pojo = cst.newInstance(new Object[] {new InstanceManager()});
		Assert.assertNotNull(pojo);
		Assert.assertTrue(pojo instanceof Pojo);

		Method method = cl.getMethod("doSomething", new Class[0]);
		Assert.assertTrue(((Boolean) method.invoke(pojo, new Object[0])).booleanValue());

	}

	public void testManipulatingChild() throws Exception {
		Manipulator manipulator = new Manipulator();
		byte[] clazz = manipulator.manipulate(getBytesFromFile(new File("target/test-classes/test/Child.class")));
		TestClassLoader classloader = new TestClassLoader("test.Child", clazz);
		Class cl = classloader.findClass("test.Child");
		Assert.assertNotNull(cl);
		Assert.assertNotNull(manipulator.getManipulationMetadata());

		boolean found = false;
		Constructor cst = null;
		Constructor[] csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 1  &&
					csts[i].getParameterTypes()[0].equals(InstanceManager.class)) {
				found = true;
				cst = csts[i];
			}
		}
		Assert.assertTrue(found);

		// We still have the regular constructor
		found = false;
		csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 2) {
				found = true;
			}
		}
		Assert.assertTrue(found);

		// Check the POJO interface
		Assert.assertTrue(Arrays.asList(cl.getInterfaces()).contains(Pojo.class));

		InstanceManager im = (InstanceManager) Mockito.mock(InstanceManager.class);
		cst.setAccessible(true);
		Object pojo = cst.newInstance(new Object[] {im});
		Assert.assertNotNull(pojo);
		Assert.assertTrue(pojo instanceof Pojo);

		Method method = cl.getMethod("doSomething", new Class[0]);
		Assert.assertEquals(9, ((Integer) method.invoke(pojo, new Object[0])).intValue());

	}

	public void testManipulatingTheInner() throws Exception {
		Manipulator manipulator = new Manipulator();
		byte[] clazz = manipulator.manipulate(getBytesFromFile(new File("target/test-classes/test/PojoWithInner.class")));
		TestClassLoader classloader = new TestClassLoader("test.PojoWithInner", clazz);
		Class cl = classloader.findClass("test.PojoWithInner");
		Assert.assertNotNull(cl);
		Assert.assertNotNull(manipulator.getManipulationMetadata());
		Assert.assertFalse(manipulator.getInnerClasses().isEmpty());


		System.out.println(manipulator.getManipulationMetadata());

		// The manipulation add stuff to the class.
		Assert.assertTrue(clazz.length > getBytesFromFile(new File("target/test-classes/test/PojoWithInner.class")).length);


		boolean found = false;
		Constructor cst = null;
		Constructor[] csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 1  &&
					csts[i].getParameterTypes()[0].equals(InstanceManager.class)) {
				found = true;
				cst = csts[i];
			}
		}
		Assert.assertTrue(found);

		// We still have the empty constructor
		found = false;
		csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 0) {
				found = true;
			}
		}
		Assert.assertTrue(found);

		// Check the POJO interface
		Assert.assertTrue(Arrays.asList(cl.getInterfaces()).contains(Pojo.class));

		InstanceManager im = (InstanceManager) Mockito.mock(InstanceManager.class);
		cst.setAccessible(true);
		Object pojo = cst.newInstance(new Object[] {im});
		Assert.assertNotNull(pojo);
		Assert.assertTrue(pojo instanceof Pojo);

		Method method = cl.getMethod("doSomething", new Class[0]);
		Assert.assertTrue(((Boolean) method.invoke(pojo, new Object[0])).booleanValue());

	}

	public void testManipulatingWithConstructorModification() throws Exception {
		Manipulator manipulator = new Manipulator();
		byte[] clazz = manipulator.manipulate(getBytesFromFile(new File("target/test-classes/test/Child.class")));
		TestClassLoader classloader = new TestClassLoader("test.Child", clazz);
		Class cl = classloader.findClass("test.Child");
		Assert.assertNotNull(cl);
		Assert.assertNotNull(manipulator.getManipulationMetadata());

		boolean found = false;
		Constructor cst = null;
		Constructor[] csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 1  &&
					csts[i].getParameterTypes()[0].equals(InstanceManager.class)) {
				found = true;
				cst = csts[i];
			}
		}
		Assert.assertTrue(found);

		// We still have the regular constructor
		found = false;
		csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 2) {
				found = true;
			}
		}
		Assert.assertTrue(found);

		// Check that we have the IM, Integer, String constructor too
		Constructor cst2 = cl.getDeclaredConstructor(new Class[] { InstanceManager.class, Integer.TYPE, String.class });
		Assert.assertNotNull(cst2);

		// Check the POJO interface
		Assert.assertTrue(Arrays.asList(cl.getInterfaces()).contains(Pojo.class));


		// Creation using cst
		InstanceManager im = (InstanceManager) Mockito.mock(InstanceManager.class);
		cst.setAccessible(true);
		Object pojo = cst.newInstance(new Object[] {im});
		Assert.assertNotNull(pojo);
		Assert.assertTrue(pojo instanceof Pojo);

		Method method = cl.getMethod("doSomething", new Class[0]);
		Assert.assertEquals(9, ((Integer) method.invoke(pojo, new Object[0])).intValue());

		// Try to create using cst2
		im = (InstanceManager) Mockito.mock(InstanceManager.class);
		cst2.setAccessible(true);
		pojo = cst2.newInstance(new Object[] {im, new Integer(2), "bariton"});
		Assert.assertNotNull(pojo);
		Assert.assertTrue(pojo instanceof Pojo);

		method = cl.getMethod("doSomething", new Class[0]);
		Assert.assertEquals(10, ((Integer) method.invoke(pojo, new Object[0])).intValue());



	}


	public void testManipulatingWithNoValidConstructor() throws Exception {
		Manipulator manipulator = new Manipulator();
		byte[] clazz = manipulator.manipulate(getBytesFromFile(new File("target/test-classes/test/NoValidConstructor.class")));
		TestClassLoader classloader = new TestClassLoader("test.NoValidConstructor", clazz);
		Class cl = classloader.findClass("test.NoValidConstructor");
		Assert.assertNotNull(cl);
		Assert.assertNotNull(manipulator.getManipulationMetadata());

		System.out.println(manipulator.getManipulationMetadata());

		// The manipulation add stuff to the class.
		Assert.assertTrue(clazz.length > getBytesFromFile(new File("target/test-classes/test/NoValidConstructor.class")).length);


		boolean found = false;
		Constructor cst = null;
		Constructor[] csts = cl.getDeclaredConstructors();
		for (int i = 0; i < csts.length; i++) {
			System.out.println(Arrays.asList(csts[i].getParameterTypes()));
			if (csts[i].getParameterTypes().length == 1  &&
					csts[i].getParameterTypes()[0].equals(InstanceManager.class)) {
				found = true;
				cst = csts[i];
			}
		}
		Assert.assertTrue(found);

		// Check the POJO interface
		Assert.assertTrue(Arrays.asList(cl.getInterfaces()).contains(Pojo.class));

		cst.setAccessible(true);
		Object pojo = cst.newInstance(new Object[] {new InstanceManager()});
		Assert.assertNotNull(pojo);
		Assert.assertTrue(pojo instanceof Pojo);

	}

//	public void test() throws Exception {
//
//
//		byte[] clazz = getBytesFromFile(new File("target/test-classes/test/Constructor.class"));
//		ClassReader cr = new ClassReader(clazz);
//        MetadataCollector collector = new MetadataCollector();
//        cr.accept(collector, 0);
//
//        System.out.println(collector.getComponentTypeDeclaration());
//
//	}

	public static byte[] getBytesFromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);
	    long length = file.length();
	    byte[] bytes = new byte[(int)length];

	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    // Close the input stream and return bytes
	    is.close();
	    return bytes;
	}

	class TestClassLoader extends ClassLoader {

		private String name;
		private byte[] clazz;

		public TestClassLoader(String name, byte[] clazz) {
			this.name = name;
			this.clazz = clazz;
		}

        public Class findClass(String name) throws ClassNotFoundException {
        	if (name.equals(this.name)) {
	            return defineClass(name, clazz, 0, clazz.length);
        	}
        	return super.findClass(name);
        }

		public Class loadClass(String arg0) throws ClassNotFoundException {
			return super.loadClass(arg0);
		}



    }

}
