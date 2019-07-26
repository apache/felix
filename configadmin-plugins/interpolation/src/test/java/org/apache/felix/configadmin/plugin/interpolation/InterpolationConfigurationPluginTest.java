/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.configadmin.plugin.interpolation;

import org.apache.felix.configadmin.plugin.interpolation.InterpolationConfigurationPlugin;
import org.junit.Test;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;

public class InterpolationConfigurationPluginTest {
    @Test
    public void testModifyConfiguration() throws Exception {
        String envUser = System.getenv("USER");
        String userVar;
        if (envUser == null) {
            envUser = System.getenv("USERNAME"); // maybe we're on Windows
            userVar = "USERNAME";
        } else {
            userVar = "USER";
        }

        String rf = getClass().getResource("/testfile").getFile();

        InterpolationConfigurationPlugin plugin = new InterpolationConfigurationPlugin(
                new File(rf).getParent());

        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("foo", "bar");
        dict.put("replaced", "$[secret:testfile]");
        dict.put("cur.user", "$[env:" + userVar + "]");
        dict.put("intval", 999);
        dict.put(Constants.SERVICE_PID, "my.service");
        plugin.modifyConfiguration(null, dict);

        assertEquals(5, dict.size());
        assertEquals("bar", dict.get("foo"));
        assertEquals("line1\nline2", dict.get("replaced"));
        assertEquals(envUser, dict.get("cur.user"));
        assertEquals("my.service", dict.get(Constants.SERVICE_PID));
        assertEquals(999, dict.get("intval"));
    }

    @Test
    public void testSubdirReplacement() throws Exception {
        String rf = getClass().getResource("/sub/sub2/testfile2").getFile();

        InterpolationConfigurationPlugin plugin = new InterpolationConfigurationPlugin(
                new File(rf).getParentFile().getParent());

        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("substed", "$[secret:sub2/testfile2]");
        dict.put("not1", "$[secret:../testfile]");
        dict.put("not2", "$[secret:sub2/../../testfile.txt]");
        plugin.modifyConfiguration(null, dict);

        assertEquals(3, dict.size());
        assertEquals("the_content", dict.get("substed"));
        assertEquals("$[secret:../testfile]", dict.get("not1"));
        assertEquals("$[secret:sub2/../../testfile.txt]", dict.get("not2"));
    }

    @Test
    public void testReplacement() throws Exception {
        String rf = getClass().getResource("/testfile.txt").getFile();
        InterpolationConfigurationPlugin plugin = new InterpolationConfigurationPlugin(
                new File(rf).getParent());

        assertEquals("xxla la layy", plugin.replaceVariablesFromFile("akey", "xx$[secret:testfile.txt]yy", "apid"));
        String doesNotReplace = "xx$[" + rf + "]yy";
        assertEquals(doesNotReplace, plugin.replaceVariablesFromFile("akey", doesNotReplace, "apid"));
    }

    @Test
    public void testNoReplacement() throws IOException {
        String rf = getClass().getResource("/testfile.txt").getFile();
        InterpolationConfigurationPlugin plugin = new InterpolationConfigurationPlugin(
                new File(rf).getParent());

        assertEquals("foo", plugin.replaceVariablesFromFile("akey", "foo", "apid"));
    }
}
