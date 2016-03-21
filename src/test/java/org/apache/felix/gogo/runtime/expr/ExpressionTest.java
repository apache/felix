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
package org.apache.felix.gogo.runtime.expr;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.gogo.runtime.Expression;
import org.junit.Assert;
import org.junit.Test;

public class ExpressionTest {

    @Test
    public void testExpr() {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("a", 2d);
        variables.put("b", 5l);
        variables.put("c", 1l);
        variables.put("d", 2l);
        variables.put("s", " foo ");
        variables.put("t", "bar");

        Assert.assertEquals(4l, new Expression("c+=1, d+=2").eval(variables));

        Assert.assertEquals(" foo ", new Expression("\" foo \"").eval());
        Assert.assertEquals(" foo bar", new Expression("s + t").eval(variables));
        Assert.assertEquals(1l, new Expression("s < t").eval(variables));
        Assert.assertEquals(1l, new Expression("s > t || t == \"bar\"").eval(variables));

        Assert.assertEquals(3l, new Expression("a += 1").eval(variables));
        Assert.assertEquals(3l, variables.get("a"));

        Assert.assertEquals(30l, new Expression("10 + 20 | 30").eval());

        Assert.assertEquals(8l, new Expression("a + b").eval(variables));
        Assert.assertEquals(3l, new Expression("if(a < b, a, b)").eval(variables));

        Assert.assertEquals(16l, new Expression("2 + 2 << 2").eval());
        Assert.assertEquals(8l, new Expression("2 | 2 << 2").eval());
    }

}
