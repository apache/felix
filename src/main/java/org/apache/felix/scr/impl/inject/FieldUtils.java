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
package org.apache.felix.scr.impl.inject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.service.log.LogService;

public class FieldUtils {

    /**
     * Finds the field named in the {@link #fieldName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable method
     * the class hierarchy is traversed until a field is found or the root
     * of the class hierarchy is reached without finding a field.
     *
     * @return The requested field or <code>null</code> if no acceptable field
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested field.
     * @param logger
     */
    public static Field findField( final Class<?> targetClass,
    		final String fieldName,
    		final SimpleLogger logger )
    throws InvocationTargetException
    {
        final ClassLoader targetClasslLoader = targetClass.getClassLoader();
        final String targetPackage = ClassUtils.getPackageName( targetClass );
        Class<?> theClass = targetClass;
        boolean acceptPrivate = true;
        boolean acceptPackage = true;
        while (true)
        {

            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG,
                    "Locating field " + fieldName + " in class " + theClass.getName(), null );
            }

            try
            {
                final Field field = getField( theClass, fieldName, acceptPrivate, acceptPackage, logger );
                if ( field != null )
                {
                    return field;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                // log and return null
                logger.log( LogService.LOG_ERROR,
                    "findField: Suitable but non-accessible field {0} found in class {1}, subclass of {2}", new Object[]
                        { fieldName, theClass.getName(), targetClass.getName() }, null );
                break;
            }

            // if we get here, we have no field, so check the super class
            theClass = theClass.getSuperclass();
            if ( theClass == null )
            {
                break;
            }

            // super class field check ignores private fields and accepts
            // package fields only if in the same package and package
            // fields are (still) allowed
            acceptPackage &= targetClasslLoader == theClass.getClassLoader()
                && targetPackage.equals( ClassUtils.getPackageName( theClass ) );

            // private fields will not be accepted any more in super classes
            acceptPrivate = false;
        }

        // nothing found after all these years ...
        return null;
    }

    /**
     * Finds the field named in the {@link #fieldName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable field
     * the class hierarchy is traversed until a field is found or the root
     * of the class hierarchy is reached without finding a field.
     *
     *
     * @param targetClass The class in which to look for the method
     * @param acceptPrivate <code>true</code> if private fields should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private fields should
     *      be considered.
     * @param logger
     * @return The requested field or <code>null</code> if no acceptable field
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested field.
     */
    private static Field getField( final Class<?> clazz,
    		final String fieldName,
            final boolean acceptPrivate,
            final boolean acceptPackage,
            final SimpleLogger logger )
    throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        try
        {
            // find the declared field in this class
            final Field field = clazz.getDeclaredField( fieldName );

            // accept public and protected fields only and ensure accessibility
            if ( accept( field, acceptPrivate, acceptPackage ) )
            {
                return field;
            }

            // the method would fit the requirements but is not acceptable
            throw new SuitableMethodNotAccessibleException();
        }
        catch ( NoSuchFieldException nsfe )
        {
            // thrown if no field is declared with the given name and
            // parameters
            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG, "Declared Field {0}.{1} not found", new Object[]
                    { clazz.getName(), fieldName }, null );
            }
        }
        catch ( NoClassDefFoundError cdfe )
        {
            // may be thrown if a method would be found but the signature
            // contains throws declaration for an exception which cannot
            // be loaded
            if ( logger.isLogEnabled( LogService.LOG_WARNING ) )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "Failure loooking up field " ).append( fieldName );
                buf.append( " in class class " ).append( clazz.getName() ).append( ". Assuming no such field." );
                logger.log( LogService.LOG_WARNING, buf.toString(), cdfe );
            }
        }
        catch ( SuitableMethodNotAccessibleException e)
        {
            throw e;
        }
        catch ( Throwable throwable )
        {
            // unexpected problem accessing the field, don't let everything
            // blow up in this situation, just throw a declared exception
            throw new InvocationTargetException( throwable, "Unexpected problem trying to get field " + fieldName );
        }

        // caught and ignored exception, assume no field and continue search
        return null;
    }

    /**
     * Returns <code>true</code> if the field is acceptable to be returned from the
     * {@link #getField(Class, String, boolean, boolean, SimpleLogger)} and also
     * makes the field accessible.
     * <p>
     * This method returns <code>true</code> if the field:
     * <ul>
     * <li>Is not static</li>
     * <li>Is public or protected</li>
     * <li>Is private and <code>acceptPrivate</code> is <code>true</code></li>
     * <li>Is package private and <code>acceptPackage</code> is <code>true</code></li>
     * </ul>
     * <p>
     *
     * @param field The field to check
     * @param acceptPrivate Whether a private field is acceptable
     * @param acceptPackage Whether a package private field is acceptable
     * @return whether the field is acceptable
     */
    private static boolean accept( final Field field,
            final boolean acceptPrivate,
            final boolean acceptPackage )
    {
        // check modifiers now
        final int mod = field.getModifiers();

        // no static fields
        if ( Modifier.isStatic( mod ) )
        {
            return true;
        }

        // accept public and protected fields
        if ( Modifier.isPublic( mod ) || Modifier.isProtected( mod ) )
        {
            setAccessible( field );
            return true;
        }

        // accept private if accepted
        if ( Modifier.isPrivate( mod ) )
        {
            if ( acceptPrivate )
            {
                setAccessible( field );
                return true;
            }

            return false;
        }

        // accept default (package)
        if ( acceptPackage )
        {
            setAccessible( field );
            return true;
        }

        // else don't accept
        return false;
    }

    private static void setAccessible(final Field field)
    {
        AccessController.doPrivileged( new PrivilegedAction<Object>()
        {
            public Object run()
            {
                field.setAccessible( true );
                return null;
            }
        } );
    }
}
