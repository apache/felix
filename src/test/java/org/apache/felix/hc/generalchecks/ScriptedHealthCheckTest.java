/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.generalchecks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.generalchecks.util.ScriptEnginesTracker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ScriptedHealthCheckTest {

    private static final String GROOVY = "Groovy";

    
    @Spy
    @InjectMocks
    ScriptedHealthCheck scriptedHealthCheck;

    @Spy
    private ScriptEnginesTracker scriptEnginesTracker;

    @Mock
    private BundleContext bundleContext;
    
    @Mock
    private ServiceReference<TestService> testServiceReference;
    
    @Mock
    private TestService testService;
    
    @Before
    public void setup() {
        initMocks(this);
        ScriptEngineManager factory = new ScriptEngineManager();
        // create JavaScript engine
        ScriptEngine groovyEngine = factory.getEngineByName(GROOVY);
        doReturn(groovyEngine).when(scriptEnginesTracker).getEngineByLanguage(GROOVY.toLowerCase());
    }

    
    
    @Test
    public void testSimpleStatusValues() {
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "log.info('good')", ""));
        assertEquals(Result.Status.OK, scriptedHealthCheck.execute().getStatus()); 

        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "log.warn('not so good')", ""));
        assertEquals(Result.Status.WARN, scriptedHealthCheck.execute().getStatus()); 

        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "log.critical('bad')", ""));
        assertEquals(Result.Status.CRITICAL, scriptedHealthCheck.execute().getStatus()); 

        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "log.temporarilyUnavailable('tmp away')", ""));
        assertEquals(Result.Status.TEMPORARILY_UNAVAILABLE, scriptedHealthCheck.execute().getStatus()); 

    }
    
    @Test
    public void testExceptionInScript() {
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "throw new IllegalStateException()", ""));
        assertEquals(Result.Status.HEALTH_CHECK_ERROR, scriptedHealthCheck.execute().getStatus()); 
    }
    
    @Test
    public void testWithScriptUrl() {
        URL scriptUrl = getClass().getResource("testHcScript.groovy");
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "", scriptUrl.toString()));
        Result result = scriptedHealthCheck.execute();
        assertEquals(Result.Status.OK, result.getStatus()); 
        
        List<Entry> entries = getEntries(result);
        assertEquals(3, entries.size()); 
        assertEquals("Test Script URL", entries.get(entries.size()-1).getMessage()); 
    }

    @Test
    public void testStdOut() {
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, "print 'test'", ""));
        Result result = scriptedHealthCheck.execute();
         
        List<Entry> entries = getEntries(result);
        assertEquals(3, entries.size()); 
        assertEquals("stdout of script: test", entries.get(entries.size()-1).getMessage()); 
    }

    @Test
    public void testCombinedResult() {
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, 
                "import org.apache.felix.hc.api.*\n" +
                "log.info('test1')\n" + 
                "return new Result(Result.Status.WARN, 'warn')",""));
        Result result = scriptedHealthCheck.execute();
        assertEquals(Result.Status.WARN, result.getStatus()); 

        List<Entry> entries = getEntries(result);
        assertEquals(4, entries.size()); 
        assertEquals("test1", entries.get(entries.size()-2).getMessage()); 
        assertEquals("warn", entries.get(entries.size()-1).getMessage()); 
    }
    
    @Test
    public void testArbitraryResultLogged() {
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, 
                "return 'ARBITRARY_RESULT_OBJECT'",""));
        Result result = scriptedHealthCheck.execute();
        assertEquals(Result.Status.OK, result.getStatus()); 

        List<Entry> entries = getEntries(result);
        assertEquals(3, entries.size()); 
        assertEquals("Script result: ARBITRARY_RESULT_OBJECT", entries.get(entries.size()-1).getMessage()); 
    }
    
    @Test
    public void testOsgiBinding() {

        TestService testService = new TestService();
        doReturn(testServiceReference).when(bundleContext).getServiceReference(TestService.class.getName());
        doReturn(testService).when(bundleContext).getService(testServiceReference);
        
        scriptedHealthCheck.activate(bundleContext, new TestConfig(GROOVY, 
                "return osgi.getService(org.apache.felix.hc.generalchecks.ScriptedHealthCheckTest.TestService.class)",""));
        
        Result result = scriptedHealthCheck.execute();
        assertEquals(Result.Status.OK, result.getStatus()); 
        assertEquals("Script result: TestService", getEntries(result).get(2).getMessage()); 

        verify(bundleContext, times(1)).getServiceReference(TestService.class.getName());
        verify(bundleContext, times(1)).getService(testServiceReference);
        verify(bundleContext, times(1)).ungetService(testServiceReference);
    }
    

    private List<Entry> getEntries(Result execute) {
        return StreamSupport.stream(execute.spliterator(), false).collect(Collectors.toList());
    }

    public class TestService {

        @Override
        public String toString() {
            return "TestService";
        }
        
    }
    
    private final class TestConfig implements ScriptedHealthCheck.Config {
        
        private final String language;
        private final String script;
        private final String scriptUrl;
        
        public TestConfig(String language, String script, String scriptUrl) {
            super();
            this.language = language;
            this.script = script;
            this.scriptUrl = scriptUrl;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ScriptedHealthCheck.Config.class;
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String scriptUrl() {
            return scriptUrl;
        }

        @Override
        public String script() {
            return script;
        }

        @Override
        public String language() {
            return language;
        }

        @Override
        public String[] hc_tags() {
            return new String[] {"test"};
        }

        @Override
        public String hc_name() {
            return "Test HC";
        }
    }

}
