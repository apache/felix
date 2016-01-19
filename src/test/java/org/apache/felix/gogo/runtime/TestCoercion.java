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
package org.apache.felix.gogo.runtime;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;

public class TestCoercion extends BaseTestCase
{
    public boolean fBool(boolean t)
    {
        return t;
    }

    public double fDouble(double x)
    {
        return x;
    }

    public int fInt(int x)
    {
        return x;
    }

    public long fLong(long x)
    {
        return x;
    }

    // the presence of this method checks that it is not invoked instead of fInt(int)
    public String fInt(Object[] x)
    {
        return "array";
    }

    public String fString(String s)
    {
        return s;
    }

    public void testSimpleTypes() throws Exception
    {
        m_ctx.addCommand("fBool", this);
        m_ctx.addCommand("fDouble", this);
        m_ctx.addCommand("fInt", this);
        m_ctx.addCommand("fLong", this);
        m_ctx.addCommand("fString", this);

        assertEquals("fBool true", true, m_ctx.execute("fBool true"));
        assertEquals("fBool 'false'", false, m_ctx.execute("fBool 'false'"));

        assertEquals("fDouble 11", 11.0, m_ctx.execute("fDouble 11"));
        assertEquals("fDouble '11'", 11.0, m_ctx.execute("fDouble '11'"));

        assertEquals("fInt 22", 22, m_ctx.execute("fInt 22"));
        assertEquals("fInt '23'", 23, m_ctx.execute("fInt '23'"));
        assertEquals("fInt 1 2", "array", m_ctx.execute("fInt 1 2"));

        assertEquals("fLong 33", 33L, m_ctx.execute("fLong 33"));
        assertEquals("fLong '34'", 34L, m_ctx.execute("fLong '34'"));

        assertEquals("fString wibble", "wibble", m_ctx.execute("fString wibble"));
        assertEquals("fString 'wibble'", "wibble", m_ctx.execute("fString 'wibble'"));

        try
        {
            Object r = m_ctx.execute("fString ");
            fail("too few args: expected IllegalArgumentException, got: " + r);
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            Object r = m_ctx.execute("fString a b");
            fail("too many args: expected IllegalArgumentException, got: " + r);
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            Object r = m_ctx.execute("fLong string");
            fail("wrong arg type: expected IllegalArgumentException, got: " + r);
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public String bundles(Long id)
    {
        return "long";
    }

    public String bundles(String loc)
    {
        return "string";
    }

    public void testBestCoercion() throws Exception
    {
        m_ctx.addCommand("bundles", this);

        assertEquals("bundles myloc", "string", m_ctx.execute("bundles myloc"));
        assertEquals("bundles 1", "long", m_ctx.execute("bundles 1"));
        assertEquals("bundles '1'", "string", m_ctx.execute("bundles '1'"));
    }

    @Descriptor("list all installed bundles")
    public String p0(
        @Descriptor("show location") @Parameter(names = { "-l", "--location" }, presentValue = "true", absentValue = "false") boolean showLoc,
        @Descriptor("show symbolic name") @Parameter(names = { "-s", "--symbolicname" }, presentValue = "true", absentValue = "false") boolean showSymbolic)
    {
        return showLoc + ":" + showSymbolic;
    }

    // function with session and parameter
    public boolean p01(
        CommandSession session,
        @Parameter(names = { "-f", "--flag" }, presentValue = "true", absentValue = "false") boolean flag)
    {
        assertNotNull("session must not be null", session);
        return flag;
    }

    public void testParameter0() throws Exception
    {
        m_ctx.addCommand("p0", this);
        m_ctx.addCommand("p01", this);

        assertEquals("p0", "false:false", m_ctx.execute("p0"));
        assertEquals("p0 -l", "true:false", m_ctx.execute("p0 -l"));
        assertEquals("p0 --location", "true:false", m_ctx.execute("p0 --location"));
        assertEquals("p0 -l -s", "true:true", m_ctx.execute("p0 -l -s"));
        assertEquals("p0 -s -l", "true:true", m_ctx.execute("p0 -s -l"));
        try
        {
            Object r = m_ctx.execute("p0 wibble");
            fail("too many args: expected IllegalArgumentException, got: " + r);
        }
        catch (IllegalArgumentException e)
        {
        }
        assertEquals("p01 -f", true, m_ctx.execute("p01 -f"));
    }

    public String p1(
        @Parameter(names = { "-p", "--parameter" }, absentValue = "absent") String parm1)
    {
        return parm1;
    }

    public void testParameter1() throws Exception
    {
        m_ctx.addCommand("p1", this);

        assertEquals("no parameter", "absent", m_ctx.execute("p1"));

        // FELIX-2894
        assertEquals("correct parameter", "wibble", m_ctx.execute("p1 -p wibble"));

        try
        {
            Object r = m_ctx.execute("p1 -p");
            fail("missing parameter: expected IllegalArgumentException, got: " + r);
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            Object r = m_ctx.execute("p1 -X");
            fail("wrong parameter: expected IllegalArgumentException, got: " + r);
        }
        catch (IllegalArgumentException e)
        {
        }
    }
}
