/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.bundleplugin;


import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.osgi.Instruction;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;


/**
 * Apply clause-based filter over given dependencies
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractDependencyFilter
{
    private static final Pattern MISSING_KEY_PATTERN = Pattern.compile( "(^|,)\\p{Blank}*(!)?\\p{Blank}*([a-zA-Z]+=)" );

    /**
     * Dependency Graph.
     */
    private final DependencyNode m_dependencyGraph;
    /**
     * Dependency artifacts.
     */
    private final Collection<Artifact> m_dependencyArtifacts;


    public AbstractDependencyFilter( DependencyNode dependencyGraph, Collection<Artifact> dependencyArtifacts )
    {
        m_dependencyGraph = dependencyGraph;
        m_dependencyArtifacts = dependencyArtifacts;
    }

    private static abstract class DependencyFilter implements ArtifactFilter
    {
        private final Instruction m_instruction;
        private final String m_defaultValue;


        public DependencyFilter( String expression )
        {
            this( expression, "" );
        }


        public DependencyFilter( String expression, String defaultValue )
        {
            m_instruction = new Instruction( expression );
            m_defaultValue = defaultValue;
        }

        public abstract boolean include( Artifact dependency );

        boolean matches( String text )
        {
            boolean result;

            if ( null == text )
            {
                result = m_instruction.matches( m_defaultValue );
            }
            else
            {
                result = m_instruction.matches( text );
            }

            return m_instruction.isNegated() ? !result : result;
        }
    }

    private static class TrimmingDependencyNodeFilter implements DependencyNodeFilter
    {
        private DependencyNodeFilter dependencyNodeFilter;

        public TrimmingDependencyNodeFilter( DependencyNodeFilter dependencyNodeFilter ) 
        {
            this.dependencyNodeFilter = dependencyNodeFilter;
        }
        
        public boolean accept( DependencyNode node )
        {
            boolean accepted = dependencyNodeFilter.accept( node );
            if( !accepted )
            {
                List<DependencyNode> children = node.getChildren();
                children.clear();
            }
            return accepted;
        }
    }

    protected final void processInstructions( String header ) throws MojoExecutionException
    {
        Map<String,Attrs> instructions = OSGiHeader.parseHeader( MISSING_KEY_PATTERN.matcher( header ).replaceAll( "$1$2*;$3" ) );

        Collection<Artifact> availableDependencies = new LinkedHashSet<Artifact>( m_dependencyArtifacts );

        for ( Iterator<Map.Entry<String,Attrs>> clauseIterator = instructions.entrySet().iterator(); clauseIterator.hasNext(); )
        {
            String inline = "false";

            // always start with a fresh *modifiable* collection for each unique clause
            Collection<Artifact> filteredDependencies = new LinkedHashSet<Artifact>( availableDependencies );

            // CLAUSE: REGEXP --> { ATTRIBUTE MAP }
            Map.Entry<String,Attrs> clause = clauseIterator.next();
            String primaryKey = clause.getKey().replaceFirst( "~+$", "" );
            boolean isNegative = primaryKey.startsWith( "!" );
            if ( isNegative )
            {
                primaryKey = primaryKey.substring( 1 );
            }

            final AndArtifactFilter andArtifactFilter = new AndArtifactFilter();
            if ( !"*".equals( primaryKey ) )
            {
                ArtifactFilter filter = new DependencyFilter( primaryKey )
                {
                    @Override
                    public boolean include( Artifact dependency )
                    {
                        return super.matches( dependency.getArtifactId() );
                    }
                };
                // FILTER ON MAIN CLAUSE
                andArtifactFilter.add(filter);
            }

            for ( Iterator<Map.Entry<String,String>> attrIterator = clause.getValue().entrySet().iterator(); attrIterator.hasNext(); )
            {
                final ArtifactFilter filter;
                // ATTRIBUTE: KEY --> REGEXP
                Map.Entry<String,String> attr = attrIterator.next();
                if ( "groupId".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue() )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            return super.matches( dependency.getGroupId() );
                        }
                    };
                }
                else if ( "artifactId".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue() )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            return super.matches( dependency.getArtifactId() );
                        }
                    };
                }
                else if ( "version".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue() )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            try
                            {
                                // use the symbolic version if available (ie. 1.0.0-SNAPSHOT)
                                return super.matches( dependency.getSelectedVersion().toString() );
                            }
                            catch ( Exception e )
                            {
                                return super.matches( dependency.getVersion() );
                            }
                        }
                    };
                }
                else if ( "scope".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue(), "compile" )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            return super.matches( dependency.getScope() );
                        }
                    };
                }
                else if ( "type".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue(), "jar" )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            return super.matches( dependency.getType() );
                        }
                    };
                }
                else if ( "classifier".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue() )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            return super.matches( dependency.getClassifier() );
                        }
                    };
                }
                else if ( "optional".equals( attr.getKey() ) )
                {
                    filter = new DependencyFilter( attr.getValue(), "false" )
                    {
                        @Override
                        public boolean include( Artifact dependency )
                        {
                            return super.matches( "" + dependency.isOptional() );
                        }
                    };
                }
                else if ( "inline".equals( attr.getKey() ) )
                {
                    inline = attr.getValue();
                    continue;
                }
                else
                {
                    throw new MojoExecutionException( "Unexpected attribute " + attr.getKey() );
                }

                // FILTER ON EACH ATTRIBUTE
                andArtifactFilter.add( filter );
            }

            filteredDependencies( andArtifactFilter, filteredDependencies );

            if ( isNegative )
            {
                // negative clauses reduce the set of available artifacts
                availableDependencies.removeAll( filteredDependencies );
                if ( !clauseIterator.hasNext() )
                {
                    // assume there's an implicit * missing at the end
                    processDependencies( availableDependencies, inline );
                }
            }
            else
            {
                // positive clause; doesn't alter the available artifacts
                processDependencies( filteredDependencies, inline );
            }
        }
    }


    protected abstract void processDependencies( Collection<Artifact> dependencies, String inline );

    private void filteredDependencies( final ArtifactFilter artifactFilter, Collection<Artifact> filteredDependencies )
    {
        CollectingDependencyNodeVisitor collectingDependencyNodeVisitor = new CollectingDependencyNodeVisitor();
        final Artifact rootArtifact = m_dependencyGraph.getArtifact();
        ArtifactFilter filter = new ArtifactFilter()
        {


            public boolean include( Artifact artifact )
            {
                return artifact.equals( rootArtifact ) || artifactFilter.include( artifact );
            }


        };
        DependencyNodeFilter dependencyNodeFilter = new ArtifactDependencyNodeFilter( filter );
        dependencyNodeFilter = new TrimmingDependencyNodeFilter( dependencyNodeFilter );
        DependencyNodeVisitor dependencyNodeVisitor =
                new FilteringDependencyNodeVisitor( collectingDependencyNodeVisitor, dependencyNodeFilter );
        dependencyNodeVisitor = new BuildingDependencyNodeVisitor( dependencyNodeVisitor );
        m_dependencyGraph.accept( dependencyNodeVisitor );
        List<DependencyNode> dependencyNodes = collectingDependencyNodeVisitor.getNodes();
        Set<String> ids = new LinkedHashSet<String>( dependencyNodes.size() );
        for( DependencyNode dependencyNode : dependencyNodes ) {
            Artifact artifact = dependencyNode.getArtifact();
            String id = artifact.getId();
            ids.add(id);
        }
        for (Iterator<Artifact> iterator = filteredDependencies.iterator(); iterator.hasNext();)
        {
            Artifact artifact = iterator.next();
            String id = artifact.getId();
            if (!ids.contains(id))
            {
                iterator.remove();
            }
        }
    }
}
