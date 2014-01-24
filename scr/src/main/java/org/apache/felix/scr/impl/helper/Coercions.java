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
package org.apache.felix.scr.impl.helper;

import java.lang.reflect.Array;
import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentException;

/**
 * This implements the coercion table in RFC 190 5.6.3 
 *
 */
public class Coercions
{
// Numbers are   AtomicInteger, AtomicLong, BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, Short
//annotation fields can be primitives, String, Class, enums, annotations, and arrays of the preceding types 
//    input scalars
//    String | Integer | Long | Float
//    | Double | Byte | Short
//| Character | Boolean
    private static final byte byte0 = 0;
    private static final double double0 = 0;
    private static final float float0 = 0;
    private static final int int0 = 0;
    private static final long long0 = 0;
    private static final short short0 = 0;
    
    public static byte coerceToByte(Object o)
    {
        o = multipleToSingle( o, byte0 );
        if (o instanceof Byte)
        {
            return (Byte)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Byte.parseByte( (String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Boolean)
        {
            return (Boolean)o? 1: byte0;
        }
        if (o instanceof Character)
        {
            return ( byte ) ((Character)o).charValue();
        }
        if (o instanceof Number)
        {
            return ((Number)o).byteValue();
        }
        if (o == null) 
        {
            return 0;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static double coerceToDouble(Object o)
    {
        o = multipleToSingle( o, double0 );
        if (o instanceof Double)
        {
            return (Double)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Double.parseDouble((String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Boolean)
        {
            return (Boolean)o? 1: 0;
        }
        if (o instanceof Character)
        {
            return ( double ) ((Character)o).charValue();
        }
        if (o instanceof Number)
        {
            return ((Number)o).doubleValue();
        }
        if (o == null) 
        {
            return 0;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static float coerceToFloat(Object o)
    {
        o = multipleToSingle( o, float0 );
        if (o instanceof Float)
        {
            return (Float)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Float.parseFloat((String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Boolean)
        {
            return (Boolean)o? 1: 0;
        }
        if (o instanceof Character)
        {
            return ( float ) ((Character)o).charValue();
        }
        if (o instanceof Number)
        {
            return ((Number)o).floatValue();
        }
        if (o == null) 
        {
            return 0;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static int coerceToInteger(Object o)
    {
        o = multipleToSingle( o, int0 );
        if (o instanceof Integer)
        {
            return (Integer)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Integer.parseInt( (String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Boolean)
        {
            return (Boolean)o? 1: 0;
        }
        if (o instanceof Character)
        {
            return ( int ) ((Character)o).charValue();
        }
        if (o instanceof Number)
        {
            return ((Number)o).intValue();
        }
        if (o == null) 
        {
            return 0;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static long coerceToLong(Object o)
    {
        o = multipleToSingle( o, long0 );
        if (o instanceof Long)
        {
            return (Long)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Long.parseLong( (String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Boolean)
        {
            return (Boolean)o? 1: 0;
        }
        if (o instanceof Character)
        {
            return ( long ) ((Character)o).charValue();
        }
        if (o instanceof Number)
        {
            return ((Number)o).longValue();
        }
        if (o == null) 
        {
            return 0;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static short coerceToShort(Object o)
    {
        o = multipleToSingle( o, short0 );
        if (o instanceof Short)
        {
            return (Short)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Short.parseShort( (String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Boolean)
        {
            return (Boolean)o? 1: short0;
        }
        if (o instanceof Character)
        {
            return ( short ) ((Character)o).charValue();
        }
        if (o instanceof Number)
        {
            return ((Number)o).shortValue();
        }
        if (o == null) 
        {
            return 0;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static String coerceToString(Object o)
    {
        o = multipleToSingle( o, null );
        if (o instanceof String)
        {
            return (String)o;
        }
        if (o == null)
        {
            return null;
        }
        
        return o.toString();
    }
    
    public static boolean coerceToBoolean(Object o)
    {
        o = multipleToSingle( o, false );
        if (o instanceof Boolean)
        {
            return (Boolean)o;
        }
        if (o instanceof String)
        {
            try
            {
                return Boolean.parseBoolean( (String)o );
            }
            catch ( NumberFormatException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o instanceof Character)
        {
            return ((Character)o).charValue() != 0;
        }
        if (o instanceof Number)
        {
            return ((Number)o).intValue() != 0;
        }
        if (o == null) 
        {
            return false;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static Class<?> coerceToClass(Object o, Bundle b)
    {
        o = multipleToSingle( o, null );
        if (o == null)
        {
            return null;
        }
        if (o instanceof String)
        {
            try
            {
                return b.loadClass( (String)o );
            }
            catch ( ClassNotFoundException e )
            {
                throw new ComponentException(e);
            }
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    public static <T extends Enum<T>> T coerceToEnum(Object o, Class<T> clazz)
    {
        o = multipleToSingle( o, null );
        if (o instanceof String)
        {
            try
            {
            return Enum.valueOf( clazz, (String)o );
            }
            catch ( IllegalArgumentException e )
            {
                throw new ComponentException(e);
            }
        }
        if (o == null) 
        {
            return null;
        }
        throw new ComponentException( "Unrecognized input type: " + o);
    }
    
    private static Object multipleToSingle(Object o, Object defaultValue)
    {
        if (o instanceof Collection)
        {
            return firstCollectionElement( o, defaultValue );
        }
        if (o != null && o.getClass().isArray()) {
            return firstArrayElement( o, defaultValue);
        }
        return o;
    }
    
    private static Object firstCollectionElement( Object raw, Object defaultValue )
    {
        if (!(raw instanceof Collection)) 
        {
            throw new ComponentException("Not a collection: " + raw);
        }
        Collection c = ( Collection ) raw;
        if (c.isEmpty())
        {
            return defaultValue;
        }
        return c.iterator().next();
    }

    private static Object firstArrayElement(Object o, Object defaultValue)
    {
        if (o == null || !o.getClass().isArray()) {
            throw new ComponentException("Not an array: " + o);
        }
        if (Array.getLength( o ) == 0)
        {
            return defaultValue;
        }
        return Array.get( o, 0 );
    }

}
