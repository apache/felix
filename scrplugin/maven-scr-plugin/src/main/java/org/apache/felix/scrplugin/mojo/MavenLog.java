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
package org.apache.felix.scrplugin.mojo;

import java.io.File;

import org.apache.felix.scrplugin.Log;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * The <code>MavenLog</code> class implements the {@link Log} interface using
 * the Maven logger created on instantiation.
 */
public class MavenLog implements Log {

	private static final int COLUMN_NUMBER_UNKNOWN = 0;
	
    private final org.apache.maven.plugin.logging.Log mavenLog;
    private final BuildContext buildContext;

    MavenLog(final org.apache.maven.plugin.logging.Log mavenLog, BuildContext buildContext) {
        this.mavenLog = mavenLog;
        this.buildContext = buildContext;
    }

    /**
     * @see org.apache.felix.scrplugin.Log#debug(java.lang.String,
     *      java.lang.Throwable)
     */
    public void debug(final String content, final Throwable error) {
        mavenLog.debug(content, error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#debug(java.lang.String)
     */
    public void debug(final String content) {
        mavenLog.debug(content);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#debug(java.lang.Throwable)
     */
    public void debug(final Throwable error) {
        mavenLog.debug(error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#error(java.lang.String, java.lang.Throwable)
     */
    public void error(final String content, final Throwable error) {
        mavenLog.error(content, error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#error(java.lang.String, java.lang.String, int)
     */
    public void error(final String content,
            final String location,
            final int lineNumber) {
    	error(content, location, lineNumber, COLUMN_NUMBER_UNKNOWN);
    }
    
    /**
     * @see org.apache.felix.scrplugin.Log#error(java.lang.String, java.lang.String, int, int)
     */
    public void error(final String content, final String location,
    		final int lineNumber, final int columnNumber) {
    	buildContext.addMessage(new File(location), lineNumber, columnNumber, 
    			content, BuildContext.SEVERITY_ERROR, null);
    }    

    /**
     * @see org.apache.felix.scrplugin.Log#error(java.lang.String)
     */
    public void error(final String content) {
        mavenLog.error(content);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#error(java.lang.Throwable)
     */
    public void error(final Throwable error) {
        mavenLog.error(error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#info(java.lang.String, java.lang.Throwable)
     */
    public void info(final String content, final Throwable error) {
        mavenLog.info(content, error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#info(java.lang.String)
     */
    public void info(final String content) {
        mavenLog.info(content);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#info(java.lang.Throwable)
     */
    public void info(final Throwable error) {
        mavenLog.info(error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#isDebugEnabled()
     */
    public boolean isDebugEnabled() {
        return mavenLog.isDebugEnabled();
    }

    /**
     * @see org.apache.felix.scrplugin.Log#isErrorEnabled()
     */
    public boolean isErrorEnabled() {
        return mavenLog.isErrorEnabled();
    }

    /**
     * @see org.apache.felix.scrplugin.Log#isInfoEnabled()
     */
    public boolean isInfoEnabled() {
        return mavenLog.isInfoEnabled();
    }

    /**
     * @see org.apache.felix.scrplugin.Log#isWarnEnabled()
     */
    public boolean isWarnEnabled() {
        return mavenLog.isWarnEnabled();
    }

    /**
     * @see org.apache.felix.scrplugin.Log#warn(java.lang.String, java.lang.Throwable)
     */
    public void warn(final String content, final Throwable error) {
        mavenLog.warn(content, error);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#warn(java.lang.String, java.lang.String, int)
     */
    public void warn(final String content, final String location,
            final int lineNumber) {
        this.warn(content, location, lineNumber, COLUMN_NUMBER_UNKNOWN);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#warn(java.lang.String, java.lang.String, int, int)
     */
    public void warn(final String content, final String location,
    		final int lineNumber, final int columnNumber) {
    	buildContext.addMessage(new File(location), lineNumber, columnNumber, 
    			content, BuildContext.SEVERITY_WARNING, null);
    }
    
    
    /**
     * @see org.apache.felix.scrplugin.Log#warn(java.lang.String)
     */
    public void warn(final String content) {
        mavenLog.warn(content);
    }

    /**
     * @see org.apache.felix.scrplugin.Log#warn(java.lang.Throwable)
     */
    public void warn(final Throwable error) {
        mavenLog.warn(error);
    }
}
