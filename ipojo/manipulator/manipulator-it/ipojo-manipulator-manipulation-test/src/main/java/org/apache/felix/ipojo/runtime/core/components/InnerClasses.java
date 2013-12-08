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

package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.Job;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

public class InnerClasses implements CheckService {
    
    private String privateObject;
    private int privateInt;
    
    protected String protectedObject;
    protected int protectedInt;
    
    String packageObject;
    int packageInt;
    
    public String publicObject;
    public int publicInt;
    
    private String nonObject = "not-managed";
    private int nonInt = 5;
    
    private static int staticint = 6;

    /**
     * A fake service.
     */
    private Runnable runnable;

    public boolean check() {
        return true;
    }

    private static final Callable<Integer> callable = new Callable<Integer>() {
        public Integer call() {
            return 1;
        }
    };

    public Properties getProps() {
        Properties props = new Properties();
        props.put("publicInner", new PublicNested().doSomething());
        props.put("packageInner", new PackageNested().doSomething());
        props.put("protectedInner", new ProtectedNested().doSomething());
        props.put("privateInner", new PrivateNested().doSomething());
        props.put("constructorInner", new ConstructorNested().doSomething());
        props.put("staticInner", new StaticNested().doSomething());
        props.put("packageStaticInner", new PackageStaticNested().doSomething());
        try {
            props.put("call", callable.call());
        } catch (Exception e) {
            // Ignore.
        }

        Job anonymous = new Job()  {
            public Map doSomething() {
                Map map = new HashMap();
                map.put("publicObject", publicObject);
                map.put("publicInt", new Integer(publicInt));
                map.put("packageObject", packageObject);
                map.put("packageInt", new Integer(packageInt));
                map.put("protectedObject", protectedObject);
                map.put("protectedInt", new Integer(protectedInt));
                map.put("privateObject", privateObject);
                map.put("privateInt", new Integer(privateInt));
                map.put("nonObject", nonObject);
                map.put("nonInt", new Integer(nonInt));
                return map;
            }
        };
        
        props.put("anonymous", anonymous.doSomething());
        props.put("public", new PublicNested());
        
        
        return props;
    }
    
    private class PrivateNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    public class PublicNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    class PackageNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    protected class ProtectedNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    protected static class StaticNested implements Job {
        private Map map = new HashMap();
        
        public Map doSomething() {
            map.put("static", new Boolean(true));
            map.put("staticint", new Integer(staticint));
            return map;
        }
    }

    static class PackageStaticNested implements Job {
        private Map map = new HashMap();

        public Map doSomething() {
            map.put("static", new Boolean(true));
            map.put("staticint", new Integer(staticint));
            return map;
        }
    }
    
    protected class ConstructorNested implements Job {
        Map map = new HashMap();
        public ConstructorNested() {
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
        }
        
        public Map doSomething() {
            return map;
        }
    }
    

}

