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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.felix.schematizer.Node;
import org.osgi.service.converter.TypeReference;

public class NodeImpl implements Node {

    private final String name;
    private final Object type;
    private final boolean isCollection;
    private final String absolutePath;

    private NodeImpl parent;
    private HashMap<String, NodeImpl> children = new HashMap<>();

    public NodeImpl(
            String aName,
            Type aType,
            boolean isACollection,
            String anAbsolutePath ) {
        name = aName;
        type = aType;
        isCollection = isACollection;
        absolutePath = anAbsolutePath;
    }

    public NodeImpl(
            String aName,
            TypeReference<?> aTypeRef,
            boolean isACollection,
            String anAbsolutePath ) {
        name = aName;
        type = aTypeRef;
        isCollection = isACollection;
        absolutePath = anAbsolutePath;
    }

    public NodeImpl(Node.DTO dto, String contextPath, Function<String, Type> f, Map<String, NodeImpl> nodes) {
        name = dto.name;
        type = f.apply(dto.type);
        isCollection = dto.isCollection;
        absolutePath = contextPath + dto.path;
        dto.children.values().stream().forEach( c -> {
                NodeImpl node = new NodeImpl(c, contextPath, f, nodes);
                children.put("/" + c.name, node);
                nodes.put(c.path, node);
            });
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
