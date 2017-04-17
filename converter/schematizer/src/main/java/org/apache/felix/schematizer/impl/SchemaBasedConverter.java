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
package org.apache.felix.schematizer.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.felix.schematizer.Node;
import org.apache.felix.schematizer.Schema;
import org.osgi.dto.DTO;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterFunction;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TargetRule;
import org.osgi.util.converter.TypeReference;

import static org.apache.felix.schematizer.impl.Util.*;

public class SchemaBasedConverter<T> implements TargetRule<T> {
    private final SchemaImpl schema;
    private final Converter converter;

    public SchemaBasedConverter(SchemaImpl aSchema) {
        schema = aSchema;
        converter = new StandardConverter();
    }

    @Override
    public ConverterFunction<T> getFunction() {
        return (obj,t) -> {
            if (!(obj instanceof Map) || schema == null)
                return handleInvalid();
            return convertMap((Map<?,?>)obj, schema, "/");
        };
    }

    @Override
    public Type getTargetType() {
        return schema.rootNode().type();
    }

    @SuppressWarnings( "unchecked" )
    private T convertMap(Map<?,?> map, Schema s, String contextPath) {
        Node node = s.nodeAtPath(contextPath);
        Class<?> cls = Util.rawClassOf(node.type());

        if (!asDTO(cls))
            return handleInvalid();

        if (!contextPath.endsWith("/"))
            contextPath = contextPath + "/";

        return (T)convertToDTO((Class<? extends DTO>)cls, map, s, contextPath);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <U extends DTO>U convertToDTO(Class<U> targetCls, Map<?,?> m, Schema schema, String contextPath) {
        try {
            U dto = targetCls.newInstance();

            for (Map.Entry entry : m.entrySet()) {
                try {
                    Field f = targetCls.getField(entry.getKey().toString());
                    Object val = entry.getValue();
                    if (val == null)
                        continue;
                    String path = contextPath + f.getName();
                    Node node = schema.nodeAtPath(path);
                    Object obj;
                    if (node.typeReference().isPresent()) {
                        TypeReference<?> tr = Util.typeReferenceOf(node.typeReference().get());
                        if (node.isCollection())
                            if (!Collection.class.isAssignableFrom(val.getClass()))
                                // TODO: PANIC! Something is wrong... what should we do??
                                obj = null;
                            else
                                obj = convertToCollection( (Class)Util.rawClassOf(tr), (Class)node.collectionType(), (Collection)val, schema, path);
                        else
                            obj = convertToDTO((Class<? extends DTO>)rawClassOf(tr), (Map<?,?>)val, schema, path + "/");
                    } else {
                        if (node.isCollection()) {
                            Collection c = instantiateCollection(node.collectionType());
                            Type type = node.type();
                            for (Object o : (Collection)val) {
                                if (o == null)
                                    c.add(null);
                                else if (asDTO(rawClassOf(type)))
                                    c.add(convertToDTO((Class)Util.rawClassOf(type), (Map)o, schema, path + "/"));
                                else
                                    c.add(converter.convert(o).to(type));
                            }
                            obj = c;
                        } else {
                            Class<?> rawClass = rawClassOf(node.type());
                            if (asDTO(rawClass))
                                obj = convertToDTO((Class<? extends DTO>)rawClass, (Map<?,?>)val, schema, path + "/");
                            else
                                obj = converter.convert(val).to(node.type());
                        }
                    }

                    f.set(dto, obj);
                } catch (NoSuchFieldException e) {
                }
            }

            return dto;
        } catch (Exception e) {
            throw new ConversionException("Cannot create DTO " + targetCls, e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <U, V extends Collection<U>>V convertToCollection(Class<U> targetCls, Class<V> collectionClass, Collection sourceCollection, Schema schema, String path) {
        try {
            V targetCollection = instantiateCollection(collectionClass);
            sourceCollection.stream()
                .map(obj -> convertCollectionItemToObject(obj, targetCls, schema, path))
                .forEach(u -> targetCollection.add((U)u));

            return targetCollection;
        } catch (Exception e) {
            throw new ConversionException("Cannot create DTO " + targetCls, e);
        }
    }

    @SuppressWarnings( "unchecked" )
    private <V extends Collection<?>>V instantiateCollection(Class<V> collectionClass) {
        if (collectionClass == null)
            return (V)new ArrayList<V>();
        if (Collection.class.equals(collectionClass) || List.class.isAssignableFrom(collectionClass))
            return (V)new ArrayList<V>();
        else
            // TODO: incomplete
            return null;
    }

    private <U>U convertCollectionItemToObject(Object obj, Class<U> targetCls, Schema schema, String path) {
        try
        {
            if (asDTO(targetCls))
                return convertCollectionItemToDTO(obj, targetCls, schema, path);

            U newItem = targetCls.newInstance();
            return newItem;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings( "unchecked" )
    private <U>U convertCollectionItemToDTO(Object obj, Class<U> targetCls, Schema schema, String path) {
        Node node = schema.nodeAtPath(path);
        if (node.typeReference().isPresent()) {
            TypeReference<U> tr = (TypeReference<U>)Util.typeReferenceOf(node.typeReference().get());
            return converter.convert(obj).to(tr);
        } else {
            Type type = node.type();
            type.toString();
            // TODO
//            if (DTO.class.isAssignableFrom(Util.rawClassOf(type)))
//                obj = convertToDTO((Class)Util.rawClassOf(type), (Map)val, schema, path + "/" );
//            else
//                obj = converter.convert(val).to(type);
            return null;
        }
    }

    // TODO
    private T handleInvalid() {
        return null;
    }
}
