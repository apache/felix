package org.apache.felix.ipojo.manipulation.switches;

import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.manipulation.ManipulatedClassLoader;
import org.apache.felix.ipojo.manipulation.Manipulator;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import test.switches.Color;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks the manipulation of switch constructs
 */
public class TestSwitches {


    @Test
    public void testSwitchOnInteger() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Manipulator manipulator = new Manipulator();
        byte[] clazz = manipulator.manipulate(getBytesFromFile(new File
                ("target/test-classes/test/switches/Switches.class")));
        ManipulatedClassLoader classloader = new ManipulatedClassLoader("test.switches.Switches", clazz);

        ClassReader reader = new ClassReader(clazz);
        CheckClassAdapter.verify(reader, false, new PrintWriter(new File("/tmp/class_dump")));

        Class cl = classloader.findClass("test.switches.Switches");
        Object o = cl.newInstance();

        Method method = cl.getMethod("switchOnInteger", new Class[] { Integer.TYPE});
        int zero = (Integer) method.invoke(o, 0);
        int one = (Integer) method.invoke(o, 1);
        int two = (Integer) method.invoke(o, 2);
        int three = (Integer) method.invoke(o, 4);

        Assert.assertEquals(zero, 0);
        Assert.assertEquals(one, 1);
        Assert.assertEquals(two, 2);
        Assert.assertEquals(three, 3);
    }

    @Test
    public void testSwitchOnEnum() throws IOException, ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        Manipulator manipulator = new Manipulator();
        byte[] clazz = manipulator.manipulate(getBytesFromFile(new File
                ("target/test-classes/test/switches/Switches.class")));
        ManipulatedClassLoader classloader = new ManipulatedClassLoader("test.switches.Switches", clazz);
        Map<String, byte[]> inners = new HashMap<String, byte[]>();

        inners.put("test.switches.Switches$1", getBytesFromFile(new File
                ("target/test-classes/test/switches/Switches$1.class")));
        classloader.addInnerClasses(inners);

        FileUtils.writeByteArrayToFile(new File("target/Switches.class"), clazz);

        Class cl = classloader.findClass("test.switches.Switches");
        Object o = cl.newInstance();

        Method method = cl.getMethod("switchOnEnum", new Class[] { Color.class});
        int one = (Integer) method.invoke(o, Color.RED);
        int two = (Integer) method.invoke(o, Color.GREEN);
        int three = (Integer) method.invoke(o, Color.BLUE);

        Assert.assertEquals(one, 1);
        Assert.assertEquals(two, 2);
        Assert.assertEquals(three, 3);
    }

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
}
