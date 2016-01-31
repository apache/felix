package org.apache.felix.dm.lambda.impl;

import java.util.Dictionary;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.felix.dm.lambda.BundleDependencyBuilder;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.ConfigurationDependencyBuilder;
import org.apache.felix.dm.lambda.FluentProperties;
import org.apache.felix.dm.lambda.FutureDependencyBuilder;
import org.apache.felix.dm.lambda.ServiceDependencyBuilder;
import org.apache.felix.dm.lambda.callbacks.CbComponent;
import org.apache.felix.dm.lambda.callbacks.CbConsumer;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponent;

/**
 * Methods common to extended components like adapters or aspects.
 * 
 * TODO javadoc
 */
@SuppressWarnings({"unchecked"})
public interface AdapterBase<B extends ComponentBuilder<B>> extends ComponentBuilder<B> {
	
    void andThenBuild(Consumer<ComponentBuilder<?>> builder);
    
    default B impl(Object impl) {
        andThenBuild(compBuilder -> compBuilder.impl(impl));
        return (B) this;
    }
    
    default <U> B impl(Class<U> implClass) {        
        andThenBuild(compBuilder -> compBuilder.impl(implClass));
        return (B) this;
    }

    default B factory(Object factory, String createMethod) {
        andThenBuild(compBuilder -> compBuilder.factory(factory, createMethod));
        return (B) this;
    }

    default B factory(Supplier<?> create) {        
        andThenBuild(compBuilder -> compBuilder.factory(create));
        return (B) this;
    }
    
    default <U, V> B factory(Supplier<U> factory, Function<U, V> create) {        
        andThenBuild(compBuilder -> compBuilder.factory(factory, create));
        return (B) this;
    }
        
    default B factory(Supplier<?> factory, Supplier<Object[]> getComposition) {        
        andThenBuild(compBuilder -> compBuilder.factory(factory, getComposition));
        return (B) this;
    }

    default <U> B factory(Supplier<U> factory, Function<U, ?> create, Function<U, Object[]> getComposition) {       
        andThenBuild(compBuilder -> compBuilder.factory(factory, create, getComposition));
        return (B) this;
    }

    default B provides(Class<?>  iface) {
        andThenBuild(compBuilder -> compBuilder.provides(iface));
        return (B) this;
    }
    
    default B provides(Class<?>  iface, String name, Object value, Object ... rest) {
        andThenBuild(compBuilder -> compBuilder.provides(iface, name, value, rest));
        return (B) this;
    }
    
    default B provides(Class<?>  iface, FluentProperties ... properties) {
        andThenBuild(compBuilder -> compBuilder.provides(iface, properties));
        return (B) this;
    }
    
    default B provides(Class<?>  iface, Dictionary<?,?> properties) {
        andThenBuild(compBuilder -> compBuilder.provides(iface, properties));
        return (B) this;
    }
    
