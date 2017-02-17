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
package org.apache.felix.serializer.impl.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.apache.felix.schematizer.Node;
import org.apache.felix.schematizer.Schema;
import org.apache.felix.schematizer.Schematizing;
import org.apache.felix.schematizer.impl.Util;
import org.osgi.dto.DTO;
import org.osgi.service.serializer.Deserializing;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.TypeReference;

public class JsonDeserializingImpl<T> implements Deserializing<T> {
    private final Object target;
    private Converter converter;
    private Schema schema;
    private boolean asDTO = false;

    public JsonDeserializingImpl(Converter c, Object t) {
        converter = c;
        target = t;
    }

    @Override
    public JsonDeserializingImpl<T> with(Converter c)
    {
        converter = c;
        if(converter instanceof Schematizing) {
            Schematizing s = (Schematizing)converter;
            schema = s.getSchema();
            asDTO = s.isDTOType();
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T from(CharSequence in) {
        JsonParser jp = new JsonParser(in);
        Map<?,?> m = jp.getParsed();
        Class<T> clazz = (Class<T>)Util.rawClassOf(target);
        if (m.getClass().isAssignableFrom(clazz))
            return (T) m;
        if (schema != null)
            return deserialize(m);

        if (asDTO)
            return converter.convert(m).targetAsDTO().to(clazz);
        else
            return converter.convert(m).to(clazz);
    }

    @Override
    public T from(InputStream in) {
        return from(in, StandardCharsets.UTF_8);
    }

    @Override
    public T from(InputStream in, Charset charset) {
        try {
            byte[] bytes = Util.readStream(in);
            String s = new String(bytes, charset);
            return from(s);
        } catch (IOException e) {
            throw new ConversionException("Error reading inputstream", e);
        }
    }

    @Override
    public T from(Readable in) {
        try (Scanner s = new Scanner(in)) {
            s.useDelimiter("\\Z");
            return from(s.next());
        }
    }

    private T deserialize(Map<?,?> map) {
        return deserialize(map, schema, "");
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private T deserialize(Map<?,?> map, Schema s, String contextPath) {
        if (s == null)
            return handleNull();

        Class<?> cls = null;
        if (contextPath.isEmpty()) {
            Optional<Node> opt = s.nodeAtPath("/");
            if (opt.isPresent())
                cls = Util.rawClassOf(opt.get().type());
        }

        if (cls == null) {
            Optional<Node> opt = s.nodeAtPath(contextPath);
            if (opt.isPresent())
                cls = Util.rawClassOf(opt.get().type());
        }

        if (cls == null)
            return handleInvalid();

        if (!DTO.class.isAssignableFrom(cls))
            return handleInvalid();

        Class<? extends DTO> targetCls = (Class)cls;

        if (!contextPath.endsWith("/"))
            contextPath = contextPath + "/";
        return (T)convertToDTO(targetCls, map, s, contextPath);
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
                    Optional<Node> opt = schema.nodeAtPath(path);
                    if (opt.isPresent()) {
                        Node node = opt.get();
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
                                obj = convertToDTO((Class)Util.rawClassOf(tr), (Map)val, schema, path + "/");
                        } else {
                            if (node.isCollection()) {
                                Collection c = instantiateCollection(node.collectionType());
                                Type type = node.type();
                                for (Object o : (Collection)val) {
                                    if (o == null)
                                        c.add(null);
                                    else if (DTO.class.isAssignableFrom(Util.rawClassOf(type)))
                                        c.add(convertToDTO((Class)Util.rawClassOf(type), (Map)o, schema, path + "/"));
                                    else
                                        c.add(converter.convert(o).to(type));
                                }
                                obj = c;
                            } else {
                                Type type = node.type();
                                if (DTO.class.isAssignableFrom(Util.rawClassOf(type)))
                                    obj = convertToDTO((Class)Util.rawClassOf(type), (Map)val, schema, path + "/");
                                else
                                    obj = converter.convert(val).to(type);
                            }
                        }

                        f.set(dto, obj);
                    }
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
            if (DTO.class.isAssignableFrom(targetCls ))
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
        Optional<Node> opt = schema.nodeAtPath(path);
        if (opt.isPresent()) {
            Node node = opt.get();
            if (node.typeReference().isPresent()) {
                TypeReference<U> tr = (TypeReference<U>)Util.typeReferenceOf(node.typeReference().get());
                return converter.convert(obj).to(tr);
            } else {
                Type type = node.type();
                type.toString();
                // TODO
//                if (DTO.class.isAssignableFrom(Util.rawClassOf(type)))
//                    obj = convertToDTO((Class)Util.rawClassOf(type), (Map)val, schema, path + "/" );
//                else
//                    obj = converter.convert(val).to(type);
                return null;
            }
        }

        return null;
    }

    private T handleNull() {
        return null;
    }

    private T handleInvalid() {
        return null;
    }
}
