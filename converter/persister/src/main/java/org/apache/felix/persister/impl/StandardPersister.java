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
package org.apache.felix.persister.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.felix.persister.Persister;
import org.apache.felix.schematizer.Schematizer;
import org.apache.felix.schematizer.StandardSchematizer;
import org.apache.felix.serializer.Serializer;
import org.osgi.util.converter.Converter;

public class StandardPersister<D> implements Persister<D> {

    private final Class<D> dataType;
    private final Serializer serializer;
    private Converter converter;

    public StandardPersister(Class<D> aDataType, Serializer aSerializer) {
        dataType = aDataType;
        serializer = aSerializer;
    }

    @Override
    public void serialize(OutputStream out, D entity) {
        try {
            serializer.serialize(entity).convertWith(getConverter()).to( out );
        } catch ( IOException e ) {
            // TODO: Handle this
            e.printStackTrace();
        }
    }

    @Override
    public D deserialize(InputStream in) {
      return (D)serializer
              .deserialize(dataType)
              .convertWith(getConverter())
              .from(in);
    }

    private Converter getConverter() {
        if (converter == null) {
            String name = dataType.getName();
            Schematizer s = new StandardSchematizer();
            s.schematize(name, dataType).get(name);
            converter = s.converterFor(name);
        }

        return converter;
    }
}
