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
package org.apache.felix.webconsole.internal.configuration;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.AttributeDefinition;


/**
 * The <code>ConfigManagerBase</code> is the base class for the
 * ConfigurationAdmin support in the web console. It provides various helper
 * methods mostly with respect to using the MetaTypeService to access
 * configuration descriptions.
 */
class MetaTypeSupport
{

    /**
     * Attribute type code for PASSWORD attributes as defined in
     * Metatype Service Specification 1.2. Since we cannot yet refer
     * to the 1.2 API package we just replicate the type code here. Once
     * the API is available and can be referred to, we should use it.
     */
    static final int ATTRIBUTE_TYPE_PASSWORD = 12;

    /**
     * Marker value of password fields used as dummy values and
     * indicating unmodified values.
     */
    static final String PASSWORD_PLACEHOLDER_VALUE = "unmodified"; //$NON-NLS-1$


    static Bundle getBundle( final BundleContext bundleContext, final String bundleLocation )
    {
        if ( bundleLocation == null )
        {
            return null;
        }

        Bundle[] bundles = bundleContext.getBundles();
        for ( int i = 0; i < bundles.length; i++ )
        {
            if ( bundleLocation.equals( bundles[i].getLocation() ) )
            {
                return bundles[i];
            }
        }

        return null;
    }


    static void attributeToJson( final JSONWriter json, final PropertyDescriptor ad, final Object propValue )
        throws JSONException
    {
        json.object();

        Object value;
        if ( propValue != null )
        {
            value = propValue;
        }
        else if ( ad.getDefaultValue() != null )
        {
            value = ad.getDefaultValue();
        }
        else if ( ad.getCardinality() == 0 )
        {
            value = ""; //$NON-NLS-1$
        }
        else
        {
            value = new String[0];
        }

        json.key( "name" ); //$NON-NLS-1$
        json.value( ad.getName() );
        json.key( "optional" ); //$NON-NLS-1$
        json.value( ad.isOptional() );
        json.key( "is_set" ); //$NON-NLS-1$
        json.value( propValue != null );

        // attribute type - overwrite metatype provided type
        // if the property name contains "password" and the
        // type is string
        int propertyType = getAttributeType( ad );

        json.key( "type" ); //$NON-NLS-1$
        if ( ad.getOptionLabels() != null && ad.getOptionLabels().length > 0 )
        {
            json.object();
            json.key( "labels" ); //$NON-NLS-1$
            json.value( Arrays.asList( ad.getOptionLabels() ) );
            json.key( "values" ); //$NON-NLS-1$
            json.value( Arrays.asList( ad.getOptionValues() ) );
            json.endObject();
        }
        else
        {
            json.value( propertyType );
        }

        // unless the property is of password type, send it
        final boolean isPassword = propertyType == ATTRIBUTE_TYPE_PASSWORD;
        if ( ad.getCardinality() == 0 )
        {
            // scalar
            if ( isPassword )
            {
                value = PASSWORD_PLACEHOLDER_VALUE;
            }
            else if ( value instanceof Vector )
            {
                value = ( ( Vector ) value ).get( 0 );
            }
            else if ( value.getClass().isArray() )
            {
                value = Array.get( value, 0 );
            }
            json.key( "value" ); //$NON-NLS-1$
            json.value( value );
        }
        else
        {
            value = new JSONArray( toList( value ) );
            if ( isPassword )
            {
                JSONArray tmp = ( JSONArray ) value;
                for ( int tmpI = 0; tmpI < tmp.length(); tmpI++ )
                {
                    tmp.put( tmpI, PASSWORD_PLACEHOLDER_VALUE );
                }
            }
            json.key( "values" ); //$NON-NLS-1$
            json.value( value );
        }

        if ( ad.getDescription() != null )
        {
            json.key( "description" ); //$NON-NLS-1$
            json.value( ad.getDescription() + " (" + ad.getID() + ")" ); //$NON-NLS-1$ //$NON-NLS-2$
        }

        json.endObject();
    }


    private static boolean isPasswordProperty( String name )
    {
        return name == null ? false : name.toLowerCase().indexOf( "password" ) != -1; //$NON-NLS-1$
    }


    private static List toList( Object value )
    {
        if ( value instanceof Vector )
        {
            return ( Vector ) value;
        }
        else if ( value.getClass().isArray() )
        {
            if ( value.getClass().getComponentType().isPrimitive() )
            {
                final int len = Array.getLength( value );
                final Object[] tmp = new Object[len];
                for ( int j = 0; j < len; j++ )
                {
                    tmp[j] = Array.get( value, j );
                }
                value = tmp;
            }
            return Arrays.asList( ( Object[] ) value );
        }
        else
        {
            return Collections.singletonList( value );
        }
    }


