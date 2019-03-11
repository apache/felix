/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.util.converter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A custom converter wraps another converter by adding rules and/or error
 * handlers.
 *
 * @author $Id$
 */
class CustomConverterImpl implements InternalConverter {
	private final InternalConverter					delegate;
	final Map<Type,List<ConverterFunction>>	typeRules;
	final List<ConverterFunction>			allRules;
	final List<ConverterFunction>			errorHandlers;

	CustomConverterImpl(InternalConverter converter,
			Map<Type,List<ConverterFunction>> rules,
			List<ConverterFunction> catchAllRules,
			List<ConverterFunction> errHandlers) {
		delegate = converter;
		typeRules = rules;
		allRules = catchAllRules;
		errorHandlers = errHandlers;
	}

	@Override
	public InternalConverting convert(Object obj) {
		InternalConverting converting = delegate.convert(obj);
		return new ConvertingWrapper(obj, converting, this);
	}

	@Override
	public Functioning function() {
		return new FunctioningImpl(this);
	}

	@Override
	public ConverterBuilder newConverterBuilder() {
		return new ConverterBuilderImpl(this);
	}

	private class ConvertingWrapper implements InternalConverting {
	    private final InternalConverter     converter;
		private final InternalConverting	del;
		private final Object				object;
		private volatile Object				defaultValue;
		private volatile boolean			hasDefault;

		ConvertingWrapper(Object obj, InternalConverting c, InternalConverter conv) {
		    converter = conv;
			object = obj;
			del = c;
		}

		@Override
		public Converting view() {
			del.view();
			return this;
		}

		@Override
		public Converting defaultValue(Object defVal) {
			del.defaultValue(defVal);
			defaultValue = defVal;
			hasDefault = true;
			return this;
		}

		@Override
		public Converting keysIgnoreCase() {
			del.keysIgnoreCase();
			return this;
		}

		@Override
		public Converting sourceAs(Class< ? > type) {
			del.sourceAs(type);
			return this;
		}

		@Override
		public Converting sourceAsBean() {
			del.sourceAsBean();
			return this;
		}

		@Override
		public Converting sourceAsDTO() {
			del.sourceAsDTO();
			return this;
		}

		@Override
		public Converting targetAs(Class< ? > cls) {
			del.targetAs(cls);
			return this;
		}

		@Override
		public Converting targetAsBean() {
			del.targetAsBean();
			return this;
		}

		@Override
		public Converting targetAsDTO() {
			del.targetAsDTO();
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T to(Class<T> cls) {
			Type type = cls;
			return (T) to(type);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T to(TypeReference<T> ref) {
			return (T) to(ref.getType());
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object to(Type type) {
		    return to(type, converter);
		}

        @SuppressWarnings("unchecked")
        @Override
        public Object to(Type type, InternalConverter c) {
			List<ConverterFunction> tr = typeRules.get(Util.baseType(type));
			if (tr == null)
				tr = Collections.emptyList();
			List<ConverterFunction> converters = new ArrayList<>(
					tr.size() + allRules.size());
			converters.addAll(tr);
			converters.addAll(allRules);

			try {
				if (object != null) {
					for (ConverterFunction cf : converters) {
						try {
							Object res = cf.apply(object, type);
							if (res != ConverterFunction.CANNOT_HANDLE) {
								return res;
							}
						} catch (Exception ex) {
							if (hasDefault)
								return defaultValue;
							else
								throw new ConversionException("Cannot convert "
										+ object + " to " + type, ex);
						}
					}
				}

				Object result = del.to(type, c);
				if (result != null && Proxy.isProxyClass(result.getClass()) && getErrorHandlers(c).size() > 0) {
				    return wrapErrorHandling(result, c);
				} else {
				    return result;
				}
			} catch (Exception ex) {
				for (ConverterFunction eh : getErrorHandlers(c)) {
					try {
						Object handled = eh.apply(object, type);
						if (handled != ConverterFunction.CANNOT_HANDLE)
							return handled;
					} catch (RuntimeException re) {
						throw re;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				// No error handler, throw the original exception
				throw ex;
			}
		}

        private List<ConverterFunction> getErrorHandlers(Converter converter) {
            List<ConverterFunction> handlers = new ArrayList<>();

            if (converter instanceof CustomConverterImpl) {
                CustomConverterImpl cconverter = (CustomConverterImpl) converter;
                handlers.addAll(cconverter.errorHandlers);

                Converter nextDel = cconverter.delegate;
                handlers.addAll(getErrorHandlers(nextDel));
            }

            handlers.addAll(errorHandlers);

            return handlers;
        }

		private Object wrapErrorHandling(final Object wrapped, final InternalConverter c) {
		    final Class<?> cls = wrapped.getClass();
		    return Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Class< ? > mdDecl = method.getDeclaringClass();
                    if (mdDecl.equals(Object.class)) {
                        switch (method.getName()) {
                            case "equals" :
                                return proxy == args[0];
                            case "hashCode" :
                                return System.identityHashCode(proxy);
                            case "toString" :
                                return "Proxy for " + cls;
                            default :
                                throw new UnsupportedOperationException("Method "
                                        + method + " not supported on proxy for "
                                        + cls);
                        }
                    }

                    try {
                        return method.invoke(wrapped, args);
                    } catch (Exception ex) {
                        for (ConverterFunction eh : getErrorHandlers(c)) {
                            try {
                                Object handled = eh.apply(wrapped, method.getGenericReturnType());
                                if (handled != ConverterFunction.CANNOT_HANDLE)
                                    return handled;
                            } catch (RuntimeException re) {
                                throw re;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    return null;
                }
            });
        }

        @Override
		public String toString() {
			return to(String.class);
		}
	}
}
