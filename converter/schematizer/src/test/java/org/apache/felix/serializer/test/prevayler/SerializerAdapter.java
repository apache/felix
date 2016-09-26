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

public class SerializerAdapter
    implements MockPrevaylerSerializer
{
    private final DTOSerializer<CommandDTO<?>> delegate;

    public SerializerAdapter( DTOSerializer<CommandDTO<?>> aDelegate )
    {
        delegate = aDelegate;
    }

    @Override
    public Object readObject( InputStream in )
            throws Exception
    {
        return delegate.deserialize( in );
    }

    @Override
    public void writeObject( OutputStream out, Object object )
            throws Exception
    {
        if( !( object instanceof CommandDTO ))
            throw new ClassCastException( "Cannot cast " + object.getClass() + " to CommandDTO" );

        delegate.serialize( out, (CommandDTO<?>)object );
    }
}
