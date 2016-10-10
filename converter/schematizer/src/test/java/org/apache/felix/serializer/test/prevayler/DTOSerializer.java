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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.felix.schematizer.Schema;
import org.apache.felix.schematizer.Schematizer;
import org.apache.felix.schematizer.TypeRule;
import org.apache.felix.schematizer.impl.SchematizerImpl;
import org.apache.felix.serializer.impl.json.JsonSerializerImpl;
import org.osgi.converter.Converter;
import org.osgi.converter.StandardConverter;
import org.osgi.converter.TypeReference;

public class DTOSerializer<C extends CommandDTO<?>>
{
    private static final int MARKER_LENGTH = 10;

    private final Converter converter = new StandardConverter();
    private final JsonSerializerImpl serializer = new JsonSerializerImpl();
    private final List<TypeRule<?>> rules;
    private final Map<String, Schema> schemas = new HashMap<>();
    private final Class<?> entityType;

    public DTOSerializer(
            List<TypeRule<?>> aRulesList,
            Class<?> anEntityType )
    {
        rules = new ArrayList<>();
        rules.addAll( aRulesList );
        entityType = anEntityType;
    }

    @SuppressWarnings( "unchecked" )
    public C deserialize( InputStream in )
            throws Exception
    {
        Command command = parseCommandFrom( in );
        Schema s = schemas.get( command.name() );
        return (C)serializer
                .deserialize( CommandDTO.class )
                .with( converter )
                .withContext( s )
                .from( in );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void serialize( OutputStream out, C command )
            throws Exception
    {
        // Lazy load the schemas as we collect new types to serialize
        String name = command.command.name();
        if( !schemas.containsKey( name ) )
        {
            Schematizer s = new SchematizerImpl()
                    .rule( name, new TypeReference(){
                        @Override
                        public Type getType()
                        {
                            return new AggregateTypeReference( null, command.getClass(), entityType ).getType();
                        }
                    } );
            rules.stream()
                .forEach( r -> s.rule( name, (TypeRule)r ) );
            Optional<Schema> opt = s.get( name );

            // TODO: What do we do if there is no schema?? Just continue anyway?
            if( opt.isPresent() )
            {
                Schema schema = opt.get();
                schemas.put( name, schema );
            }
        }

        out.write( markerFor( command.command ) );
        serializer.serialize( command ).to( out );
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
