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

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Modified;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

@Component
public class Dependency {

    @Requires
    public FooService fs;
    
    @Unbind
    public void unbindBar() {
        
    }
    
    @Bind
    public void bindBar() {
        
    }
    
    @Unbind
    public void unbindBaz() {
        
    }
    
    @Bind
    public void bindBaz() {
        
    }
   
    
    @Requires
    public FooService fs2;
    
    @Bind(id="fs2")
    public void bindFS2() {
        
    }
    
    @Unbind(id="fs2")
    public void unbindFS2() {
        
    }
    
    @Requires(id="inv")
    public FooService fs2inv;
    
    @Bind(id="inv")
    public void bindFS2Inv() {
        
    }
    
    @Unbind(id="inv")
    public void unbindFS2Inv() {
        
    }
    
    @Bind(id="mod")
    public void bindMod() {
        
    }
    
    @Unbind(id="mod")
    public void unbindMod() {
        
    }
    
    @Modified(id="mod")
    public void modifiedMod() {
        
    }
    
    
    @Requires(proxy=false, id="notproxied")
    FooService myFoo;
    
    
}
