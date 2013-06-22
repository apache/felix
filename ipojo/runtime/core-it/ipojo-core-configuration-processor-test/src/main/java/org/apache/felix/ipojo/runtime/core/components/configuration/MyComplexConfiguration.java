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

package org.apache.felix.ipojo.runtime.core.components.configuration;

import org.apache.felix.ipojo.configuration.Configuration;
import org.apache.felix.ipojo.configuration.Instance;
import org.apache.felix.ipojo.runtime.core.components.Bean;
import org.apache.felix.ipojo.runtime.core.components.MyComplexComponent;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import static org.apache.felix.ipojo.configuration.Instance.*;

/**
 * A complex configuration creating two instances of MyComplex component
 */
@Configuration
public class MyComplexConfiguration {

    Instance complex1(BundleContext bc) throws FileNotFoundException {

        File file = bc.getBundle().getDataFile("file1.txt");
        write(file, "I'm file 1");

        Bean bean = new Bean();
        bean.setMessage("I'm 1");
        bean.setCount(1);

        return instance().of(MyComplexComponent.class)
                .with("file").setto(file)
                .with("bean").setto(bean)
                .with("map").setto(map(pair("a", "b"), pair("c", "d")));

    }

    Instance complex2(BundleContext bc) throws FileNotFoundException {

        File file = bc.getBundle().getDataFile("file2.txt");
        write(file, "I'm file 2");

        Bean bean = new Bean();
        bean.setMessage("I'm 2");
        bean.setCount(2);

        return instance().of(MyComplexComponent.class)
                .with("file").setto(file)
                .with("bean").setto(bean)
                .with("map").setto(map(pair("a", "b2"), pair("c", "d2")));

    }

    private void write(File file, String message) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(file);
        out.println(message);
        out.close();
    }
}
