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
package org.apache.felix.hc.core.it;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import javax.servlet.Servlet;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

class MockHttpService implements HttpService {

    private List<String> paths = new ArrayList<String>();

    private List<String> classNames = new ArrayList<String>();

    @Override
    public void registerResources(String alias, String name, HttpContext context) {
    }

    @Override
    public void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext context) {
        paths.add(alias);
        classNames.add(servlet.getClass().getName());
    }

    public void unregister(String alias) {
        paths.remove(alias);
    }

    @Override
    public HttpContext createDefaultHttpContext() {
        return null;
    }

    List<String> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    List<String> getServletClassNames() {
        return Collections.unmodifiableList(classNames);
    }
}
