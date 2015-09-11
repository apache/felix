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
package org.apache.felix.webconsole.plugins.event.internal;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Date;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * The Event Plugin
 */
public class PluginServlet extends HttpServlet
{
    
    private static final String ACTION_POST = "post"; //$NON-NLS-1$
    private static final String ACTION_SEND = "send"; //$NON-NLS-1$
    private static final String ACTION_CLEAR = "clear"; //$NON-NLS-1$

    private static final String PARAMETER_ACTION = "action"; //$NON-NLS-1$

    /** The event collector. */
    private final EventCollector collector;

    /** Is the config admin available? */
    private volatile boolean configAdminAvailable = false;
    
    private EventAdmin eventAdmin;

    private final String TEMPLATE;

    public PluginServlet()
    {
        this.collector = new EventCollector(null);
        TEMPLATE = readTemplateFile(getClass(), "/res/events.html"); //$NON-NLS-1$
    }
    
    private final String readTemplateFile(final Class clazz, final String templateFile)
    {
        InputStream templateStream = getClass().getResourceAsStream(templateFile);
        if (templateStream != null)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            try
            {
                int len = 0;
                while ((len = templateStream.read(data)) > 0)
                {
                    baos.write(data, 0, len);
                }
                return baos.toString("UTF-8"); //$NON-NLS-1$
            }
            catch (IOException e)
            {
                // don't use new Exception(message, cause) because cause is 1.4+
                throw new RuntimeException("readTemplateFile: Error loading "
                    + templateFile + ": " + e);
            }
            finally
            {
                try
                {
                    templateStream.close();
                }
                catch (IOException e)
                {
                    /* ignore */
                }

            }
        }