    default B provides(Class<?>[] ifaces) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces));
        return (B) this;
    }
    
    default B provides(Class<?>[] ifaces, String name, Object value, Object ... rest) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces, name, value, rest));
        return (B) this;  
    }
    
    default B provides(Class<?>[] ifaces, FluentProperties ... properties) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces, properties));
        return (B) this;
    }
    
    default B provides(Class<?>[] ifaces, Dictionary<?,?> properties) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces, properties));
        return (B) this;
    }
    
    default B provides(String  iface) {
        andThenBuild(compBuilder -> compBuilder.provides(iface));
        return (B) this;
    }
    
    default B provides(String  iface, String name, Object value, Object ... rest) {
        andThenBuild(compBuilder -> compBuilder.provides(iface, name, value, rest));
        return (B) this;
    }
    
    default B provides(String  iface, FluentProperties ... properties) {
        andThenBuild(compBuilder -> compBuilder.provides(iface, properties));
        return (B) this;
    }
    
    default B provides(String  iface, Dictionary<?,?> properties) {
        andThenBuild(compBuilder -> compBuilder.provides(iface, properties));
        return (B) this;
    }
    
    default B provides(String[] ifaces) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces));
        return (B) this;
    }
    
    default B provides(String[] ifaces, String name, Object value, Object ... rest) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces, name, value, rest));
        return (B) this;  
    }
    
    default B provides(String[] ifaces, FluentProperties ... properties) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces, properties));
        return (B) this;
    }
    
    default B provides(String[] ifaces, Dictionary<?,?> properties) {
        andThenBuild(compBuilder -> compBuilder.provides(ifaces, properties));
        return (B) this;
    }

    default B properties(Dictionary<?, ?> properties) {
        andThenBuild(compBuilder -> compBuilder.properties(properties));
        return (B) this;
    }

    default B properties(String name, Object value, Object ... rest) {
        andThenBuild(compBuilder -> compBuilder.properties(name, value, rest));
        return (B) this;
    }
    
    default B properties(FluentProperties ...properties) {
        andThenBuild(compBuilder -> compBuilder.properties(properties));
        return (B) this;
    }
    
    default B withSrv(Class<?> service, String filter) {
        andThenBuild(compBuilder -> compBuilder.withSrv(service, filter));
        return (B) this;
    }

    default B withSrv(Class<?> ... services) {
        andThenBuild(compBuilder -> compBuilder.withSrv(services));
        return (B) this;
    }

    default <U> B withSrv(Class<U> service, Consumer<ServiceDependencyBuilder<U>> consumer) {
        andThenBuild(compBuilder -> compBuilder.withSrv(service, consumer));
        return (B) this;
    }
    
    default B withCnf(Consumer<ConfigurationDependencyBuilder> consumer) {
        andThenBuild(compBuilder -> compBuilder.withCnf(consumer));
        return (B) this;
    }
    
    default B withBundle(Consumer<BundleDependencyBuilder> consumer) {
        andThenBuild(compBuilder -> compBuilder.withBundle(consumer));
        return (B) this;
    }
    
    default <U> B withFuture(CompletableFuture<U> future, Consumer<FutureDependencyBuilder<U>> consumer) {
        andThenBuild(compBuilder -> compBuilder.withFuture(future, consumer));
        return (B) this;
    }
    
    default B init(String callback) {
        andThenBuild(compBuilder -> compBuilder.init(callback));
        return (B) this;
    }
    
    default B start(String callback) {
        andThenBuild(compBuilder -> compBuilder.start(callback));
        return (B) this;
    }

    default B stop(String callback) {
        andThenBuild(compBuilder -> compBuilder.stop(callback));
        return (B) this;
    }

    default B destroy(String callback) {
        andThenBuild(compBuilder -> compBuilder.destroy(callback));
        return (B) this;
    }
    
    default B init(Object callbackInstance, String callback) {
        andThenBuild(compBuilder -> compBuilder.init(callbackInstance, callback));
        return (B) this;
    }
    
    default B start(Object callbackInstance, String callback) {
        andThenBuild(compBuilder -> compBuilder.start(callbackInstance, callback));
        return (B) this;
    }

    default B stop(Object callbackInstance, String callback) {
        andThenBuild(compBuilder -> compBuilder.stop(callbackInstance, callback));
        return (B) this;
    }

    default B destroy(Object callbackInstance, String callback) {
        andThenBuild(compBuilder -> compBuilder.destroy(callbackInstance, callback));
        return (B) this;
    }
    
    default <U> B init(CbConsumer<U> callback) {
        andThenBuild(compBuilder -> compBuilder.init(callback));
        return (B) this;
    }
    
    default <U> B start(CbConsumer<U> callback) {
        andThenBuild(compBuilder -> compBuilder.start(callback));
        return (B) this;
    }

    default <U> B stop(CbConsumer<U> callback) {
        andThenBuild(compBuilder -> compBuilder.stop(callback));
        return (B) this;
    }

    default <U> B destroy(CbConsumer<U> callback) {
        andThenBuild(compBuilder -> compBuilder.destroy(callback));
        return (B) this;
    }
        
    default <U> B init(CbTypeComponent<U> callback) {
        andThenBuild(compBuilder -> compBuilder.init(callback));
        return (B) this;
    }
        
    default <U> B start(CbTypeComponent<U> callback) {
        andThenBuild(compBuilder -> compBuilder.start(callback));
        return (B) this;
    }
        
    default <U> B stop(CbTypeComponent<U> callback) {
        andThenBuild(compBuilder -> compBuilder.stop(callback));
        return (B) this;
    }
        
    default <U> B destroy(CbTypeComponent<U> callback) {
        andThenBuild(compBuilder -> compBuilder.destroy(callback));
        return (B) this;
    }
        
    default B initInstance(Runnable callback) {
        andThenBuild(compBuilder -> compBuilder.initInstance(callback));
        return (B) this;
    }
        
    default B startInstance(Runnable callback) {
        andThenBuild(compBuilder -> compBuilder.startInstance(callback));
        return (B) this;
    }
        
    default B stopInstance(Runnable callback) {
        andThenBuild(compBuilder -> compBuilder.stopInstance(callback));
        return (B) this;
    }
        
    default B destroyInstance(Runnable callback) {
        andThenBuild(compBuilder -> compBuilder.destroyInstance(callback));
        return (B) this;
    }
        
    default B initInstance(CbComponent callback) {
        andThenBuild(compBuilder -> compBuilder.initInstance(callback));
        return (B) this;
    }

    default B startInstance(CbComponent callback) {
        andThenBuild(compBuilder -> compBuilder.startInstance(callback));
        return (B) this;
    }

    default B stopInstance(CbComponent callback) {
        andThenBuild(compBuilder -> compBuilder.stopInstance(callback));
        return (B) this;
    }

    default B destroyInstance(CbComponent callback) {
        andThenBuild(compBuilder -> compBuilder.destroyInstance(callback));
        return (B) this;
    }

   default B autoConfig(Class<?> clazz, boolean autoConfig) {
        andThenBuild(compBuilder -> compBuilder.autoConfig(clazz, autoConfig));
        return (B) this;
    }
    
    default B autoConfig(Class<?> clazz, String field) {
        andThenBuild(compBuilder -> compBuilder.autoConfig(clazz, field));
        return (B) this;
    }
    
    default B debug(String label) {
        andThenBuild(compBuilder -> compBuilder.debug(label));
        return (B) this;
    }
    
    default B composition(String getCompositionMethod) {
        andThenBuild(compBuilder -> compBuilder.composition(getCompositionMethod));
        return (B) this;
    }

    default B composition(Object instance, String getCompositionMethod) {
        andThenBuild(compBuilder -> compBuilder.composition(instance, getCompositionMethod));
        return (B) this;
    }
    
    default B composition(Supplier<Object[]> getCompositionMethod) {
        andThenBuild(compBuilder -> compBuilder.composition(getCompositionMethod));
        return (B) this;
    }
}
