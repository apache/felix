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

/**
 * A classloader used to load manipulated classes.
 */
public class ManipulatedClassLoader extends ClassLoader {

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
