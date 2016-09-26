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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.felix.schematizer.Node;
import org.apache.felix.schematizer.NodeVisitor;
import org.apache.felix.schematizer.Schema;

public class SchemaImpl
        implements Schema
{
    private final String name;
    private final HashMap<String, NodeImpl> nodes = new LinkedHashMap<>();

    public SchemaImpl(String aName) {
        name = aName;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Node rootNode() {
        return rootNodeInternal();
    }

    public NodeImpl rootNodeInternal() {
        return nodes.get("/");        
    }

    @Override
    public Optional<Node> nodeAtPath( String absolutePath )
    {
        return Optional.ofNullable(nodes.get(absolutePath));
    }

    void add(NodeImpl node) {
        nodes.put(node.absolutePath(), node);
    }

    void add(Map<String, NodeImpl> moreNodes) {
        nodes.putAll(moreNodes);
    }

    @Override
    public Map<String, Node.DTO> toMap() {
        NodeImpl root = nodes.get("/");
        Map<String, Node.DTO> m = new HashMap<>();
        m.put("/",root.toDTO());
        return m;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    Map<String, NodeImpl> toMapInternal() {
        return (Map)nodes.clone();
    }

    @Override
    public void visit(NodeVisitor visitor) {
        nodes.values().stream().forEach(n ->  visitor.apply(n));
    }
}
