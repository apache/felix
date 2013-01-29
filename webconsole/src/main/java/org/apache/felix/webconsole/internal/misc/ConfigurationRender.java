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
package org.apache.felix.webconsole.internal.misc;


import java.io.PrintWriter;

import org.apache.felix.webconsole.WebConsoleUtil;


public class ConfigurationRender
{

    /**
     * Renders an info line - element in the framework configuration. The info line will
     * look like:
     * <pre>
     * label = value
     * </pre>
     *
     * Optionally it can be indented by a specific string.
     *
     * @param pw the writer to print to
     * @param indent indentation string
     * @param label the label data
     * @param value the data itself.
     */
    public static final void infoLine( PrintWriter pw, String indent, String label, Object value )
    {
        if ( indent != null )
        {
            pw.print( indent );
        }

        if ( label != null )
        {
            pw.print( label );
            pw.print( " = " );
        }

        pw.print( WebConsoleUtil.toString( value ) );

        pw.println();
    }
}
