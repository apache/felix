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
package org.apache.felix.sigil.gogo.junit;


import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;


public class SigilTestAdapter
{
    public TestCase newTest( final CommandSession session, final String name, final Function f, final Object... args )
    {
        return new TestCase( name )
        {
            public int countTestCases()
            {
                return 1;
            }


            public void run( TestResult result )
            {
                try
                {
                    f.execute( session, Arrays.asList( args ) );
                }
                catch ( InvocationTargetException e )
                {
                    Throwable c = e.getCause();
                    if ( c instanceof AssertionFailedError )
                    {
                        result.addFailure( this, ( AssertionFailedError ) c );
                    }
                    else
                    {
                        result.addError( this, c );
                    }
                }
                catch ( AssertionFailedError e )
                {
                    result.addFailure( this, e );
                }
                catch ( Throwable t )
                {
                    result.addError( this, t );
                }
            }
        };
    }


    public TestSuite newTestSuite( String name, Test... tests )
    {
        TestSuite suite = new TestSuite( name );
        for ( Test t : tests )
        {
            suite.addTest( t );
        }
        return suite;
    }
}
