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
package org.apache.felix.logback.test.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import org.junit.BeforeClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import junit.framework.AssertionFailedError;

public class LogTestHelper {

    protected static FileAppender<ILoggingEvent> fileAppender;

    @BeforeClass
    public static void before() throws Exception {
        LoggerContext context = (LoggerContext)org.slf4j.LoggerFactory.getILoggerFactory();

        for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext();) {
                Appender<ILoggingEvent> appender = index.next();

                if (appender instanceof FileAppender) {
                    fileAppender = (FileAppender<ILoggingEvent>)appender;
                }
            }
        }
    }

    protected void assertLog(String level, String name, long time) {
        assertLog(level + "|" + name + "|" + time);
    }

    protected void assertLog(String record) {
        boolean found = false;

        ResilientFileOutputStream rfos = (ResilientFileOutputStream)fileAppender.getOutputStream();
        rfos.flush();
        try {
            rfos.getChannel().force(true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        File logFile = rfos.getFile();

        try (Scanner sc = new Scanner(logFile)) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();

                if (nextLine.equals(record)) {
                    found = true;
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (!found) {
            throw new AssertionFailedError("Log record not found: " + record);
        }
    }

    protected static org.osgi.service.log.Logger getLogger(Class<?> clazz) {
        BundleContext bundleContext = FrameworkUtil.getBundle(clazz).getBundleContext();
        ServiceReference<org.osgi.service.log.LoggerFactory> serviceReference =
            bundleContext.getServiceReference(org.osgi.service.log.LoggerFactory.class);
        org.osgi.service.log.LoggerFactory loggerFactory = bundleContext.getService(serviceReference);
        return loggerFactory.getLogger(clazz);
    }

}
