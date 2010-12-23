package org.apache.felix.ipojo.manipulation;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.manipulator.Pojoization;

public class PojoizationTest extends TestCase {

	public void testJarManipulation() {
		Pojoization pojoization = new Pojoization();
		File in = new File("target/test-classes/tests.manipulation-no-annotations.jar");
		File out = new File("target/test-classes/tests.manipulation-no-annotations-manipulated.jar");
		out.delete();
		File metadata = new File("target/test-classes/metadata.xml");
		pojoization.pojoization(in, out, metadata);

		Assert.assertTrue(out.exists());
	}

	public void testManipulationWithAnnotations() {
		Pojoization pojoization = new Pojoization();
		File in = new File("target/test-classes/tests.manipulator-annotations.jar");
		File out = new File("target/test-classes/tests.manipulation-annotations-manipulated.jar");
		out.delete();
		pojoization.pojoization(in, out, (File) null);

		Assert.assertTrue(out.exists());
	}

	public void testJarManipulationJava5() {
		Pojoization pojoization = new Pojoization();
		File in = new File("target/test-classes/tests.manipulation.java5.jar");
		File out = new File("target/test-classes/tests.manipulation.java5-manipulated.jar");
		out.delete();
		pojoization.pojoization(in, out, (File) null);

		Assert.assertTrue(out.exists());
	}

}
