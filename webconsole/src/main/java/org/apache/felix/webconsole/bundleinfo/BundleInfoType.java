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
package org.apache.felix.webconsole.bundleinfo;


import java.net.URL;


/**
 * This pre-java 5 enum defines all valid bundle information value types.
<<<<<<< HEAD
 * 
=======
 *
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
 * @author Valentin Valchev
 */
public final class BundleInfoType
{

    /**
     * Specifies that the value is {@link String} and is either a link to a
     * local Servlet, or link to external HTTP server. In case the link starts
     * with <code>&lt;protocol&gt;://</code> the link will be considered as
     * external. Otherwise the link should be absolute link to a local Servlet
     * and must always start with <code>/</code>.
<<<<<<< HEAD
     * 
=======
     *
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * for security reasons, the protocol cannot be <code>file</code> for
     * external links.
     */
    public static final BundleInfoType LINK = new BundleInfoType( "link" ); //$NON-NLS-1$
    /**
     * This information type, specifies that the value of the information is URL
     * object, that points to a resource. In that case the UI could consider
     * that as a <em>download</em> link.
     */
    public static final BundleInfoType RESOURCE = new BundleInfoType( "resource" ); //$NON-NLS-1$
    /**
     * That information type is for normal information keys, that provide a
     * normal (not link) value as information. The type of the value is
     * <code>Object</code> and UI will visualize it by using it's
     * {@link Object#toString()} method.
     */
    public static final BundleInfoType VALUE = new BundleInfoType( "value" ); //$NON-NLS-1$

    private final String name;


    private BundleInfoType( String name )
    {
        /* prevent instantiation */
        this.name = name;
    }


    /**
     * Returns the name of the type.
<<<<<<< HEAD
     * 
=======
     *
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * @return the type name
     */
    public final String getName()
    {
        return name;
    }


    /**
     * That method is used to validate if the object is correct for the
     * specified type.
<<<<<<< HEAD
     * 
=======
     *
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * @param value
     *            the value that will be validated.
     */
    public final void validate( final Object value )
    {
        if ( this == LINK )
        {
            if ( !( value instanceof String ) )
                throw new IllegalArgumentException( "Not a String" );
            final String val = ( String ) value;
            final int idx = val.indexOf( "://" ); //$NON-NLS-1$
            // check local
            if ( idx == -1 )
            {
                if ( !val.startsWith( "/" ) ) //$NON-NLS-1$
<<<<<<< HEAD
                    throw new IllegalArgumentException( "Invalid local link" );
=======
                    throw new IllegalArgumentException( "Invalid local link: " + val );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
            else
            {
                // check external link
                if ( val.substring( 0, idx ).equalsIgnoreCase( "file" ) ) //$NON-NLS-1$
<<<<<<< HEAD
                    throw new IllegalArgumentException( "External link cannot use file protocol" );
=======
                    throw new IllegalArgumentException( "External link cannot use file protocol: " + value );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        else if ( this == RESOURCE )
        {
            if ( !( value instanceof URL ) )
<<<<<<< HEAD
                throw new IllegalArgumentException( "Invalid URL" );
=======
                throw new IllegalArgumentException( "Invalid URL: " + value );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
    }

}
