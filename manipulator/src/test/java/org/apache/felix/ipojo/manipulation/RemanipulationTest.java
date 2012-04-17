package org.apache.felix.ipojo.manipulation;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.metadata.AnnotationMetadataProvider;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.metadata.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Test Case for FELIX-3461.
 * Checks the consistency of multiple manipulation.
 */
public class RemanipulationTest extends TestCase {

    /**
     * Tests checking that the consecutive manipulation does still returns valid metadata (from annotations),
     * and valid manipulation metadata.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testDoubleManipulationWithAnnotations() throws IOException, ClassNotFoundException {
        Reporter reporter = mock(Reporter.class);
        // Step 1 - First collection and manipulation
        //1.1 Metadata collection
        MiniStore store = new MiniStore()
                .addClassToStore("test.PlentyOfAnnotations",
                        ManipulatorTest.getBytesFromFile(new File("target/test-classes/test/PlentyOfAnnotations.class")));
        AnnotationMetadataProvider provider = new AnnotationMetadataProvider(store, reporter);
        List<Element> originalMetadata = provider.getMetadatas();
        // 1.2 Manipulation
        Manipulator manipulator = new Manipulator();
        byte[] clazz = manipulator.manipulate(
                ManipulatorTest.getBytesFromFile(new File("target/test-classes/test/PlentyOfAnnotations.class")));
        Element originalManipulationMetadata = manipulator.getManipulationMetadata();
        // 1.3 Check that the class is valid
        ManipulatedClassLoader classloader = new ManipulatedClassLoader("test.PlentyOfAnnotations", clazz);
        Class cl = classloader.findClass("test.PlentyOfAnnotations");
        Assert.assertNotNull(cl);

        // ---------------

        // Step 2 - Second collection and manipulation
        // We use the output class as entry.
        // 2.1 Metadata collection
        store = new MiniStore().addClassToStore("test.PlentyOfAnnotations", clazz);
        provider = new AnnotationMetadataProvider(store, reporter);
        List<Element> metadataAfterOneManipulation = provider.getMetadatas();
        // 2.2 Manipulation
        manipulator = new Manipulator();
        byte[] clazz2 = manipulator.manipulate(clazz);
        Element manipulationMetadataAfterSecondManipulation = manipulator.getManipulationMetadata();
        // 2.3 Check that the class is valid
        classloader = new ManipulatedClassLoader("test.PlentyOfAnnotations", clazz);
        cl = classloader.findClass("test.PlentyOfAnnotations");
        Assert.assertNotNull(cl);

        // ---------------

        // Step 3 - Third collection and manipulation
        // We use the output class of 2 as entry.
        // 3.1 Metadata collection
        store = new MiniStore().addClassToStore("test.PlentyOfAnnotations", clazz2);
        provider = new AnnotationMetadataProvider(store, reporter);
        List<Element> metadataAfterTwoManipulation = provider.getMetadatas();
        // 3.2 Manipulation
        manipulator = new Manipulator();
        byte[] clazz3 = manipulator.manipulate(clazz2);
        Element manipulationMetadataAfterThirdManipulation = manipulator.getManipulationMetadata();
        // 3.3 Check that the class is valid
        classloader = new ManipulatedClassLoader("test.PlentyOfAnnotations", clazz);
        cl = classloader.findClass("test.PlentyOfAnnotations");
        Assert.assertNotNull(cl);

        // ---------------
        // Verification

        // Unchanged metadata
        Assert.assertEquals(originalMetadata.toString(), metadataAfterOneManipulation.toString());
        Assert.assertEquals(originalMetadata.toString(), metadataAfterTwoManipulation.toString());

        // Unchanged manipulation metadata
        Assert.assertEquals(originalManipulationMetadata.toString(),
                manipulationMetadataAfterSecondManipulation.toString());
        Assert.assertEquals(originalManipulationMetadata.toString(),
                manipulationMetadataAfterThirdManipulation.toString());

    }

    private class MiniStore implements ResourceStore {

        private Map<String, byte[]> resources;

        public MiniStore addClassToStore(String qualifiedName, byte[] bytes) {
            if (this.resources == null) {
                this.resources = new HashMap<String, byte[]>();
            }
            this.resources.put(Strings.asResourcePath(qualifiedName), bytes);
            return this;
        }

        public byte[] read(String path) throws IOException {
            return resources.get(path);
        }

        public void accept(ResourceVisitor visitor) {
            for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
                visitor.visit(entry.getKey());
            }
        }

        public void open() throws IOException {
        }

        public void writeMetadata(Element metadata) {
        }

        public void write(String resourcePath, byte[] resource) throws IOException {
        }

        public void close() throws IOException {
        }
    }

}
