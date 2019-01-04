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
package org.apache.felix.gogo.jline;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class ShellTest extends AbstractParserTest {

    @Test
    public void testAssignmentWithEcho() throws Exception {
        Context context = new Context();
        context.execute("a = \"foo\"");
        Assert.assertEquals("foo", context.get("a"));
        context.execute("a = $(echo bar)");
        Assert.assertEquals("bar", context.get("a"));
    }

    @Test
    public void testLoopBreak() throws Exception {
        Context context = new Context();
        Object result = context.execute("$(each {1..10} { i = $it; if { %(i >= 5) } { break } ; echo $i })");
        Assert.assertEquals("1\n2\n3\n4", result);
    }

    @Test
    public void testJobIds() throws Exception {
        Context context = new Context();
        // TODO: not than in zsh, the same thing is achieved using
        // TODO:     ${${${(@f)"$(jobs)"}%]*}#*\[}
//        Object result = context.execute("sleep 1 & sleep 1 & ${${${(f)\"$(jobs)\"}%']*'}#'*\\['}");
        Object result = context.execute("sleep 1 & sleep 1 & ${${${(f)$(jobs)}%\\]*}#*\\[}");
        Assert.assertEquals(Arrays.asList("1", "2"), result);
    }

}
