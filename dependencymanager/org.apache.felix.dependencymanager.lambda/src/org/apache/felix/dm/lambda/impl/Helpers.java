package org.apache.felix.dm.lambda.impl;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.lambda.callbacks.SerializableLambda;

/**
 * Various helper methods related to generics and lambda expressions.
 */
public class Helpers {
	private final static Pattern LAMBDA_INSTANCE_METHOD_TYPE = Pattern.compile("(L[^;]+)+");
	private final static String DEFAULT_DEPENDENCY_MODE = "org.apache.felix.dependencymanager.lambda.dependencymode";
	private final static String REQUIRED = "required";
	private final static String OPTIONAL = "optional";

	/**
	 * Tests if a dependency is required by default.
	 * By default, a dependency is required, but you can configure the mode of a dependency, by configure the
	 * DEFAULT_DEPENDENCY_MODE property in the bundle context properties. This property can take as value:
	 * <ul><li>"required" meaning that dependencies are required by default,
	 * <li>"optional" meaning that dependencies are optional by default.
	 * </ul>
	 * By default, a dependency is required.
	 */
	static boolean isDependencyRequiredByDefault(Component component) {
	    String property = ((ComponentContext) component).getBundleContext().getProperty(DEFAULT_DEPENDENCY_MODE);
	    if (REQUIRED.equalsIgnoreCase(property)) {
	        return true;
	    } else if (OPTIONAL.equalsIgnoreCase(property)) {
	        return false;
	    } else {
	        return true;
	    }
	}
	
	/**
	 * Gets the class name of a given object.
	 * @param obj the object whose class has to be returned.
	 */
	public static Class<?> getClass(Object obj) {
		Class<?> clazz = obj.getClass();
		if (Proxy.isProxyClass(clazz)) {
			return Proxy.getProxyClass(clazz.getClassLoader(), clazz);
		}
		return clazz;
	}
	
	/**
	 * Extracts the type of a given generic lambda parameter.
	 * Example: for "BiConsumer<String, Integer>", and with genericParamIndex=0, this method returns java.lang.String class.
	 * 
	 * @param lambda a lambda expression, which must extends @link {@link SerializableLambda} interface.
	 * @param genericParamIndex the index of a given lambda generic parameter.
	 * @return the type of the lambda generic parameter that corresponds to the <code>genericParamIndex</code>  
	 */
	@SuppressWarnings("unchecked")
    public static <T> Class<T> getLambdaArgType(SerializableLambda lambda, int genericParamIndex) {
	    String[] lambdaParams = getGenericTypeStrings(lambda);
	    Class<?> clazz;
        try {
            clazz = lambda.getClass().getClassLoader().loadClass(lambdaParams[genericParamIndex]);
        } catch (ClassNotFoundException e) {
           throw new RuntimeException("Can't load class " + lambdaParams[genericParamIndex]);
        }
	    return (Class<T>) clazz;
	}
	
	/**
	 * Extracts the first parameter of a lambda.
	 */
	public static String getLambdaParameterName(SerializableLambda lambda, int index) {
		SerializedLambda serialized = getSerializedLambda(lambda);
		Method m = getLambdaMethod(serialized, lambda.getClass().getClassLoader());
		Parameter p = m.getParameters()[index];
		
        if (Objects.equals("arg0", p.getName())) {
            throw new IllegalStateException("Can'f find lambda method name (Please check you are using javac -parameters option).");
        }
        return p.getName();
	}
	
	/**
	 * Returns the SerializedObject of a given lambda.
	 */
    private static SerializedLambda getSerializedLambda(SerializableLambda lambda) {
	    if (lambda == null) {
	        throw new IllegalArgumentException();
	    }

	    for (Class<?> clazz = lambda.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
	        try {
	            Method replaceMethod = clazz.getDeclaredMethod("writeReplace");
	            replaceMethod.setAccessible(true);
	            Object serializedForm = replaceMethod.invoke(lambda);

	            if (serializedForm instanceof SerializedLambda) {
	                return (SerializedLambda) serializedForm;
	            }
	        }
	        catch (NoSuchMethodException e) {
	            // fall through the loop and try the next class
	        }
	        catch (Throwable t) {
	            throw new RuntimeException("Error while extracting serialized lambda", t);
	        }
	    }

	    throw new RuntimeException("writeReplace method not found");
	}

    /**
     * Finds a composite
     * @param component
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <U> U findCompositeInstance(Component component, Class<U> type) {
        U instance = (U) Stream.of(component.getInstances())
            .filter(inst -> Objects.equals(Helpers.getClass(inst), type))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Did not find a component instance matching type " + type));
        return instance;                       
    }

    /**
     * Extracts the actual types of all lambda generic parameters.
     * Example: for "BiConsumer<String, Integer>", this method returns ["java.lang.String", "java.lang.Integer"].
     */
    private static String[] getGenericTypeStrings(SerializableLambda lambda) {
        // The only portable way to get the actual lambda generic parameters can be done using SerializedLambda.
        SerializedLambda sl = getSerializedLambda(lambda);
        String lambdaMethodType = sl.getInstantiatedMethodType();
        Matcher m = LAMBDA_INSTANCE_METHOD_TYPE.matcher(lambdaMethodType);
        List<String> results = new ArrayList<>();
        while (m.find()) {
            results.add(m.group().substring(1).replace("/", "."));
        }
        return results.toArray(new String[0]);
    }
    
    /**
     * Extracts the actual java method from a given lambda.
     */
    private static Method getLambdaMethod(SerializedLambda lambda, ClassLoader loader) {
        String implClassName = lambda.getImplClass().replace('/', '.');
        Class<?> implClass;
        try {
            implClass = loader.loadClass(implClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Lambda Method not found (can not instantiate class " + implClassName);
        }

        return Stream.of(implClass.getDeclaredMethods())
            .filter(method -> Objects.equals(method.getName(), lambda.getImplMethodName()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Lambda Method not found"));
    }
    
}
