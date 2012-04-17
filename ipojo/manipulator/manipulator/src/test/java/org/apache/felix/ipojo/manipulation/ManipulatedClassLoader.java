package org.apache.felix.ipojo.manipulation;

/**
 * A classloader used to load manipulated classes.
 */
class ManipulatedClassLoader extends ClassLoader {

    private String name;
    private byte[] clazz;

    public ManipulatedClassLoader(String name, byte[] clazz) {
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
