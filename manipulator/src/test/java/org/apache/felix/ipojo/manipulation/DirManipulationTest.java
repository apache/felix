package org.apache.felix.ipojo.manipulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.junit.Test;

import test.AnnotatedComponent;

import junit.framework.TestCase;

public class DirManipulationTest extends TestCase {

	@Test
	public void testManifestLocationKept() {

		Pojoization pojoizator = new Pojoization();
		File tmpDir = null, manifestFile = null, testClass = null;
		try {
			// To obtain OS's temp directory.
			File tmpFile = File.createTempFile("pojodir", ".dir");
			String tmpPath = tmpFile.getAbsolutePath();
			tmpFile.delete();

			// Creating directory on temp location
			tmpDir = new File(tmpPath);
			tmpDir.mkdir();
			tmpDir.deleteOnExit();

			// Create manifest file under temp directory
			manifestFile = new File(tmpDir, "MANIFEST.MF");
			new FileOutputStream(manifestFile)
					.write("Manifest-Version: 1.0\r\n".getBytes());
			manifestFile.deleteOnExit();

			// Just to ensure it is not deleted later from test classes.
			AnnotatedComponent safe;

			// Annotated Class File
			File annotedClassPackage = new File(tmpDir, "test");
			annotedClassPackage.deleteOnExit();
			annotedClassPackage.mkdir();
			testClass = new File(annotedClassPackage,
					"AnnotatedComponent.class");
			testClass.deleteOnExit();
			FileOutputStream os = new FileOutputStream(testClass);
			os.write(ManipulatorTest.getBytesFromFile(new File(
					"target/test-classes/test/AnnotatedComponent.class")));
			os.close();

			// Issue directory manipulation
			pojoizator.directoryPojoization(tmpDir, null, manifestFile);

			// Check if supplied manifest file is altered in place
			BufferedReader fi = new BufferedReader(new FileReader(manifestFile));
			String manifestLine;
			while ((manifestLine = fi.readLine()) != null) {
				if (manifestLine.contains("iPOJO-Components")) {
					assertTrue(true);
					return;
				}
			}

			assertTrue(
					"Directory Manipulation didn't use supplied manifest file as output",
					false);

		} catch (IOException e) {
			assertTrue(
					"File system error occured while testing directory manipulation",
					false);
		}

	}

}
