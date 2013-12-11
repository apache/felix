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

package org.apache.felix.ipojo.manipulation;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A classloader used to load manipulated classes.
 */
public class ManipulatedClassLoader extends ClassLoader {

    private String name;
    private byte[] clazz;

    private Map<String, byte[]> inner = new LinkedHashMap<String, byte[]>();

    public ManipulatedClassLoader(String name, byte[] clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public byte[] get(String name) {
        if (name.equals(this.name)) {
            return clazz;
        }
        if (inner.containsKey(name)) {
            return inner.get(name);
        }
        return null;
    }

    public void addInnerClass(String name, byte[] clazz) {
        inner.put(name, clazz);
    }

    public Class findClass(String name) throws ClassNotFoundException {
        if (name.equals(this.name)) {
            return defineClass(name, clazz, 0, clazz.length);
        }

        if (inner.containsKey(name)) {
            return defineClass(name, inner.get(name), 0, inner.get(name).length);
        }

        return super.findClass(name);
    }

    public Class loadClass(String classname) throws ClassNotFoundException {
        if (inner.containsKey(classname)) {
            return findClass(classname);
        }
        return super.loadClass(classname);
    }

    public static final File DUMP_BASEDIR = new File("target/dump");

    public void dump() throws IOException {
        File outer = new File(DUMP_BASEDIR, name.replace(".", "/") + ".class");
        FileUtils.writeByteArrayToFile(outer, clazz);
        for (String name : inner.keySet()) {
            File file = new File(DUMP_BASEDIR, name.replace(".", "/") + ".class");
            FileUtils.writeByteArrayToFile(file, inner.get(name));
        }
    }

    public Map<String, byte[]> getAllInnerClasses() {
        return inner;
    }

    public void addInnerClassIfNotAlreadyDefined(String name, byte[] innerClassBytecode) {
        if (! inner.containsKey(name)) {
            inner.put(name, innerClassBytecode);
        }
    }
}
