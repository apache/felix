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
package org.apache.felix.webconsole.internal.system;


import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;


/**
 * VMStatPlugin provides the System Information tab. This particular plugin uses
 * more than one templates.
 */
public class VMStatPlugin extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{

    private static final long serialVersionUID = 2293375003997163600L;

    private static final String LABEL = "vmstat"; //$NON-NLS-1$
    private static final String TITLE = "%vmstat.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = null;

    private static final String ATTR_TERMINATED = "terminated"; //$NON-NLS-1$

    private static final String PARAM_SHUTDOWN_TIMER = "shutdown_timer"; //$NON-NLS-1$
    private static final String PARAM_SHUTDOWN_TYPE = "shutdown_type"; //$NON-NLS-1$
    private static final String PARAM_SHUTDOWN_TYPE_RESTART = "Restart"; //$NON-NLS-1$
    //private static final String PARAM_SHUTDOWN_TYPE_STOP = "Stop";

    private static final long startDate = System.currentTimeMillis();

    // from BaseWebConsolePlugin
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    // templates
    private final String TPL_VM_MAIN;
    private final String TPL_VM_STOP;
    private final String TPL_VM_RESTART;


    /** Default constructor */
    public VMStatPlugin()
    {
        super( LABEL, TITLE, CATEGORY_OSGI_MANAGER, CSS );

        // load templates
        TPL_VM_MAIN = readTemplateFile(  "/templates/vmstat.html"  ); //$NON-NLS-1$
        TPL_VM_STOP = readTemplateFile( "/templates/vmstat_stop.html" ); //$NON-NLS-1$
        TPL_VM_RESTART = readTemplateFile( "/templates/vmstat_restart.html" ); //$NON-NLS-1$
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final String action = request.getParameter( "action"); //$NON-NLS-1$

        if ( "setStartLevel".equals( action )) //$NON-NLS-1$
        {
            StartLevel sl = getStartLevel();
            if ( sl != null )
            {
                int bundleSL = WebConsoleUtil.getParameterInt( request, "bundleStartLevel", -1 );
                if ( bundleSL > 0 && bundleSL != sl.getInitialBundleStartLevel() )
                {
                    sl.setInitialBundleStartLevel( bundleSL );
                }

                int systemSL = WebConsoleUtil.getParameterInt( request, "systemStartLevel", -1 );
                if ( systemSL > 0 && systemSL != sl.getStartLevel() )
                {
                    sl.setStartLevel( systemSL );
                }
            }
        }
        else if ( "gc".equals( action ) ) //$NON-NLS-1$
        {
            System.gc();
            System.gc(); // twice for sure
        }
        else if ( request.getParameter( PARAM_SHUTDOWN_TIMER ) == null )
        {

            // whether to stop or restart the framework
            final boolean restart = PARAM_SHUTDOWN_TYPE_RESTART.equals( request.getParameter( PARAM_SHUTDOWN_TYPE ) );

            // simply terminate VM in case of shutdown :-)
            final Bundle systemBundle = getBundleContext().getBundle( 0 );
            Thread t = new Thread( "Stopper" )
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep( 2000L );
                    }
                    catch ( InterruptedException ie )
                    {
                        // ignore
                    }

                    log( "Shutting down server now!" );

                    // stopping bundle 0 (system bundle) stops the framework
                    try
                    {
                        if ( restart )
                        {
                            systemBundle.update();
                        }
                        else
                        {
                            systemBundle.stop();
                        }
                    }
                    catch ( BundleException be )
                    {
                        log( "Problem stopping or restarting the Framework", be );
                    }
                }
            };
            t.start();

            request.setAttribute( ATTR_TERMINATED, ATTR_TERMINATED );
            request.setAttribute( PARAM_SHUTDOWN_TYPE, new Boolean( restart ) );
        }

        // render the response without redirecting
        doGet( request, response );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        String body;

        if ( request.getAttribute( ATTR_TERMINATED ) != null )
        {
            Object restart = request.getAttribute( PARAM_SHUTDOWN_TYPE );
            if ( ( restart instanceof Boolean ) && ( ( Boolean ) restart ).booleanValue() )
            {
                body = TPL_VM_RESTART;
            }
            else
            {
                body = TPL_VM_STOP;
            }
            response.getWriter().print( body );
            return;
        }

        body = TPL_VM_MAIN;

        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024;
        long usedMem = totalMem - freeMem;

        boolean shutdownTimer = request.getParameter( PARAM_SHUTDOWN_TIMER ) != null;
        String shutdownType = request.getParameter( PARAM_SHUTDOWN_TYPE );
        if ( shutdownType == null )
            shutdownType = "";

        DateFormat format = DateFormat.getDateTimeInstance( DateFormat.LONG, DateFormat.LONG, request.getLocale() );
        final String startTime = format.format( new Date( startDate ) );
        final String upTime = formatPeriod( System.currentTimeMillis() - startDate );

        JSONObject json = new JSONObject();
        try
        {
            json.put( "systemStartLevel", getStartLevel().getStartLevel() );
            json.put( "bundleStartLevel", getStartLevel().getInitialBundleStartLevel() );
            json.put( "lastStarted", startTime );
            json.put( "upTime", upTime );
            json.put( "runtime", sysProp( "java.runtime.name" ) + "(build "
                + sysProp( "java.runtime.version" ) + ")" );
            json.put( "jvm", sysProp( "java.vm.name" ) + "(build " + sysProp( "java.vm.version" )
                + ", " + sysProp( "java.vm.info" ) + ")" );
            json.put( "shutdownTimer", shutdownTimer );
            json.put( "mem_total", totalMem );
            json.put( "mem_free", freeMem );
            json.put( "mem_used", usedMem );
            json.put( "shutdownType", shutdownType );

            // only add the processors if the number is available
            final int processors = getAvailableProcessors();
            if ( processors > 0 )
            {
                json.put( "processors", processors );
            }
        }
        catch ( JSONException e )
        {
            throw new IOException( e.toString() );
        }

        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "startData", json.toString() );

        response.getWriter().print( body );
    }

    private static final String sysProp( String name )
    {
        String ret = System.getProperty( name );
        if ( null == ret || ret.length() == 0 ) {
            ret = "n/a"; //$NON-NLS-1$
        }
        return ret;
    }


    private static final String formatPeriod( final long period )
    {
        final Long msecs = new Long( period % 1000 );
        final Long secs = new Long( period / 1000 % 60 );
        final Long mins = new Long( period / 1000 / 60 % 60 );
        final Long hours = new Long( period / 1000 / 60 / 60 % 24 );
        final Long days = new Long( period / 1000 / 60 / 60 / 24 );
        return MessageFormat.format(
            "{0,number} '${vmstat.upTime.format.days}' {1,number,00}:{2,number,00}:{3,number,00}.{4,number,000}",
            new Object[]
                { days, hours, mins, secs, msecs } );
    }


    private final StartLevel getStartLevel()
    {
        return ( StartLevel ) getService( START_LEVEL_NAME );
    }


    /**
     * Returns the number of processor available on Java 1.4 and newer runtimes.
     * If the Runtime.availableProcessors() method is not available, this
     * method returns -1.
     */
    private static final int getAvailableProcessors()
    {
        try
        {
            return Runtime.getRuntime().availableProcessors();
        }
        catch ( Throwable t )
        {
            // NoSuchMethodError on pre-1.4 runtimes
        }

        return -1;
    }
}
