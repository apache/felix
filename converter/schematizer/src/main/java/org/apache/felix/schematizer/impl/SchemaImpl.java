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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.felix.schematizer.Node;
import org.apache.felix.schematizer.NodeVisitor;
import org.apache.felix.schematizer.Schema;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.StandardConverter;

public class SchemaImpl implements Schema {
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
    public boolean hasNodeAtPath(String absolutePath) {
        return nodes.containsKey(absolutePath);
    }

    @Override
    public Node nodeAtPath( String absolutePath ) {
        return nodes.get(absolutePath);
    }

    @Override
    public Node parentOf( Node aNode ) {
        if (aNode == null || aNode.absolutePath() == null)
            return Node.ERROR;

        NodeImpl node = nodes.get(aNode.absolutePath());
        if (node == null)
            return Node.ERROR;

        return node.parent();
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

    @Override
    public Collection<?> valuesAt(String path, Object object) {
        final Converter converter = new StandardConverter();
        @SuppressWarnings( "unchecked" )
        final Map<String, Object> map = (Map<String, Object>)converter.convert(object).sourceAsDTO().to( Map.class );
        if (map == null || map.isEmpty())
            return Collections.emptyList();

        if (path.startsWith("/"))
            path = path.substring(1);
        String[] pathParts = path.split("/");
        if (pathParts.length <= 0)
            return Collections.emptyList();

        List<String> contexts = Arrays.asList(pathParts);

        return valuesAt("", map, contexts, 0);
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private Collection<?> valuesAt(String context, Map<String, Object> objectMap, List<String> contexts, int currentIndex) {
        List<Object> result = new ArrayList<>();
        String currentContext = contexts.get(currentIndex);
        if (objectMap == null)
            return result;
        Object o = objectMap.get(currentContext);
        if (o instanceof List) {
            List<Object> l = (List<Object>)o;
            if (currentIndex == contexts.size() - 1) {
                // We are at the end, so just add the collection
                result.add(convertToType(pathFrom(contexts, 0), l));
                return result;
            }

            currentContext = pathFrom(contexts, ++currentIndex);
            for (Object o2 : l)
            {
                final Converter converter = new StandardConverter();
                final Map<String, Object> m = (Map<String, Object>)converter.convert(o2).sourceAsDTO().to( Map.class );
                result.addAll( valuesAt( currentContext, m, contexts, currentIndex ) );
            }        
        } else if (o instanceof Map){
            if (currentIndex == contexts.size() - 1) {
                // We are at the end, so just add the result
                result.add(convertToType(pathFrom(contexts, 0), (Map)o));
                return result;
            }

            result.addAll(valuesAt( currentContext, (Map)o, contexts, ++currentIndex));
        } else if (currentIndex < contexts.size() - 1) {
            final Converter converter = new StandardConverter();
            final Map<String, Object> m = (Map<String, Object>)converter.convert(o).sourceAsDTO().to(Map.class);
            currentContext = pathFrom(contexts, ++currentIndex);
            result.addAll(valuesAt( currentContext, m, contexts, currentIndex ));
        } else {
            result.add(o);
        }

        return result;
    }

    @SuppressWarnings( "rawtypes" )
    private Object convertToType( String path, Map map ) {
        if (!hasNodeAtPath(path))
            return map;

        Node node = nodeAtPath(path);
        Object result = new StandardConverter().convert(map).targetAsDTO().to(node.type());
        return result;
    }

    private List<?> convertToType( String path, List<?> list ) {
        if (!hasNodeAtPath(path))
            return list;

        Node node = nodeAtPath(path);
        return list.stream()
                .map( v -> new StandardConverter().convert(v).sourceAsDTO().to(node.type()))
                .collect( Collectors.toList() );
    }

    private String pathFrom(List<String> contexts, int index) {
        return IntStream.range(0, contexts.size())
                .filter( i -> i >= index )
                .mapToObj( i -> contexts.get(i) )
                .reduce( "", (s1,s2) -> s1 + "/" + s2 );
                
    }
}