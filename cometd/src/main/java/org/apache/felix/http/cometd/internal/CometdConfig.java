/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.cometd.internal;

import org.osgi.framework.BundleContext;
import java.util.Dictionary;
import java.util.Properties;

public final class CometdConfig
{
    /** cometd servlet path */
    private static final String COMETD_PATH = "org.apache.felix.http.cometd.path";

    /** default cometd servlet path */
    private static final String DEFAULT_COMETD_PATH = "/system/cometd";

    private final BundleContext context;
    private String path;

    public CometdConfig(BundleContext context)
    {
        this.context = context;
        reset();
    }

    public String getPath()
    {
        return this.path;
    }

    public void reset()
    {
        update(null);
    }

    public void update(Dictionary props)
    {
        if (props == null) {
            props = new Properties();
        }

        this.path = getProperty(props, COMETD_PATH, DEFAULT_COMETD_PATH);
    }

    private String getProperty(Dictionary props, String name, String defValue)
    {
        Object value = props.get(name);
        if (value == null)
        {
            value = this.context.getProperty(name);
        }

        return value != null ? String.valueOf(value) : defValue;
    }
}
