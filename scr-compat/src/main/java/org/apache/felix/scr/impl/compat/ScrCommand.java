/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.compat;

import java.io.PrintWriter;

import org.apache.felix.scr.ScrInfo;

public class ScrCommand implements ScrInfo
{

    private final org.apache.felix.scr.info.ScrInfo scrInfo;

    public ScrCommand(final org.apache.felix.scr.info.ScrInfo scrInfo)
    {
        this.scrInfo = scrInfo;
     }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#list(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void list(final String bundleIdentifier, final PrintWriter out)
    {
        this.scrInfo.list(bundleIdentifier, out);
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#info(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void info(final String componentId, final PrintWriter out)
    {
        this.scrInfo.info(componentId, out);
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.ScrInfo#config(java.io.PrintStream)
     */
    public void config(final PrintWriter out)
    {
        this.scrInfo.config(out);
    }
}
