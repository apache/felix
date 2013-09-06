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

package test;

import org.apache.felix.ipojo.annotations.*;
import test.ipojo.ExternalHandler;

import java.util.List;

@Component
@Instantiate
public class PlentyOfAnnotations {

    @Requires
    List list;
    private String m_prop;
    private Runnable m_runnable;
    private String m_stuff;

    @ExternalHandler
    private String stuff2;


    PlentyOfAnnotations(@Property String prop, @Requires Runnable runnable, @ExternalHandler String stuff) {

        m_prop = prop;
        m_runnable = runnable;
        m_stuff = stuff;

    }

    @Validate
    public void start() {
        //...
    }

    @ExternalHandler
    public void stuff() {
        // ...
    }

    public String doSomethingWithArguments(String message, int value) {
        return message + " - " + value;
    }

}