    static PropertyDescriptor createAttributeDefinition( final String id, final Object value )
    {
        int attrType;
        int attrCardinality;
        Class type;

        if ( value == null )
        {
            attrCardinality = 0;
            type = String.class;
        }
        else if ( value instanceof Collection )
        {
            attrCardinality = Integer.MIN_VALUE;
            Collection coll = ( Collection ) value;
            if ( coll.isEmpty() )
            {
                type = String.class;
            }
            else
            {
                type = coll.iterator().next().getClass();
            }
        }
        else if ( value.getClass().isArray() )
        {
            attrCardinality = Integer.MAX_VALUE;
            type = value.getClass().getComponentType();
        }
        else
        {
            attrCardinality = 0;
            type = value.getClass();
        }

        if ( type == Boolean.class || type == Boolean.TYPE )
        {
            attrType = AttributeDefinition.BOOLEAN;
        }
        else if ( type == Byte.class || type == Byte.TYPE )
        {
            attrType = AttributeDefinition.BYTE;
        }
        else if ( type == Character.class || type == Character.TYPE )
        {
            attrType = AttributeDefinition.CHARACTER;
        }
        else if ( type == Double.class || type == Double.TYPE )
        {
            attrType = AttributeDefinition.DOUBLE;
        }
        else if ( type == Float.class || type == Float.TYPE )
        {
            attrType = AttributeDefinition.FLOAT;
        }
        else if ( type == Long.class || type == Long.TYPE )
        {
            attrType = AttributeDefinition.LONG;
        }
        else if ( type == Integer.class || type == Integer.TYPE )
        {
            attrType = AttributeDefinition.INTEGER;
        }
        else if ( type == Short.class || type == Short.TYPE )
        {
            attrType = AttributeDefinition.SHORT;
        }
        else
        {
            attrType = AttributeDefinition.STRING;
        }

        return new PropertyDescriptor( id, attrType, attrCardinality );
    }


    static int getAttributeType( final PropertyDescriptor ad )
    {
        if ( ad.getType() == AttributeDefinition.STRING && isPasswordProperty( ad.getID() ) )
        {
            return ATTRIBUTE_TYPE_PASSWORD;
        }
        return ad.getType();
    }


    /**
     * @throws NumberFormatException If the value cannot be converted to
     *      a number and type indicates a numeric type
     */
    static final Object toType( int type, String value )
    {
        switch ( type )
        {
            case AttributeDefinition.BOOLEAN:
                return Boolean.valueOf( value );
            case AttributeDefinition.BYTE:
                return Byte.valueOf( value );
            case AttributeDefinition.CHARACTER:
                char c = ( value.length() > 0 ) ? value.charAt( 0 ) : 0;
                return new Character( c );
            case AttributeDefinition.DOUBLE:
                return Double.valueOf( value );
            case AttributeDefinition.FLOAT:
                return Float.valueOf( value );
            case AttributeDefinition.LONG:
                return Long.valueOf( value );
            case AttributeDefinition.INTEGER:
                return Integer.valueOf( value );
            case AttributeDefinition.SHORT:
                return Short.valueOf( value );
            default:
                // includes AttributeDefinition.STRING
                // includes ATTRIBUTE_TYPE_PASSWORD/AttributeDefinition.PASSWORD
                return value;
        }
    }


    static void setPasswordProps( final Vector vec, final String[] properties, Object props )
    {
        List propList = ( props == null ) ? new ArrayList() : toList( props );
        for ( int i = 0; i < properties.length; i++ )
        {
            if ( PASSWORD_PLACEHOLDER_VALUE.equals( properties[i] ) )
            {
                if ( i < propList.size() && propList.get( i ) != null )
                {
                    vec.add( propList.get( i ) );
                }
            }
            else
            {
                vec.add( properties[i] );
            }
        }
    }


    static final Object toArray( int type, Vector values )
    {
        int size = values.size();

        // short cut for string array
        if ( type == AttributeDefinition.STRING || type == ATTRIBUTE_TYPE_PASSWORD )
        {
            return values.toArray( new String[size] );
        }

        Object array;
        switch ( type )
        {
            case AttributeDefinition.BOOLEAN:
                array = new boolean[size];
                break;
            case AttributeDefinition.BYTE:
                array = new byte[size];
                break;
            case AttributeDefinition.CHARACTER:
                array = new char[size];
                break;
            case AttributeDefinition.DOUBLE:
                array = new double[size];
                break;
            case AttributeDefinition.FLOAT:
                array = new float[size];
                break;
            case AttributeDefinition.LONG:
                array = new long[size];
                break;
            case AttributeDefinition.INTEGER:
                array = new int[size];
                break;
            case AttributeDefinition.SHORT:
                array = new short[size];
                break;
            default:
                // unexpected, but assume string
                array = new String[size];
        }

        for ( int i = 0; i < size; i++ )
        {
            Array.set( array, i, values.get( i ) );
        }

        return array;
    }

}
