package org.apache.felix.dm.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * Class copied from DM core (but, ultimately, the core should export this class so we could then 
 * import/reuse it).
 */
public class InvocationUtil
{
    public static Object invokeCallbackMethod(Object instance, String methodName, Class[][] signatures,
                                            Object[][] parameters) 
        throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        Class currentClazz = instance.getClass();
        while (currentClazz != null)
        {
            try
            {
                return invokeMethod(instance, currentClazz, methodName, signatures, parameters, false);
            }
            catch (NoSuchMethodException nsme)
            {
                // ignore
            }
            currentClazz = currentClazz.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }

    public static Object invokeMethod(Object object, Class clazz, String name, Class[][] signatures,
                                      Object[][] parameters, boolean isSuper) 
        throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException, IllegalAccessException
    {
        if (object == null)
        {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        if (clazz == null)
        {
            throw new IllegalArgumentException("Class cannot be null");
        }

        // If we're talking to a proxy here, dig one level deeper to expose the
        // underlying invocation handler ...
        
        if (Proxy.isProxyClass(clazz))
        {
            object = Proxy.getInvocationHandler(object);
            clazz = object.getClass();
        }

        Method m = null;
        for (int i = 0; i < signatures.length; i++)
        {
            Class[] signature = signatures[i];
            try
            {
                m = clazz.getDeclaredMethod(name, signature);
                if (!(isSuper && Modifier.isPrivate(m.getModifiers())))
                {
                    m.setAccessible(true);
                    return m.invoke(object, parameters[i]);
                }
            }
            catch (NoSuchMethodException e)
            {
                // ignore this and keep looking
            }
        }
        throw new NoSuchMethodException(name);
    }
}