        // template file does not exist, return an empty string
        log("readTemplateFile: File '" + templateFile + "' not found through class "
            + clazz);
        return ""; //$NON-NLS-1$
    }
    
    private static final Event newEvent(HttpServletRequest request)
    {
        String topic = request.getParameter("topic"); //$NON-NLS-1$

        return new Event(topic, (Dictionary)PropertiesEditorSupport.convertProperties(request));
    }
    
   
    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
    throws ServletException, IOException
    {
        final String action = req.getParameter( PARAMETER_ACTION );
        if ( ACTION_POST.equals(action) ) {
            final Event event = newEvent(req);
            eventAdmin.postEvent(event);
        } else if (ACTION_SEND.equals(action)) {
            final Event event = newEvent(req);
            eventAdmin.sendEvent(event);
        } else if ( ACTION_CLEAR.equals( action ) ) {
            this.collector.clear();
        }
        // we always send back the json data
        resp.setContentType( "application/json" ); //$NON-NLS-1$
        resp.setCharacterEncoding( "utf-8" ); //$NON-NLS-1$

        renderJSON( resp.getWriter() );
    }

    private void renderJSON( final PrintWriter pw ) throws IOException
    {
        List events = this.collector.getEvents();

        StringBuffer statusLine = new StringBuffer();
        statusLine.append( events.size() );
        statusLine.append( " Event");
        if ( events.size() != 1 )
        {
            statusLine.append('s');
        }
        statusLine.append( " received" );
        if ( !events.isEmpty() )
        {
            statusLine.append( " since " );
            Date d = new Date();
            d.setTime( ( ( EventInfo ) events.get( 0 ) ).received );
            statusLine.append( d );
        }
        statusLine.append( ". (Event admin: " );
        if ( this.eventAdmin == null )
        {
            statusLine.append("un");
        }
        statusLine.append("available; Config admin: ");
        if ( !this.configAdminAvailable )
        {
            statusLine.append("un");
        }
        statusLine.append("available)");

        // Compute scale: startTime is 0, lastTimestamp is 100%
        final long startTime = this.collector.getStartTime();
        final long endTime = (events.size() == 0 ? startTime : ((EventInfo)events.get(events.size() - 1)).received);
        final float scale = (endTime == startTime ? 100.0f : 100.0f / (endTime - startTime));

        pw.write("{");

        jsonKey( pw, "status" );
        jsonValue( pw, statusLine.toString() );
        pw.write(',');
        jsonKey( pw, "data" );

        pw.write('[');

        // display list in reverse order
        for ( int index = events.size() - 1; index >= 0; index-- )
        {
            eventJson( pw, ( EventInfo ) events.get( index ), index, startTime, scale );
            if ( index > 0 )
            {
                pw.write(',');
            }
        }

        pw.write(']');

        pw.write("}"); //$NON-NLS-1$
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {

        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) ) //$NON-NLS-1$
        {
            response.setContentType( "application/json" ); //$NON-NLS-1$
            response.setCharacterEncoding( "UTF-8" ); //$NON-NLS-1$

            PrintWriter pw = response.getWriter();
            this.renderJSON( pw );

            // nothing more to do
            return;
        }

        this.renderContent( request, response );
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        final PrintWriter pw = response.getWriter();
        //final String appRoot = ( String ) request.getAttribute( "felix.webconsole.appRoot" );
        //pw.println( "<script src='" + appRoot + "/events/res/ui/" + "events.js" + "' type='text/javascript'></script>" );
        pw.print(TEMPLATE);
    }

    public URL getResource(String path)
    {
        if ( path.startsWith("/events/res/ui/") ) //$NON-NLS-1$
        {
            return this.getClass().getResource(path.substring(7));
        }
        return null;
    }

    private void jsonValue( final PrintWriter pw, final String v)
    throws IOException
    {
        if (v == null || v.length() == 0)
        {
            pw.write("\"\"");
            return;
        }

        pw.write('"');
        char previousChar = 0;
        char c;

        for (int i = 0; i < v.length(); i += 1)
        {
            c = v.charAt(i);
            switch (c)
            {
                case '\\':
                case '"':
                    pw.write('\\');
                    pw.write(c);
                    break;
                case '/':
                    if (previousChar == '<')
                    {
                        pw.write('\\');
                    }
                    pw.write(c);
                    break;
                case '\b':
                    pw.write("\\b");
                    break;
                case '\t':
                    pw.write("\\t");
                    break;
                case '\n':
                    pw.write("\\n");
                    break;
                case '\f':
                    pw.write("\\f");
                    break;
                case '\r':
                    pw.write("\\r");
                    break;
                default:
                    if (c < ' ')
                    {
                        final String hexValue = "000" + Integer.toHexString(c);
                        pw.write("\\u");
                        pw.write(hexValue.substring(hexValue.length() - 4));
                    }
                    else
                    {
                        pw.write(c);
                    }
            }
            previousChar = c;
        }
        pw.write('"');
    }

    private void jsonValue( final PrintWriter pw, final long l)
    {
        pw.write(Long.toString(l));
    }

    private void jsonKey( final PrintWriter pw, String key)
    throws IOException
    {
        jsonValue( pw, key);
        pw.write(':');
    }

    private void eventJson( PrintWriter jw, EventInfo info, int index, final long start, final float scale )
    throws IOException
    {
        final long msec = info.received - start;

        // Compute color bar size and make sure the bar is visible
        final int percent = Math.max((int)(msec * scale), 2);

        jw.write("{");
        jsonKey(jw, "id" );
        jsonValue(jw, String.valueOf( index ) );
        jw.write(',');
        jsonKey(jw, "offset" );
        jsonValue(jw, msec );
        jw.write(',');
        jsonKey(jw, "width" );
        jsonValue(jw, percent );
        jw.write(',');
        jsonKey(jw, "category" );
        jsonValue(jw, info.category );
        jw.write(',');
        jsonKey(jw, "received" );
        jsonValue(jw, info.received );
        jw.write(',');
        jsonKey(jw, "topic" );
        jsonValue(jw, info.topic );
        if ( info.info != null )
        {
            jw.write(',');
            jsonKey(jw, "info" );
            jsonValue(jw, info.info );
        }
        jw.write(',');
        jsonKey(jw, "properties" );
        jw.write("{");
        if ( info.properties != null && info.properties.size() > 0 )
        {
            final Iterator i = info.properties.entrySet().iterator();
            boolean first = true;
            while ( i.hasNext() )
            {
                final Map.Entry current = (Entry) i.next();
                if ( !first)
                {
                    jw.write(',');
                }
                first = false;
                jsonKey(jw, current.getKey().toString() );
                final Object value = current.getValue();
                if ( null == value )
                {
                  jw.write( "null" ); //$NON-NLS-1$
                }
                else if ( value.getClass().isArray() )
                {
                    // as we can't use 1.5 functionality we have to print the array ourselves
                    final StringBuffer b = new StringBuffer("[");
                    final int arrayLength = Array.getLength(value);
                    for(int m=0; m<arrayLength; m++) {
                        if ( m > 0 )
                        {
                            b.append(", ");
                        }
                        b.append( String.valueOf( Array.get(value, m) ) );
                    }
                    b.append(']');
                    jsonValue(jw, b.toString());
                }
                else
                {
                    jsonValue(jw, value.toString());
                }
            }
        }
        jw.write("}");

        jw.write("}");
    }

    public void updateConfiguration( Dictionary dict)
    {
        this.collector.updateConfiguration(dict);
    }

    public EventCollector getCollector()
    {
        return this.collector;
    }

    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        this.eventAdmin = eventAdmin;
    }

    public void setConfigAdminAvailable(final boolean flag)
    {
        this.configAdminAvailable = flag;
    }
}
