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
package org.apache.felix.webconsole.plugins.obr.internal;


import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;


/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
class OsgiBundleRepositoryRenderHelper extends AbstractBundleRepositoryRenderHelper
{

    OsgiBundleRepositoryRenderHelper( final AbstractWebConsolePlugin logger, final BundleContext bundleContext )
    {
        super( logger, bundleContext, RepositoryAdmin.class.getName() );
    }


    @Override
    String getData( final String filter, final boolean details, Bundle[] bundles )
    {
        RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
        if ( admin != null )
        {
            final StringWriter sw = new StringWriter();
            JSONWriter json = new JSONWriter(sw);
            try
            {
                json.object();
                json.key( "status" ); //$NON-NLS-1$
                json.value(true);
                json.key( "details" ); //$NON-NLS-1$
                json.value(details);

                final Repository repositories[] = admin.listRepositories();
                if ( repositories != null )
                {
                    json.key("repositories"); //$NON-NLS-1$
                    json.array();
                    for ( int i = 0; i < repositories.length; i++ )
                    {
                        json.object();
                        json.key("lastModified"); //$NON-NLS-1$
                        json.value(repositories[i].getLastModified());
                        json.key("name"); //$NON-NLS-1$
                        json.value(repositories[i].getName());
                        json.key("url"); //$NON-NLS-1$
                        json.value(repositories[i].getURL());
                        json.endObject();
                    }
                    json.endArray();
                }
                Resource[] resources = admin.discoverResources( filter );
                if ( resources != null )
                {
                    json.key("resources"); //$NON-NLS-1$
                    json.array();
                    for ( int i = 0; i < resources.length; i++ )
                    {
                        toJSON( json, resources[i], bundles, details );
                    }
                    json.endArray();
                }
                json.endObject();
                json.flush();
            }
            catch ( IOException e )
            {
                logger.log( "Failed to serialize repository to JSON object.", e );
            }
            catch ( Exception e )
            {
                logger.log( "Failed to parse filter '" + filter + "'", e );
                try
                {
                    String reason = "filter=" + filter;
                    if ( e.getMessage() != null )
                    {
                        reason = e.getMessage() + "(" + reason + ")";
                    }
                    json.key( "error" ); //$NON-NLS-1$
                    json.value(reason);
                    json.endObject();
                    json.flush();
                }
                catch ( IOException je )
                {
                    // ignore
                }
            }

            return sw.toString();
        }

        // fall back to no data
        return "{}"; //$NON-NLS-1$
    }

    @Override
    void doAction( String action, String urlParam ) throws IOException, ServletException
    {
        RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
        Repository[] repos = admin.listRepositories();
        Repository repo = getRepository( repos, urlParam );

        URL uri = repo != null ? repo.getURL() : new URL( urlParam );

        if ( "delete".equals( action ) ) //$NON-NLS-1$
        {
            if ( !admin.removeRepository( uri ) )
            {
                throw new ServletException( "Failed to remove repository with URL " + uri );
            }
        }
        else if ( "add".equals( action ) || "refresh".equals( action ) ) //$NON-NLS-1$ //$NON-NLS-2$
        {
            try
            {
                admin.addRepository( uri );
            }
            catch ( IOException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new ServletException( "Failed to " + action + " repository " + uri + ": " + e.toString() );
            }

        }
    }


    @Override
    final void doDeploy( String[] bundles, boolean start, boolean optional )
    {
        // check whether we have to do something
        if ( bundles == null || bundles.length == 0 )
        {
            logger.log( "No resources to deploy" );
            return;
        }

        RepositoryAdmin repoAdmin = ( RepositoryAdmin ) getRepositoryAdmin();
        Resolver resolver = repoAdmin.resolver();

        // prepare the deployment
        for ( int i = 0; i < bundles.length; i++ )
        {
            String bundle = bundles[i];
            if ( bundle == null || bundle.equals( "-" ) )
            {
                continue;
            }

            String filter = "(id=" + bundle + ")";
            Resource[] resources = repoAdmin.discoverResources( filter );
            if ( resources != null && resources.length > 0 )
            {
                resolver.add( resources[0] );
            }
        }

        OsgiDeployer.deploy( resolver, logger, start );
    }


