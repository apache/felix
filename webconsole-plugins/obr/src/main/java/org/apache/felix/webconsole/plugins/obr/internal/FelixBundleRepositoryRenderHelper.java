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
<<<<<<< HEAD
import javax.servlet.ServletException;
=======
import java.io.StringWriter;

import javax.servlet.ServletException;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
<<<<<<< HEAD
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
=======
import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368


/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
class FelixBundleRepositoryRenderHelper extends AbstractBundleRepositoryRenderHelper
{

    FelixBundleRepositoryRenderHelper( AbstractWebConsolePlugin logger, BundleContext bundleContext )
    {
        super( logger, bundleContext, RepositoryAdmin.class.getName() );
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    String getData( final String filter, final boolean details, Bundle[] bundles )
    {
        RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
        if ( admin != null )
        {
<<<<<<< HEAD
            JSONObject json = new JSONObject();
            try
            {
                json.put( "status", true ); //$NON-NLS-1$
                json.put( "details", details ); //$NON-NLS-1$

                final Repository repositories[] = admin.listRepositories();
                for ( int i = 0; repositories != null && i < repositories.length; i++ )
                {
                    json.append( "repositories", new JSONObject() //$NON-NLS-1$
                        .put( "lastModified", repositories[i].getLastModified() ) //$NON-NLS-1$
                        .put( "name", repositories[i].getName() ) //$NON-NLS-1$
                        .put( "url", repositories[i].getURI() ) ); //$NON-NLS-1$
                }

                Resource[] resources = admin.discoverResources( filter );
                for ( int i = 0; resources != null && i < resources.length; i++ )
                {
                    json.append( "resources", toJSON( resources[i], bundles, details ) ); //$NON-NLS-1$
                }

            }
            catch ( JSONException e )
=======
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
                    json.key("repositories");//$NON-NLS-1$
                    json.array();
                    for ( int i = 0; repositories != null && i < repositories.length; i++ )
                    {
                        json.object();
                        json.key( "lastModified"); //$NON-NLS-1$
                        json.value( repositories[i].getLastModified());
                        json.key( "name"); //$NON-NLS-1$
                        json.value(repositories[i].getName() );
                        json.key( "url"); //$NON-NLS-1$
                        json.value( repositories[i].getURI() );
                        json.endObject();
                    }
                    json.endArray();
                }

                Resource[] resources = admin.discoverResources( filter );
                if ( resources != null )
                {
                    json.key("resources");//$NON-NLS-1$
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
                    json.put( "error", reason ); //$NON-NLS-1$
                }
                catch ( JSONException je )
=======
                    json.key("error"); //$NON-NLS-1$
                    json.value( reason );
                    json.endObject();
                    json.flush();
                }
                catch ( IOException je )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                {
                    // ignore
                }
            }
<<<<<<< HEAD
            return json.toString();
=======
            return sw.toString();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }

        // fall back to no data
        return "{}"; //$NON-NLS-1$
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    final void doAction( String action, String urlParam ) throws IOException, ServletException
    {
        RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
        Repository[] repos = admin.listRepositories();
        Repository repo = getRepository( repos, urlParam );

        String uri = repo != null ? repo.getURI() : urlParam;

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


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    final void doDeploy( String[] bundles, boolean start, boolean optional )
    {
        try
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
                if ( bundle == null || bundle.equals( "-" ) ) //$NON-NLS-1$
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

            FelixDeployer.deploy( resolver, logger, start, optional );
        }
        catch ( InvalidSyntaxException e )
        {
            throw new IllegalStateException( e );
        }
    }


    private final Repository getRepository( Repository[] repos, String repositoryUrl )
    {
        if ( repositoryUrl == null || repositoryUrl.length() == 0 )
        {
            return null;
        }

        for ( int i = 0; i < repos.length; i++ )
        {
            if ( repositoryUrl.equals( repos[i].getURI() ) )
            {
                return repos[i];
            }
        }

        return null;
    }


