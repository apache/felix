/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.felix.schematizer.Node;
import org.osgi.util.converter.TypeReference;

public class NodeImpl implements Node {

    private final String name;
    private final Object type;
    private final boolean isCollection;
    private final String absolutePath;

    private NodeImpl parent;
    private HashMap<String, NodeImpl> children = new HashMap<>();
    private Field field;

    public NodeImpl(
            String aName,
            Object aType,
            boolean isACollection,
            String anAbsolutePath ) {
        name = aName;
        type = aType;
        isCollection = isACollection;
        absolutePath = anAbsolutePath;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public NodeImpl(Node.DTO dto, String contextPath, Function<String, Type> f, Map<String, NodeImpl> nodes) {
        name = dto.name;
        type = f.apply(dto.type);
        isCollection = dto.isCollection;
        absolutePath = contextPath + dto.path;
        for (Node.DTO child : dto.children.values()) {
            NodeImpl node;
            if (child.isCollection)
                try {
                    node = new CollectionNode(child, contextPath, f, nodes, (Class)getClass().getClassLoader().loadClass(child.collectionType));
                } catch ( ClassNotFoundException e ) {
                    node = new CollectionNode(child, contextPath, f, nodes, (Class)Collection.class);
                }
            else
                node = new NodeImpl(child, contextPath, f, nodes);
            children.put("/" + child.name, node);
            nodes.put(child.path, node);        
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type() {
        if (type instanceof TypeReference)
            return ((TypeReference<?>)type).getType();
        return (Type)type;
    }

    @Override
    public Optional<TypeReference<?>> typeReference() {
        if (type instanceof TypeReference)
            return Optional.of((TypeReference<?>)type);
        return Optional.empty();
    }

    @Override
    public boolean isCollection() {
        return isCollection;
    }

    @Override
    public String absolutePath() {
        return absolutePath;
    }

    @Override
    public Field field() {
        return field;
    }

    public void field(Field aField) {
        field = aField;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public Map<String, Node> children() {
        return (Map)childrenInternal();
    }

    @Override
    public Class<? extends Collection<?>> collectionType() {
        return null;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    Map<String, NodeImpl> childrenInternal() {
        return (Map)children.clone();
    }

    NodeImpl parent() {
        return parent;
    }

    void parent(NodeImpl aParent) {
        parent = aParent;
    }

    void add(NodeImpl child) {
        children.put(child.absolutePath, child);
    }

    void add(Map<String, NodeImpl> moreChildren) {
        children.putAll(moreChildren);
    }

    public Node.DTO toDTO() {
        Node.DTO dto = new Node.DTO();
        dto.name = name();
        dto.path = absolutePath();
        dto.type = type().getTypeName();
        dto.isCollection = isCollection();
        childrenInternal().values().stream().forEach(v -> dto.children.put(v.name, v.toDTO()));
        return dto;
    }

    @Override
    public String toString() {
        return absolutePath;
    }
}
