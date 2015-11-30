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

package org.apache.felix.webconsole.plugins.scriptconsole.internal;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class ScriptEngineManagerTest {

    @Test
    public void simpleConfig() throws Exception
    {
        String config = "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory\n";
        List<String> classNames = ScriptEngineManager.getClassNames(new BufferedReader(new StringReader(config)));
        assertEquals(asList("org.codehaus.groovy.jsr223.GroovyScriptEngineFactory"),classNames);
    }

    @Test
    public void configWithComment() throws Exception
    {
        String config = "#Groovy Script Support\n" +
                "\n" +
                "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory";
        List<String> classNames = ScriptEngineManager.getClassNames(new BufferedReader(new StringReader(config)));
        assertEquals(asList("org.codehaus.groovy.jsr223.GroovyScriptEngineFactory"),classNames);
    }

    @Test
    public void configWithCommentAtEnd() throws Exception
    {
        String config = "\n" +
                "#script engines supported\n" +
                "\n" +
                "com.sun.script.javascript.RhinoScriptEngineFactory #javascript\n" +
                "\n";
        List<String> classNames = ScriptEngineManager.getClassNames(new BufferedReader(new StringReader(config)));
        assertEquals(asList("com.sun.script.javascript.RhinoScriptEngineFactory"),classNames);
    }
}
