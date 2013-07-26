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

package org.apache.felix.ipojo.runtime.core.handlers;

import java.util.Dictionary;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;

/**
 * User: guillaume
 * Date: 24/07/13
 * Time: 12:08
 */
@Handler(namespace = "com.acme", name = "foo")
public class FooHandler extends PrimitiveHandler {

    @Override
    public void configure(final Element metadata, final Dictionary configuration) throws ConfigurationException {
        Element[] elements = metadata.getElements("foo", "com.acme");
        for (Element foo : elements) {
            String value = foo.getAttribute("value");
            String field = foo.getAttribute("field");

            this.getInstanceManager().register(getPojoMetadata().getField(field, "java.lang.String"),
                                               new FixedValueFieldInterceptor(value));

        }

    }

    @Override
    public Object onGet(final Object pojo, final String fieldName, final Object value) {
        return value;
    }

    @Override
    public void stop() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void start() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private static class FixedValueFieldInterceptor implements FieldInterceptor {
        private final String m_value;

        public FixedValueFieldInterceptor(final String value) {
            m_value = value;
        }

        @Override
        public void onSet(final Object pojo, final String fieldName, final Object value) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object onGet(final Object pojo, final String fieldName, final Object value) {
            return m_value;
        }
    }
}
