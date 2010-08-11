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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.felix.gogo.options.Option;
import org.apache.felix.gogo.options.Options;
import org.apache.felix.sigil.common.junit.server.JUnitService;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.osgi.util.tracker.ServiceTracker;

public class SigilJunitRunner
{
    private ServiceTracker tracker;

    public SigilJunitRunner(ServiceTracker tracker)
    {
        this.tracker = tracker;
    }

    public boolean runTests(Object[] args) throws IOException
    {
        final String[] usage = {
        "runTests - run unit tests",
        "Usage: runTests [OPTION]... [TESTS]...",
        "  -? --help                show help",
        "  -d --directory=DIR       Write test results to specified directory",
        "  -q --quiet               Do not output test results to console"};
        
        Option opts = Options.compile( usage ).parse( args );
        
        boolean quiet = opts.isSet( "quiet" );
        String d = opts.isSet( "directory" ) ? opts.get( "directory" ) : null;
        File dir = null;
        if (d != null)
        {
            dir = new File(d);
            dir.mkdirs();
            if (!quiet) {
                System.out.println("Writing results to " + dir.getAbsolutePath());
                System.out.flush();
            }
        }
        
        List<Object> tests = opts.argObjects();
        
        return runTests(tests, quiet, dir);
    }
    
    public void listTests() {
        JUnitService service = ( JUnitService ) tracker.getService();
        
        if ( service == null ) {
            throw new IllegalStateException(JUnitService.class.getName() + " not found");
        }
        
        for (String t : service.getTests())
        {
            System.out.println("\t" + t);
            System.out.flush();
        }
    }
    
    private boolean runTests(List<Object> tests, boolean quiet, File dir) throws IOException
    {
        int count = 0;
        int failures = 0;
        int errors = 0;
        
        TestSuite[] suites = buildTestSuites(tests);
        
        if (suites.length > 0) {
            // redirect io to capture test output - broken due to gogo bug
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
            ByteArrayOutputStream tempErr = new ByteArrayOutputStream();
            
            System.setOut( new PrintStream( tempOut, false ) );
            System.setErr( new PrintStream( tempErr, false ) );
            
            try {
                for (TestSuite test : suites)
                {
                    TestResult result = new TestResult();
    
                    runTests(test, result, quiet, dir, tempOut, tempErr);
    
                    tempOut.reset();
                    tempErr.reset();
                    count += result.runCount();
                    failures += result.failureCount();
                    errors += result.errorCount();
                }
            }
            finally {
                System.setOut( oldOut );
                System.setErr( oldErr );                    
            }
        }

        System.out.println("Ran " + count + " tests. " + failures + " failures " + errors
            + " errors.");
        System.out.flush();

        return failures + errors == 0;
    }

    /**
     * @param tests
     * @return
     */
    private TestSuite[] buildTestSuites( List<Object> tests )
    {
        ArrayList<TestSuite> suites = new ArrayList<TestSuite>(tests.size());
        
        for (Object o : tests) {
            TestSuite[] s = coerceTest(o);
            if (s.length == 0)
            {
                System.err.println("No tests found for " + o);
            }
            else
            {
                for (TestSuite t : s) {
                    suites.add(t);
                }
            }
        }
        
        return suites.toArray(new TestSuite[suites.size()]);
    }

    /**
     * @param tempOut
     * @param tempErr
     * @throws IOException 
     */
    private void runTests( TestSuite test, TestResult result, boolean quiet, File dir, ByteArrayOutputStream tempOut, ByteArrayOutputStream tempErr ) throws IOException
    {
        if (!quiet)
        {
            result.addListener(new PrintListener());
        }

        JUnitTest antTest = null;
        FileOutputStream fout = null;
        XMLJUnitResultFormatter formatter = null;

        if (dir != null)
        {
            antTest = new JUnitTest(test.getName(), false, false, true);

            formatter = new XMLJUnitResultFormatter();
            formatter.startTestSuite(antTest);

            String name = "TEST-" + test.getName() + ".xml";

            File f = new File(dir, name);
            fout = new FileOutputStream(f);
            formatter.setOutput(fout);
            result.addListener(formatter);
        }
        
        test.run(result);

        System.out.flush();
        System.err.flush();

        if ( dir != null ) {
            formatter.setSystemOutput( tempOut.toString() );
            formatter.setSystemError( tempErr.toString() );
            
            antTest.setCounts(result.runCount(), result.failureCount(),
                result.errorCount());
            
            formatter.endTestSuite(antTest);
            
            fout.flush();
            fout.close();
        }                        
    }

    /**
     * @param o
     * @return
     */
    private TestSuite[] coerceTest( Object o )
    {
        if ( o instanceof TestCase ) {
            TestCase t = ( TestCase ) o;
            TestSuite suite = new TestSuite(t.getName());
            suite.addTest(t);
            return new TestSuite[] { suite };
        }
        else if (o instanceof TestSuite ) {
            return new TestSuite[] { ( TestSuite ) o };
        }
        else if (o instanceof String) {
            return findTests(( String ) o);
        }
        else {
            throw new IllegalArgumentException("Unexpected test type " + o.getClass().getName() );
        }
    }

    private TestSuite[] findTests(String t)
    {
        JUnitService service = ( JUnitService ) tracker.getService();
        
        if ( service == null ) {
            throw new IllegalStateException(JUnitService.class.getName() + " not found");
        }
        
        if (t.contains("*"))
        {
            Pattern p = compile(t);
            LinkedList<TestSuite> tests = new LinkedList<TestSuite>();
            for (String n : service.getTests())
            {
                if (p.matcher(n).matches())
                {
                    tests.add(service.createTest(n));
                }
            }
            return tests.toArray(new TestSuite[tests.size()]);
        }
        else
        {
            TestSuite test = service.createTest(t);
            return test == null ? new TestSuite[0] : new TestSuite[] { test };
        }
    }

    public static final Pattern compile(String glob)
    {
        char[] chars = glob.toCharArray();
        if (chars.length > 0)
        {
            StringBuilder builder = new StringBuilder(chars.length + 5);

            builder.append('^');

            for (char c : chars)
            {
                switch (c)
                {
                    case '*':
                        builder.append(".*");
                        break;
                    case '.':
                        builder.append("\\.");
                        break;
                    case '$':
                        builder.append("\\$");
                        break;
                    default:
                        builder.append(c);
                }
            }

            return Pattern.compile(builder.toString());
        }
        else
        {
            return Pattern.compile(glob);
        }
    }
}
