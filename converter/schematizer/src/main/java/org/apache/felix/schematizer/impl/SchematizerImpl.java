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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.felix.schematizer.Schematizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TypeReference;

import static org.apache.felix.schematizer.impl.Util.*;

public class SchematizerImpl implements Schematizer, ServiceFactory<Schematizer> {
    private final Map<String, SchemaImpl> schemas = new HashMap<>();
    private final Map<String, Map<String, Object>> typeRules = new HashMap<>();

    @Override
    public Schematizer getService( Bundle bundle, ServiceRegistration<Schematizer> registration ) {
        return this;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Schematizer> registration, Schematizer service) {
        // For now, a brutish, simplistic version. If there is any change to the environment, just
        // wipe the state and start over.
        //
        // TODO: something more precise, which will remove only the classes that are no longer valid (if that is possible).
        schemas.clear();
    }

    @Override
    public Schematizer type(String schemaName, String path, TypeReference<?> type) {
        Map<String, Object> rules = rulesFor(schemaName);
        // The internal implementation uses "" as the path for the root,
        // but the API accepts "/".
        path = "/".equals( path ) ? "" : path;
        rules.put(path, type);
        return this;
    }

    @Override
    public Schematizer type(String schemaName, String path, Class<?> cls) {
        Map<String, Object> rules = rulesFor(schemaName);
        // The internal implementation uses "" as the path for the root,
        // but the API accepts "/".
        path = "/".equals( path ) ? "" : path;
        rules.put(path, cls);
        return this;
    }

    private Map<String, Object> rulesFor(String schemaName) {
        if (!typeRules.containsKey(schemaName))
            typeRules.put(schemaName, new HashMap<>());

        return typeRules.get(schemaName);
    }

    @Override
    public SchematizerImpl schematize(String schemaName, Object type) {
        return schematize(schemaName, type, "");
    }

    public SchematizerImpl schematize(String schemaName, Object type, String context) {
        // TODO: test to ensure that the schema is not already in the cache
        Map<String, Object> rules = typeRules.get(schemaName);
        rules = ( rules != null ) ? rules : Collections.emptyMap();
        SchemaImpl schema = internalSchematize(schemaName, type, context, rules, false);
        schemas.put(schemaName, schema);
        return this;
    }

    private static SchemaImpl internalSchematize(
            String schemaName, 
            Object unknownType, 
            String contextPath,
            Map<String, Object> rules,
            boolean isCollection) {

        TypeRefOrClass type = new TypeRefOrClass(unknownType, rules.get(contextPath));

        if (asDTO(type.cls)) {
            return schematizeDTO(schemaName, type, contextPath, rules, isCollection);
        }

        return schematizeObject(schemaName, type.cls, contextPath, isCollection);
    }

    private static SchemaImpl schematizeDTO(
            String schemaName,
            TypeRefOrClass type,
            String contextPath,
            Map<String, Object> rules,
            boolean isCollection ) {

        SchemaImpl schema = new SchemaImpl(schemaName);
        NodeImpl rootNode = new NodeImpl(contextPath, type.isTypeRef() ? type.typeRef : type.cls, false, contextPath + "/");
        schema.add(rootNode);
        Map<String, NodeImpl> m = createMapFromDTO(schemaName, type, rules, contextPath);
        m.values().stream()
            .filter(v -> v.absolutePath().equals(rootNode.absolutePath() + v.name()))
            .forEach(v -> rootNode.add(v));
        associateChildNodes( rootNode );
        schema.add(m);
        return schema;
    }

    private static SchemaImpl schematizeObject(
            String schemaName,
            Class<?> targetCls,
            String contextPath,
            boolean isCollection) {

        SchemaImpl schema = new SchemaImpl(schemaName);
        NodeImpl node = new NodeImpl(contextPath, targetCls, isCollection, contextPath + "/");
        schema.add(node);
        return schema;
    }