<<<<<<< HEAD
    private final JSONObject toJSON( Resource resource, Bundle[] bundles, boolean details ) throws JSONException
    {
        final String symbolicName = resource.getSymbolicName();
        final String version = resource.getVersion().toString();
        boolean installed = false;
        for ( int i = 0; symbolicName != null && !installed && bundles != null && i < bundles.length; i++ )
        {
            final String ver = ( String ) bundles[i].getHeaders( "" ).get( Constants.BUNDLE_VERSION ); //$NON-NLS-1$
            installed = symbolicName.equals( bundles[i].getSymbolicName() ) && version.equals( ver );
        }
        JSONObject json = new JSONObject( resource.getProperties() ) //
            .put( "id", resource.getId() ) // //$NON-NLS-1$
            .put( "presentationname", resource.getPresentationName() ) // //$NON-NLS-1$
            .put( "symbolicname", symbolicName ) // //$NON-NLS-1$
            .put( "url", resource.getURI() ) // //$NON-NLS-1$
            .put( "version", version ) // //$NON-NLS-1$
            .put( "categories", resource.getCategories() ) // //$NON-NLS-1$
            .put( "installed", installed ); //$NON-NLS-1$
=======
    private final void toJSON( JSONWriter writer, Resource resource, Bundle[] bundles, boolean details ) throws IOException
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
        writer.object();
        writer.key( "id"); //$NON-NLS-1$
        writer.value(resource.getId() );
        writer.key( "presentationname" );  //$NON-NLS-1$
        writer.value(resource.getPresentationName() );
        writer.key( "symbolicname" ); //$NON-NLS-1$
        writer.value( symbolicName );
        writer.key( "url"); //$NON-NLS-1$
        writer.value(resource.getURI() );
        writer.key( "version" );  //$NON-NLS-1$
        writer.value(version );
        writer.key( "categories" ); //$NON-NLS-1$
        writer.value(resource.getCategories() );
        writer.key( "installed" ); //$NON-NLS-1$
        writer.value(installed );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        if ( details )
        {
            Capability[] caps = resource.getCapabilities();
<<<<<<< HEAD
            for ( int i = 0; caps != null && i < caps.length; i++ )
            {
                json.append( "capabilities", new JSONObject() //$NON-NLS-1$
                    .put( "name", caps[i].getName() ) //$NON-NLS-1$
                    .put( "properties", toJSON( caps[i].getProperties() ) ) ); //$NON-NLS-1$
            }
            Requirement[] reqs = resource.getRequirements();
            for ( int i = 0; reqs != null && i < reqs.length; i++ )
            {
                json.append( "requirements", new JSONObject() //$NON-NLS-1$
                    .put( "name", reqs[i].getName() ) //$NON-NLS-1$
                    .put( "filter", reqs[i].getFilter() ) //$NON-NLS-1$
                    .put( "optional", reqs[i].isOptional() ) ); //$NON-NLS-1$
=======
            if ( caps != null )
            {
                writer.key("capabilities"); //$NON-NLS-1$
                writer.array();
                for ( int i = 0; i < caps.length; i++ )
                {
                    writer.object();
                    writer.key( "name" ); //$NON-NLS-1$
                    writer.value(caps[i].getName() );
                    writer.key( "properties" ); //$NON-NLS-1$
                    toJSON( writer, caps[i].getProperties() );
                    writer.endObject();
                }
                writer.endArray();
            }
            Requirement[] reqs = resource.getRequirements();
            if ( reqs != null )
            {
                writer.key("requirements"); //$NON-NLS-1$
                writer.array();
                for ( int i = 0; i < reqs.length; i++ )
                {
                    writer.object();
                    writer.key( "name" ); //$NON-NLS-1$
                    writer.value(reqs[i].getName() );
                    writer.key( "filter" ); //$NON-NLS-1$
                    writer.value(reqs[i].getFilter() );
                    writer.key( "optional" ); //$NON-NLS-1$
                    writer.value(reqs[i].isOptional() );
                    writer.endObject();
                }
                writer.endArray();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }

            final RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
            Resolver resolver = admin.resolver();
            resolver.add( resource );
            resolver.resolve( Resolver.NO_OPTIONAL_RESOURCES );
            Resource[] required = resolver.getRequiredResources();
<<<<<<< HEAD
            for ( int i = 0; required != null && i < required.length; i++ )
            {
                json.append( "required", toJSON( required[i], bundles, false ) ); //$NON-NLS-1$
            }
            Resource[] optional = resolver.getOptionalResources();
            for ( int i = 0; optional != null && i < optional.length; i++ )
            {
                json.append( "optional", toJSON( optional[i], bundles, false ) ); //$NON-NLS-1$
            }
            Reason[] unsatisfied = resolver.getUnsatisfiedRequirements();
            for ( int i = 0; unsatisfied != null && i < unsatisfied.length; i++ )
            {
                json.append( "unsatisfied", new JSONObject() //$NON-NLS-1$
                    .put( "name", unsatisfied[i].getRequirement().getName() ) //$NON-NLS-1$
                    .put( "filter", unsatisfied[i].getRequirement().getFilter() ) //$NON-NLS-1$
                    .put( "optional", unsatisfied[i].getRequirement().isOptional() ) ); //$NON-NLS-1$
            }
        }
        return json;
    }


    private JSONObject toJSON( final Property[] props ) throws JSONException
    {
        JSONObject json = new JSONObject();
        for ( int i = 0; props != null && i < props.length; i++ )
        {
            json.put( props[i].getName(), props[i].getValue() );
        }
        return json;
=======
            if ( required != null )
            {
                writer.key("required"); //$NON-NLS-1$
                writer.array();
                for ( int i = 0; required != null && i < required.length; i++ )
                {
                    toJSON( writer, required[i], bundles, false );
                }
                writer.endArray();
            }
            Resource[] optional = resolver.getOptionalResources();
            if ( optional != null )
            {
                writer.key("optional"); //$NON-NLS-1$
                writer.array();
                for ( int i = 0; optional != null && i < optional.length; i++ )
                {
                    toJSON( writer, optional[i], bundles, false );
                }
                writer.endArray();
            }
            Reason[] unsatisfied = resolver.getUnsatisfiedRequirements();
            if ( unsatisfied != null)
            {
                writer.key("unsatisfied"); //$NON-NLS-1$
                writer.array();
                for ( int i = 0; unsatisfied != null && i < unsatisfied.length; i++ )
                {
                    writer.object();
                    writer.key( "name" ); //$NON-NLS-1$
                    writer.value(unsatisfied[i].getRequirement().getName() );
                    writer.key( "filter" ); //$NON-NLS-1$
                    writer.value(unsatisfied[i].getRequirement().getFilter() );
                    writer.key( "optional" ); //$NON-NLS-1$
                    writer.value(unsatisfied[i].getRequirement().isOptional() );

                    writer.endObject();
                }
                writer.endArray();
            }
        }
        writer.endObject();
    }


    private void toJSON( final JSONWriter writer, final Property[] props ) throws IOException
    {
        writer.object();
        for ( int i = 0; props != null && i < props.length; i++ )
        {
            writer.key(props[i].getName());
            writer.value(props[i].getValue());
        }
        writer.endObject();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