    private final Repository getRepository( Repository[] repos, String repositoryUrl )
    {
        if ( repositoryUrl == null || repositoryUrl.length() == 0 )
        {
            return null;
        }

        for ( int i = 0; i < repos.length; i++ )
        {
            if ( repositoryUrl.equals( repos[i].getURL().toString() ) )
            {
                return repos[i];
            }
        }

        return null;
    }


    private final void toJSON( JSONWriter json, Resource resource, Bundle[] bundles, boolean details ) throws IOException
    {
        final String symbolicName = resource.getSymbolicName();
        final Version version = resource.getVersion();
        String installed = "";
        for ( int i = 0; symbolicName != null && installed.length() == 0 && bundles != null && i < bundles.length; i++ )
        {
            final Version ver = bundles[i].getVersion();
            if ( symbolicName.equals(bundles[i].getSymbolicName()))
            {
                installed = ver.toString();
            }
        }
        json.object();
        json.key("id"); //$NON-NLS-1$
        json.value(resource.getId());
        json.key("presentationname"); //$NON-NLS-1$
        json.value(resource.getPresentationName());
        json.key("symbolicname"); //$NON-NLS-1$
        json.value(symbolicName);
        json.key("url"); //$NON-NLS-1$
        json.value(resource.getURL());
        json.key("version"); //$NON-NLS-1$
        json.value(version);
        json.key("categories"); //$NON-NLS-1$
        json.value(resource.getCategories());
        json.key("installed"); //$NON-NLS-1$
        json.value(installed);

        if ( details )
        {
            Capability[] caps = resource.getCapabilities();
            if ( caps != null )
            {
                json.key("capabilities"); //$NON-NLS-1$
                json.array();
                for ( int i = 0; i < caps.length; i++ )
                {
                    json.key("name"); //$NON-NLS-1$
                    json.value(caps[i].getName());
                    json.key("properties"); //$NON-NLS-1$
                    toJSON(json, caps[i].getProperties());
                }
                json.endArray();
            }
            Requirement[] reqs = resource.getRequirements();
            if ( caps != null )
            {
                json.key("requirements"); //$NON-NLS-1$
                json.array();

                for ( int i = 0; i < reqs.length; i++ )
                {
                    json.key("name"); //$NON-NLS-1$
                    json.value(reqs[i].getName());
                    json.key("filter"); //$NON-NLS-1$
                    json.value(reqs[i].getFilter());
                    json.key("optional"); //$NON-NLS-1$
                    json.value(reqs[i].isOptional());
                }
                json.endArray();
            }

            final RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
            Resolver resolver = admin.resolver();
            resolver.add( resource );
            resolver.resolve(); // (Resolver.NO_OPTIONAL_RESOURCES);
            Resource[] required = resolver.getRequiredResources();
            if ( required != null )
            {
                json.key("required"); //$NON-NLS-1$
                json.array();
                for ( int i = 0; i < required.length; i++ )
                {
                    toJSON( json, required[i], bundles, false );
                }
                json.endArray();
            }
            Resource[] optional = resolver.getOptionalResources();
            if ( optional != null )
            {
                json.key("optional"); //$NON-NLS-1$
                json.array();
                for ( int i = 0; optional != null && i < optional.length; i++ )
                {
                    toJSON( json, optional[i], bundles, false );
                }
                json.endArray();
            }
            Requirement/*Reason*/[] unsatisfied = resolver.getUnsatisfiedRequirements();
            if ( unsatisfied != null )
            {
                json.key("unsatisfied"); //$NON-NLS-1$
                json.array();
                for ( int i = 0; i < unsatisfied.length; i++ )
                {
                    json.key("name"); //$NON-NLS-1$
                    json.value(unsatisfied[i].getName());
                    json.key("filter"); //$NON-NLS-1$
                    json.value(unsatisfied[i].getFilter());
                    json.key("optional"); //$NON-NLS-1$
                    json.value(unsatisfied[i].isOptional());
                }
                json.endArray();
            }
        }
        json.endObject();
    }

    private void toJSON( final JSONWriter writer, final Map props ) throws IOException
    {
        writer.object();
        Iterator i = props.entrySet().iterator();
        while ( i.hasNext() )
        {
            Map.Entry entry = (Entry) i.next();
            writer.key(entry.getKey().toString());
            writer.value(entry.getValue());
        }
        writer.endObject();
    }
}
