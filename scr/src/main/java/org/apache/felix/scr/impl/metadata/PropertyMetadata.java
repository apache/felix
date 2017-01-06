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
package org.apache.felix.scr.impl.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A property descriptor that contains the information for properties
 * defined in the descriptor
 *
 */
public class PropertyMetadata {

	// Name of the property (required)
	private String m_name;

	// Type of the property (optional)
	private String m_type;

	// Value of the type (optional)
	// - before validate: raw value from XML (String or String[])
	// - after validate: converted value provided to component
	private Object m_value;

	// Flag that indicates if this PropertyMetadata has been validated and thus has become immutable
	private boolean m_validated = false;

	/**
	 * Set the name
	 *
	 * @param name
	 */
	public void setName(String name) {
		if (m_validated == true) {
			return;
		}

		m_name = name;
	}


	/**
	 * Set the type
	 *
	 * @param type
	 */
	public void setType(String type) {
		if (m_validated == true) {
			return;
		}
		m_type = type;
	}

	/**
	 * Set the value
	 *
	 * @param value
	 */
	public void setValue(String value) {
		if (m_validated == true) {
			return;
		}
        m_value = value;
	}

    /**
     * Set multiple values as an array, where the values are contained in
     * the string as one value per line.
     *
     * @param values
     */
    public void setValues(String values) {
        if (m_validated == true) {
            return;
        }
        // splite th values
        List<String> valueList = new ArrayList<String>();
        StringTokenizer tokener = new StringTokenizer(values, "\r\n");
        while (tokener.hasMoreTokens()) {
            String value = tokener.nextToken().trim();
            if (value.length() > 0) {
                valueList.add( value );
            }
        }
        m_value = valueList.toArray( new String[valueList.size()] );
    }

    /**
     * Get the name of the property
     *
     * @return the name of the property
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the type of the property
     *
     * @return the type of the property
     */
    public String getType() {
        return m_type;
    }

    /**
     * Get the value of the property
     *
     * @return the value of the property as an Object
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Method used to verify if the semantics of this metadata are correct
     */
    public void validate( ComponentMetadata componentMetadata )
    {
        if ( m_name == null )
        {
            throw componentMetadata.validationFailure( "Property name attribute is mandatory" );
        }

        // check character type name
        if ( m_type == null )
        {
            m_type = "String";
        }
        else if ( componentMetadata.getDSVersion().isDS11() && m_type.equals( "Char" ) )
        {
            throw componentMetadata
                .validationFailure( "Illegal property type 'Char' used for DS 1.1 descriptor, use 'Character' instead" );
        }
        else if ( !componentMetadata.getDSVersion().isDS11() && m_type.equals( "Character" ) )
        {
            throw componentMetadata
                .validationFailure( "Illegal property type 'Character' used for DS 1.0 descriptor, use 'Char' instead" );
        }

        // validate and covert value
        if ( m_value != null )
        {
            try
            {
                if ( m_value instanceof String )
                {
                    m_value = toType( ( String ) m_value );
                }
                else
                {
                    m_value = toTypeArray( ( String[] ) m_value );
                }
            }
            catch ( NumberFormatException nfe )
            {
                throw componentMetadata.validationFailure( getName() + ": Cannot convert property value to "
                    + getType() );
            }
            catch ( IllegalArgumentException e )
            {
                throw componentMetadata.validationFailure( getName() + ": " + e.getMessage() );
            }
        }

        m_validated = true;
    }


    /**
     * @throws IllegalArgumentException if the property type is not valid
     *          according to the spec
     * @throws NumberFormatException if the string value cannot be converted
     *          to the numeric type indicated by the property type
     */
    private Object toType( String value )
    {
        // 112.4.5 Parsing of the value is done by the valueOf(String) method (P. 291)
        // Should the type accept lowercase too?
        if ( m_type.equals( "String" ) )
        {
            return value;
        }
        else if ( m_type.equals( "Long" ) )
        {
            return Long.valueOf( value );
        }
        else if ( m_type.equals( "Double" ) )
        {
            return Double.valueOf( value );
        }
        else if ( m_type.equals( "Float" ) )
        {
            return Float.valueOf( value );
        }
        else if ( m_type.equals( "Integer" ) )
        {
            return Integer.valueOf( value );
        }
        else if ( m_type.equals( "Byte" ) )
        {
            return Byte.valueOf( value );
        }
        else if ( m_type.equals( "Char" ) || m_type.equals( "Character" ) )
        {
            // DS 1.1 changes the "Char" type to "Character", here we support both
            // For Character types, the conversion is handled by Integer.valueOf method.
            // (since valueOf is defined in terms of parseInt we directly call
            // parseInt to prevent unneeded Object creation)
            return Character.valueOf( ( char ) Integer.parseInt( value ) );
        }
        else if ( m_type.equals( "Boolean" ) )
        {
            return Boolean.valueOf( value );
        }
        else if ( m_type.equals( "Short" ) )
        {
            return Short.valueOf( value );
        }
        else
        {
            throw new IllegalArgumentException( "Undefined property type '" + m_type + "'" );
        }
    }


    /**
     * @throws IllegalArgumentException if the property type is not valid
     *          according to the spec
     * @throws NumberFormatException if the string value cannot be converted
     *          to the numeric type indicated by the property type
     */
    private Object toTypeArray( String[] valueList )
    {
        // 112.4.5 Except for String objects, the result will be translated to an array of primitive types.
        if ( m_type.equals( "String" ) )
        {
            return valueList;
        }
        else if ( m_type.equals( "Double" ) )
        {
            double[] array = new double[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Double.parseDouble( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Float" ) )
        {
            float[] array = new float[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Float.parseFloat( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Long" ) )
        {
            long[] array = new long[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Long.parseLong( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Integer" ) )
        {
            int[] array = new int[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Integer.parseInt( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Short" ) )
        {
            short[] array = new short[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Short.parseShort( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Byte" ) )
        {
            byte[] array = new byte[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Byte.parseByte( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Char" ) || m_type.equals( "Character" ) )
        {
            // DS 1.1 changes the "Char" type to "Character", here we support both
            char[] array = new char[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = ( char ) Integer.parseInt( valueList[i] );
            }
            return array;
        }
        else if ( m_type.equals( "Boolean" ) )
        {
            boolean[] array = new boolean[valueList.length];
            for ( int i = 0; i < array.length; i++ )
            {
                array[i] = Boolean.valueOf( valueList[i] );
            }
            return array;
        }
        else
        {
            throw new IllegalArgumentException( "Undefined property type '" + m_type + "'" );
        }
    }
}
