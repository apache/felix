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
package org.apache.felix.threaddump.internal;

import java.io.PrintWriter;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.threaddump.internal.jdk5.Jdk15ThreadDumper;
import org.apache.felix.threaddump.internal.jdk6.Jdk16ThreadDumper;

/**
 * A composite {@link ThreadDumper} implementation that:
 * <ul>
 * <li>uses Java 6 JMX API if available;
 * <li>
 * <li>falls back to Java 5 JMX API if not Java 6;</li>
 * <li>falls back to regular Java API as a last step.</li>
 * </ul>
 */
final class ThreadDumpInventoryPrinter implements InventoryPrinter
{

    /**
     * The <code>java.specification.version</code> string constant.
     */
    private static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";

    /**
     * The <code>1.6</code> string constant.
     */
    private static final String JDK16_SPECIFICATION_VERSION = "1.6";

    /**
     * The <code>1.5</code> string constant.
     */
    private static final String JDK15_SPECIFICATION_VERSION = "1.5";

    /**
     * {@inheritDoc}
     */
    public void print(PrintWriter printWriter, Format format, boolean isZip)
    {
        ThreadDumper delegated;

        final String javaSpecificationVersion = System.getProperty(JAVA_SPECIFICATION_VERSION);

        // JDK 1.6, 1.7 and 1.8 have same APIs
        if (JDK16_SPECIFICATION_VERSION.compareToIgnoreCase(javaSpecificationVersion) <= 0)
        {
            delegated = new Jdk16ThreadDumper();
        }
        else if (JDK15_SPECIFICATION_VERSION.equalsIgnoreCase(javaSpecificationVersion))
        {
            delegated = new Jdk15ThreadDumper();
        }
        else
        {
            // Falls back to regular Java API as a last step
            delegated = new Jdk14ThreadDumper();
        }

        ThreadWriter threadWriter = new ThreadWriter(printWriter);

        threadWriter.printHeader();
        delegated.printThreads(threadWriter);
    }

}
