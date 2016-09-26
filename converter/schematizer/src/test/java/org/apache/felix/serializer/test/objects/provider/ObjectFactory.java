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
package org.apache.felix.serializer.test.objects.provider;

import java.util.Collection;

import org.osgi.service.converter.Converter;
import org.osgi.service.converter.StandardConverter;

public class ObjectFactory
{
    private final Converter cnv = new StandardConverter();

    public ComplexTopEntity newComplexTop( String anId, String aValue, ComplexMiddleEntity aMiddle )
    {
        final ComplexTopEntity top = new ComplexTopEntity();
        top.id = anId;
        top.value = aValue;
        top.embeddedValue = cnv.convert( aMiddle ).to( ComplexMiddleEntity.class );
        return top;
    }

    public SimpleTopEntity newSimpleTop( String anId, String aValue, SimpleMiddleEntity aMiddle )
    {
        final SimpleTopEntity top = new SimpleTopEntity();
        top.id = anId;
        top.value = aValue;
        top.embeddedValue = cnv.convert( aMiddle ).to( SimpleMiddleEntity.class );
        return top;
    }

    public ComplexMiddleEntity newComplexMiddle( String anId, String aValue, Collection<BottomEntity> bums )
    {
        final ComplexMiddleEntity middle = new ComplexMiddleEntity();
        middle.id = anId;
        middle.someValue = aValue;
        bums.stream().forEach( b -> middle.embeddedValue.add( b ) );
        return middle;
    }

    public SimpleMiddleEntity newSimpleMiddle( BottomEntity aBum )
    {
        final SimpleMiddleEntity middle = new SimpleMiddleEntity();
        middle.embeddedValue = null;
        return middle;
    }

    public BottomEntity newBottom( String anId, String aBum )
    {
        final BottomEntity bum = new BottomEntity();
        bum.id = anId;
        bum.bum = aBum;
        return bum;
    }
}

