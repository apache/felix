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
package org.apache.felix.bundleplugin;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.libg.generics.Create;

/**
 * Create capabilities for DS components
 * @deprecated bnd generates these as well.
 */
@Deprecated
public class ScrPlugin implements AnalyzerPlugin
{

    Transformer transformer;

    public ScrPlugin() throws Exception
    {
        transformer = getTransformer( getClass().getResource( "scr.xsl" ) );
    }


    public boolean analyzeJar( Analyzer analyzer ) throws Exception
    {
        Set<String> headers = Create.set();

        String bpHeader = analyzer.getProperty( "Service-Component" );

        Map<String, ? extends Map<String, String>> map = Processor.parseHeader( bpHeader, null );
        for ( String root : map.keySet() )
        {
            Resource resource = analyzer.getJar().getResource(root);
            if ( resource != null ) {
                process(analyzer, root, resource, headers);
            }
        }

        // Group and analyze
        for ( String str : headers )
        {
            int idx = str.indexOf( ':' );
            if ( idx < 0 )
            {
                analyzer.warning( ( new StringBuilder( "Error analyzing services in scr resource: " ) ).append( str ).toString() );
                continue;
            }
            String h = str.substring( 0, idx ).trim();
            String v = str.substring( idx + 1 ).trim();

            StringBuilder sb = new StringBuilder();
            String header = analyzer.getProperty( h );
            if (header != null && !header.isEmpty())
            {
                sb.append(header);
                sb.append(",");
            }
            sb.append( v );
            analyzer.setProperty(h, sb.toString());
        }
        return false;
    }


    private void process( Analyzer analyzer, String path, Resource resource, Set<String> headers )
    {
        InputStream in = null;
        try
        {
            in = resource.openInputStream();

            // Retrieve headers
            Set<String> set = analyze( in );
            headers.addAll( set );
        }
        catch ( Exception e )
        {
            analyzer.error( ( new StringBuilder( "Unexpected exception in processing scr resources(" ) )
                .append( path ).append( "): " ).append( e ).toString() );
        }
        finally
        {
            try
            {
                if ( in != null )
                {
                    in.close();
                }
            }
            catch ( IOException e )
            {
            }
        }
    }


    public Set<String> analyze( InputStream in ) throws Exception
    {
        Set<String> refers = new HashSet<String>();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        javax.xml.transform.Result r = new StreamResult( bout );
        javax.xml.transform.Source s = new StreamSource( in );
        transformer.transform( s, r );
        ByteArrayInputStream bin = new ByteArrayInputStream( bout.toByteArray() );
        bout.close();
        BufferedReader br = new BufferedReader( new InputStreamReader( bin ) );
        for ( String line = br.readLine(); line != null; line = br.readLine() )
        {
            line = line.trim();
            if ( line.length() > 0 )
            {
                refers.add( line );
            }
        }

        br.close();
        return refers;
    }


    protected Transformer getTransformer( URL url ) throws Exception
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        javax.xml.transform.Source source = new StreamSource( url.openStream() );
        return tf.newTransformer( source );
    }

}
