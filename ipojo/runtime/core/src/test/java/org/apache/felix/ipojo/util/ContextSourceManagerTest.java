/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

import java.util.Hashtable;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Checks the behavior of the context source manager.
 */
public class ContextSourceManagerTest {

    @Test
    public void testSubstitute() throws Exception {
        String filter_with_one_var = "(id=${id})";
        String filter_with_two_vars = "(&(id=${id})(count=${system.count}))";

        Hashtable<String, Object> context = new Hashtable<String, Object>();
        context.put("id", "my.id");
        context.put("system.count", 1);

        String filter = ContextSourceManager.substitute(filter_with_one_var, context);
        assertThat(filter).isEqualTo("(id=my.id)");

        filter = ContextSourceManager.substitute(filter_with_two_vars, context);
        assertThat(filter).isEqualTo("(&(id=my.id)(count=1))");
    }

    @Test
    public void testEmptySubstitution() {
        String filter_with_two_vars = "(&(id=${id})(count=${system.count}))";
        Hashtable<String, Object> context = new Hashtable<String, Object>();
        String filter = ContextSourceManager.substitute(filter_with_two_vars, context);
        assertThat(filter).isEqualTo(filter_with_two_vars);
    }

    @Test
    public void testTwoStepsSubstitution() {
        String filter_with_vars_equality = "(${attr}=${var})";

        Hashtable<String, Object> context1 = new Hashtable<String, Object>();
        context1.put("var", "value");

        Hashtable<String, Object> context2 = new Hashtable<String, Object>();
        context2.put("attr", "prop");

        String filter = ContextSourceManager.substitute(filter_with_vars_equality, context1);
        assertThat(filter).isEqualTo("(${attr}=value)");

        filter = ContextSourceManager.substitute(filter, context2);
        assertThat(filter).isEqualTo("(prop=value)");
    }

    @Test
    public void testExtractVariablesFromFilter() throws Exception {
        String filter_with_one_var = "(id=${id})";
        String filter_with_two_vars = "(&(id=${id})(count=${system.count}))";
        String filter_with_vars_equality = "(${attr}=${var})";

        assertThat(ContextSourceManager.extractVariablesFromFilter(filter_with_one_var)).containsExactly("id");
        assertThat(ContextSourceManager.extractVariablesFromFilter(filter_with_two_vars)).contains("id",
                "system.count");
        assertThat(ContextSourceManager.extractVariablesFromFilter(filter_with_vars_equality)).contains("attr",
                "var");
    }

    @Test
    public void testBrokenFilters() throws Exception {
        String broken = "(id=${i)";
        try {
            ContextSourceManager.extractVariablesFromFilter(broken);
            fail("Unfinished variable undetected");
        } catch (InvalidSyntaxException e) {
            // OK
        }

        String broken2 = "(id=${})";
        try {
            ContextSourceManager.extractVariablesFromFilter(broken2);
            fail("Empty variable undetected");
        } catch (InvalidSyntaxException e) {
            // OK
        }

        String broken3 = "(id=${I contain a space})";
        try {
            ContextSourceManager.extractVariablesFromFilter(broken3);
            fail("Spaced variable undetected");
        } catch (InvalidSyntaxException e) {
            // OK
        }
    }
}
