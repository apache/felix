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
package org.apache.felix.ipojo.runtime.core.test.components;

import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.osgi.framework.ServiceReference;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

public class CheckServiceProvider extends CheckProviderParentClass implements CheckService {
    
    FooService fs;
    
    int simpleB = 0;
    int objectB = 0;
    int refB = 0;
    int bothB = 0;
    int mapB = 0;
    int dictB = 0;
    
    int modified = 0;

    public boolean check() {
        return fs.foo();
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("voidB", simpleB);
        props.put("objectB", objectB);
        props.put("refB", refB);
        props.put("bothB", bothB);
        props.put("voidU", simpleU);
        props.put("objectU", objectU);
        props.put("refU", refU);
        props.put("bothU", bothU);
        props.put("mapB", mapB);
        props.put("dictB", dictB);
        props.put("mapU", mapU);
        props.put("dictU", dictU);
        if (fs != null) {
            // If nullable = false and proxy, a runtime exception may be launched here
            // catch the exception and add this to props.
            try {
                props.put("exception", Boolean.FALSE); // Set exception to false for checking.
                props.put("result", fs.foo());
                props.put("boolean", fs.getBoolean());
                props.put("int", fs.getInt());
                props.put("long", fs.getLong());
                props.put("double", fs.getDouble());
                if(fs.getObject() != null) { props.put("object", fs.getObject()); }
            } catch (RuntimeException e) {
                props.put("exception", Boolean.TRUE);
            }
        }
        props.put("static", CheckService.foo);
        props.put("class", CheckService.class.getName());
        
        
        // Add modified
        props.put("modified", modified);
        
        return props;
    }
    
    private void voidBind() {
        simpleB++;
    }
    
    public void voidModify() {
        modified ++;
    }
    
    protected void objectBind(FooService o) {
        if (o == null) {
            System.err.println("Bind receive null !!! ");
            return;
        }
        objectB++;
    }
    
    protected void objectModify(FooService o) {
        if (o == null) {
            System.err.println("Bind receive null !!! [" + modified + "]");
            return;
        }
        modified++;
    }
    
    public void refBind(ServiceReference sr) {
        if(sr != null) { refB++; }
    }
    
    public void refModify(ServiceReference sr) {
        if(sr != null) { modified++; }
    }
    
    public void bothBind(FooService o, ServiceReference sr) {
        if(sr != null && o != null) { bothB++; }
    }
    
    public void bothModify(FooService o, ServiceReference sr) {
        if(sr != null && o != null) { modified++; }
    }
    
    protected void propertiesDictionaryBind(FooService o, Dictionary props) {
        if(props != null && o != null && props.size() > 0) { dictB++; }
        fs = o;
    }   
    
    protected void propertiesDictionaryModify(FooService o, Dictionary props) {
        if(props != null && o != null && props.size() > 0) { modified++; }
        fs = o;
    }   
    
    protected void propertiesMapBind(FooService o, Map props) {
        if(props != null && o != null && props.size() > 0) { mapB++; }
        fs = o;
    } 
    
    protected void propertiesMapModify(FooService o, Map props) {
        if(props != null && o != null && props.size() > 0) { modified++; }
        fs = o;
    } 

}