    private static final Comparator<Entry<String, NodeImpl>> byPath = (e1, e2) -> e1.getValue().absolutePath().compareTo(e2.getValue().absolutePath());
    private static Map<String, NodeImpl> createMapFromDTO(
            String schemaName,
            TypeRefOrClass type,
            Map<String, Object> rules,
            String contextPath) {
        Set<String> handledFields = new HashSet<>();

        Map<String, NodeImpl> result = new HashMap<>();
        for (Field f : type.cls.getDeclaredFields()) {
            handleField(schemaName, f, rules, handledFields, result, contextPath);
        }
        for (Field f : type.cls.getFields()) {
            handleField(schemaName, f, rules, handledFields, result, contextPath);
        }

        return result.entrySet().stream()
                .sorted(byPath)
                .collect(Collectors.toMap(
                        Entry::getKey, 
                        Entry::getValue, 
                        (e1, e2) -> e1, 
                        LinkedHashMap::new));
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static void handleField(
            String schemaName,
            Field field,
            Map<String, Object> rules,
            Set<String> handledFields,
            Map<String, NodeImpl> result,
            String contextPath) {
        if (Modifier.isStatic(field.getModifiers()))
            return;

        String fieldName = field.getName();
        if (handledFields.contains(fieldName))
            return; // Field with this name was already handled

        try {
            String path = contextPath + "/" + fieldName;
            NodeImpl node;
            if (rules.containsKey(path)) {
                // The actual field. Since the type for this node is provided as a rule, we
                // only need it to test whether or not it is a collection.
                Class<?> actualFieldType = field.getType();
                boolean isCollection = Collection.class.isAssignableFrom(actualFieldType);
                Class<?> ruleBasedClass = rawClassOf(rules.get(path));
                // This is the type we will persist in the Schema (as provided by the rules), NOT the "actual" field type.
                SchemaImpl embedded = SchematizerImpl.internalSchematize(schemaName, ruleBasedClass, path, rules, isCollection);
                Class<?> fieldClass = Util.primitiveToBoxed(ruleBasedClass);
                TypeRefOrClass fieldType = new TypeRefOrClass(fieldClass,rules.get(path));
                if (isCollection)
                    node = new CollectionNode(
                            field.getName(),
                            fieldType.get(),
                            path,
                            (Class)actualFieldType);
                else
                    node = new NodeImpl(fieldName, fieldType.get(), false, path);
                Map<String, NodeImpl> allNodes = embedded.toMapInternal();
                allNodes.remove(path + "/");
                result.putAll(allNodes);
                Map<String, NodeImpl> childNodes = extractChildren(path, allNodes);
                node.add(childNodes);
            } else {
                Type fieldType = field.getType();
                Class<?> rawClass = rawClassOf(fieldType);
                Class<?> fieldClass = primitiveToBoxed(rawClass);

                if (isCollectionType(fieldClass)) {
                    Class<?> collectionType = getCollectionTypeOf(field);
                    node = new CollectionNode(
                            field.getName(),
                            collectionType,
                            path,
                            (Class)fieldClass);

                    if (asDTO(collectionType)) {
//                        newSchematizer.typeRules.put(path, rules);
//                        if (!rules.containsKey(path))
//                            newSchematizer.rule(path, path, collectionType);
                        SchemaImpl embedded = new SchematizerImpl().schematize(path, collectionType, path).get(path);
                        Map<String, NodeImpl> allNodes = embedded.toMapInternal();
                        allNodes.remove(path + "/");
                        result.putAll(allNodes);
                        Map<String, NodeImpl> childNodes = extractChildren(path, allNodes);
                        node.add(childNodes);
                    }
                }
                else if (asDTO(fieldClass)) {
//                    newSchematizer.typeRules.put(path, rules);
//                    if (!rules.containsKey(path))
//                        newSchematizer.rule(path, path, fieldClass);
                    SchemaImpl embedded = new SchematizerImpl().schematize(path, fieldClass, path).get(path);
                    node = new NodeImpl(
                            field.getName(),
                            fieldClass,
                            false,
                            path);
                    Map<String, NodeImpl> allNodes = embedded.toMapInternal();
                    allNodes.remove(path + "/");
                    result.putAll(allNodes);
                    Map<String, NodeImpl> childNodes = extractChildren(path, allNodes);
                    node.add(childNodes);
                } else {
                    node = new NodeImpl(
                            field.getName(),
                            fieldClass,
                            false,
                            path);
                }
            }

            result.put(node.absolutePath(), node);
            handledFields.add(fieldName);
        } catch (Exception e) {
            // Ignore this field
            // TODO print warning??
            return;
        }
    }

    static private void associateChildNodes(NodeImpl rootNode) {
        for (NodeImpl child: rootNode.childrenInternal().values()) {
            child.parent(rootNode);
            String fieldName = child.name();
            Class<?> parentClass = rawClassOf(rootNode.type());
            try {
                Field field = parentClass.getField(fieldName);
                child.field(field);
            } catch ( NoSuchFieldException e ) {
                e.printStackTrace();
            }            

            associateChildNodes(child);
        }
    }

    @Override
    public SchemaImpl get(String schemaName) {
        return schemas.get(schemaName);
    }

    @Override
    public Converter converterFor(String schemaName) {
//        ConverterBuilder b = new StandardConverter().newConverterBuilder();
//        Schema s = schemas.get(schemaName);
//        RuleExtractor ex = new RuleExtractor();
//        s.visit( ex );
//        ex.rules().stream().forEach( rule -> b.rule(rule) );
//        return b.build();
        return new StandardConverter()
                .newConverterBuilder()
                .rule(new SchemaBasedConverter<Object>(schemas.get(schemaName)))
                .build();
    }

//    private static class RuleExtractor implements NodeVisitor {
//        private final List<TargetRule<?>> rules = new ArrayList<>();
//
//        @Override
//        public void apply(Node node) {
//            rules.add(new DTOTargetRule<Type>(node));
//        }
//
//        List<TargetRule<?>> rules() {
//            return rules;
//        }
//    }
//    private static class DTOTargetRule<T> implements TargetRule<T> {
//        private final Type type;
//
//        public DTOTargetRule(Node node) {
//            if (node.isCollection())
//                type = new CollectionType(node.collectionType(), new TypeRefOrClass(node.type()));
//            else
//                type = node.type();
//        }
//
//        @Override
//        public ConverterFunction<T> getFunction() {
//            return (obj,t) -> {
//                TypeRefOrClass type = null;
//                if(t instanceof CollectionType) {
//                    return convertCollection((Collection<?>)obj, (CollectionType)t);
//                } else {
//                    type = new TypeRefOrClass(t);
//                    return convertObject(obj,type);                    
//                }
//            };
//        }
//
//        @SuppressWarnings( "unchecked" )
//        private T convertCollection(Collection<?> c, CollectionType type) {
//            Collection<Object> copy = newCollection(type);
//            for(Object obj : c)
//                copy.add((Object)convertObject(obj, type.itemType));
//            return (T)copy;
//        }
//
//        private Collection<Object> newCollection(CollectionType type) {
//            // TODO what else?
//            return new ArrayList<>();
//        }
//
//        @SuppressWarnings( "unchecked" )
//        private T convertObject(Object obj, TypeRefOrClass type) {
//            Converter c = new StandardConverter();
//            if (asDTO(type.getClassType()))
//                if(type.isTypeRef())
//                    return c.convert(obj).targetAsDTO().to((TypeReference<T>)type.getTypeRef());
//                else
//                    return c.convert(obj).targetAsDTO().to(type.getType());
//            return c.convert(obj).targetAsDTO().to(type.getType());
//        }
//
//        @Override
//        public Type getTargetType() {
//            if (type instanceof CollectionType)
//                return ((CollectionType)type).collectionType;
//            return type;
//        }
//    };

    static class TypeRefOrClass {
        TypeReference<?> typeRef;
        Class<?> cls;

        public TypeRefOrClass(Object type, Object ruleBasedType) {
            this(ruleBasedType != null ? ruleBasedType : type);
        }

        public TypeRefOrClass(Object type) {
            typeRef = (TypeReference<?>)(typeReferenceOf(type));
            if (typeRef != null )
                cls = rawClassOf(typeRef);
            else
                cls = rawClassOf(type);
        }

        boolean isTypeRef() {
            return typeRef != null;
        }

        TypeReference<?> getTypeRef() {
            return typeRef;
        }

        Object get() {
            if (typeRef != null )
                return typeRef;
            return cls;
        }

        Type getType() {
            if (typeRef != null)
                return typeRef.getType();
            return cls;
        }

        Class<?> getClassType() {
            Type t = getType();
            if (t instanceof Class)
                return (Class<?>)t;
            return t.getClass();
        }
    }

    static class CollectionType implements Type {
        Class<? extends Collection<?>> collectionType;
        TypeRefOrClass itemType;

        public CollectionType(Class<? extends Collection<?>> aCollectionType, TypeRefOrClass anItemType) {
            collectionType = aCollectionType;
            itemType = anItemType;
        }
    }
}