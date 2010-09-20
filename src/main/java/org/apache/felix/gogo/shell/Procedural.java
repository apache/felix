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
package org.apache.felix.gogo.shell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;

public class Procedural
{
    static final String[] functions = { "each", "if", "not", "throw", "try", "until",
            "while" };

    public List<Object> each(CommandSession session, Collection<Object> list,
        Function closure) throws Exception
    {
        List<Object> args = new ArrayList<Object>();
        List<Object> results = new ArrayList<Object>();
        args.add(null);

        for (Object x : list)
        {
            checkInterrupt();
            args.set(0, x);
            results.add(closure.execute(session, args));
        }

        return results;
    }

    public Object _if(CommandSession session, Function[] fns) throws Exception
    {
        int length = fns.length;
        if (length < 2)
        {
            throw new IllegalArgumentException(
                "Usage: if {condition} {if-action} ... {else-action}");
        }

        for (int i = 0; i < length; ++i)
        {
            if (i == length - 1 || isTrue(fns[i++].execute(session, null)))
            {
                return fns[i].execute(session, null);
            }
        }

        return null;
    }

    public boolean not(CommandSession session, Function condition) throws Exception
    {
        if (null == condition)
        {
            return true;
        }
        
        return !isTrue(condition.execute(session, null));
    }

    // Reflective.coerce() prefers to construct a new Throwable(String)
    // than to call this method directly.
    public void _throw(String message)
    {
        throw new IllegalArgumentException(message);
    }

    public void _throw(Exception e) throws Exception
    {
        throw e;
    }

    public void _throw(CommandSession session) throws Throwable
    {
        Object exception = session.get("exception");
        if (exception instanceof Throwable)
            throw (Throwable) exception;
        else
            throw new IllegalArgumentException("exception not set or not Throwable.");
    }

    public Object _try(CommandSession session, Function func) throws Exception
    {
        try
        {
            return func.execute(session, null);
        }
        catch (Exception e)
        {
            session.put("exception", e);
            return null;
        }
    }

    public Object _try(CommandSession session, Function func, Function error)
        throws Exception
    {
        try
        {
            return func.execute(session, null);
        }
        catch (Exception e)
        {
            session.put("exception", e);
            return error.execute(session, null);
        }
    }

    public void _while(CommandSession session, Function condition, Function ifTrue)
        throws Exception
    {
        while (isTrue(condition.execute(session, null)))
        {
            ifTrue.execute(session, null);
        }
    }

    public void until(CommandSession session, Function condition, Function ifTrue)
        throws Exception
    {
        while (!isTrue(condition.execute(session, null)))
        {
            ifTrue.execute(session, null);
        }
    }

    private boolean isTrue(Object result) throws InterruptedException
    {
        checkInterrupt();

        if (result == null)
            return false;

        if (result instanceof Boolean)
            return ((Boolean) result).booleanValue();

        if (result instanceof Number)
        {
            if (0 == ((Number) result).intValue())
                return false;
        }

        if ("".equals(result))
            return false;

        if ("0".equals(result))
            return false;

        return true;
    }
    
    private void checkInterrupt() throws InterruptedException
    {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("loop interrupted");
    }
}
