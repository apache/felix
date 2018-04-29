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
package org.apache.felix.scrplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SCRDescriptorGeneratorTest {

    File folder;
    File dest;

    @Before
    public void setup() throws IOException {
        folder = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
        FileUtils.forceMkdir(folder);

        dest = new File(folder, "testComponents");
        FileUtils.forceMkdir(dest);

    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(folder);
    }

    @Test
    public void testSimpleComponent() throws SCRDescriptorException, SCRDescriptorFailureException, IOException {
        Env env = new Env("SimpleComponent").invoke();
        EasyMock.replay(env.log());
        env.generator().execute();
        EasyMock.verify(env.log());
    }

    @Test
    public void testComponentWithClassReferenceAndMissingInterface() throws SCRDescriptorException, SCRDescriptorFailureException, IOException {
        Env env = new Env("ComponentWithClassReferenceAndMissingInterface").invoke();
        EasyMock.replay(env.log());
        try {
            env.generator().execute();
        } catch ( final SCRDescriptorFailureException e) {
            // this is expected as the interface for a reference is missing
        }
        EasyMock.verify(env.log());
    }

    private void unpackSource(String resource, File dest) throws IOException {
        IOUtils.copy(getClass().getResourceAsStream(resource), new FileOutputStream(dest));
    }

    /**
     * Setups a minimal environment.
     */
    private class Env {
        private String className;
        private Log log;
        private SCRDescriptorGenerator gen;

        public Env(String className) {
            this.className = className;
        }

        public Log log() {
            return log;
        }

        public SCRDescriptorGenerator generator() {
            return gen;
        }

        public Env invoke() throws IOException {
            File aFile = new File(dest, className + ".class");
            unpackSource("/testComponents/" + className + ".class", aFile);

            log = EasyMock.createNiceMock(Log.class);
            gen = new SCRDescriptorGenerator(log);
            Project p = new Project();
            p.setClassLoader(getClass().getClassLoader());
            p.setSources(Collections.<Source>singletonList(new SourceImpl("testComponents." + className, aFile)));

            Options o = new Options();
            o.setOutputDirectory(folder);
            gen.setProject(p);
            gen.setOptions(o);
            return this;
        }
    }
}
