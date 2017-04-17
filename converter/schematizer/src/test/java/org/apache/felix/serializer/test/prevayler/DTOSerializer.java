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
package org.apache.felix.serializer.test.prevayler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.felix.schematizer.Schematizer;
import org.apache.felix.schematizer.impl.SchematizerImpl;
import org.apache.felix.serializer.impl.json.JsonSerializerImpl;
import org.osgi.util.converter.Converter;

public class DTOSerializer<C extends CommandDTO<?>>
{
    private static final int MARKER_LENGTH = 10;

    private final Schematizer schematizer = new SchematizerImpl();
    private final JsonSerializerImpl serializer = new JsonSerializerImpl();
    private final Class<?> entityType;

    public DTOSerializer(Class<?> anEntityType)
    {
        entityType = anEntityType;
    }

    @SuppressWarnings( "unchecked" )
    public C deserialize( InputStream in )
            throws Exception
    {
        Command command = parseCommandFrom( in );
        Converter c = schematizer.converterFor( command.name() );
        return (C)serializer
                .deserialize( CommandDTO.class )
                .with( c )
                .from( in );
    }

    public void serialize( OutputStream out, C command )
            throws Exception
    {
        // Lazy load the schemas as we collect new types to serialize
        String name = command.command.name();
        schematizer.schematize( name, entityType ).get( name );
        out.write( markerFor( command.command ) );
        Converter c = schematizer.converterFor(name);
        serializer.serialize( command ).with( c ).to( out );
    }

    private final byte[] markerFor( Command command )
    {
        return pad( command.name() ).getBytes( StandardCharsets.UTF_8 );
    }

    private String pad( String value )
    {
        StringBuilder s = new StringBuilder();
        s.append( value );
        for( int i = 0; i < MARKER_LENGTH - value.length(); i++ )
            s.append( ":" );
        return s.toString();
    }

    private Command parseCommandFrom( InputStream in )
        throws Exception
    {
        byte[] buffer = new byte[MARKER_LENGTH];
        in.read( buffer, 0, MARKER_LENGTH );
        String name = new String( buffer );
        name = name.replaceAll( ":", "" );
        Command command = Command.valueOf( name );
        return command;
    }
}
