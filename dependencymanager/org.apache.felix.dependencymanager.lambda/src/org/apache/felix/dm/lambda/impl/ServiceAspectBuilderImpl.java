package org.apache.felix.dm.lambda.impl;

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.ComponentBuilder;
import org.apache.felix.dm.lambda.ServiceAspectBuilder;

public class ServiceAspectBuilderImpl<T> extends ServiceCallbacksBuilderImpl<T, ServiceAspectBuilder<T>> implements 
    AdapterBase<ServiceAspectBuilder<T>>, ServiceAspectBuilder<T> 
{    
    private final DependencyManager m_dm;
    private final Class<T> m_aspectType;
    private String m_aspectFilter;
    private int m_aspectRanking;
    private boolean m_autoAdd = true;
    private Consumer<ComponentBuilder<?>> m_compBuilder = (componentBuilder -> {});

    public ServiceAspectBuilderImpl(DependencyManager dm, Class<T> aspectType) {
        super(aspectType);
        m_dm = dm;
        m_aspectType = aspectType;
    }

    @Override
    public void andThenBuild(Consumer<ComponentBuilder<?>> after) {
        m_compBuilder = m_compBuilder.andThen(after);        
    }

    @Override
    public ServiceAspectBuilderImpl<T> autoAdd(boolean autoAdd) {
        m_autoAdd = autoAdd;
        return this;
    }
    
    public boolean isAutoAdd() {
        return m_autoAdd;
    }

    @Override
    public ServiceAspectBuilder<T> filter(String aspectFilter) {
        m_aspectFilter = aspectFilter;
        return this;
    }

    @Override
    public ServiceAspectBuilder<T> rank(int ranking) {
        m_aspectRanking = ranking;
        return this;
    }
    
    @Override
    public Component build() {
        Objects.nonNull(m_aspectType);
        
        if (getAutoConfigField() != null && (hasRefs()|| hasCallbacks())) {
            throw new IllegalStateException("Can't mix autoConfig fields and aspect callbacks.");
        }
        
        Component c = null;
        if (getAutoConfigField() != null) {
            c = m_dm.createAspectService(m_aspectType, m_aspectFilter, m_aspectRanking, getAutoConfigField());
        } else if (hasRefs()) {        
            Object cbInstance = createCallbackInstance();
            String add = "add";
            String change = "change";
            String remove = "remove";
            String swap = m_swapRefs.size() > 0 ? "swap" : null;        
            c = m_dm.createAspectService(m_aspectType, m_aspectFilter, m_aspectRanking, cbInstance, add, change, remove, swap);
        } else if (hasCallbacks()) {
            String add = getAdded();
            String change = getChanged();
            String remove = getRemoved();
            String swap = getSwapped();
            c = m_dm.createAspectService(m_aspectType, m_aspectFilter, m_aspectRanking, add, change, remove, swap);
        } else {
            c = m_dm.createAspectService(m_aspectType, m_aspectFilter, m_aspectRanking);
        }
        ComponentBuilderImpl cb = new ComponentBuilderImpl(c, false);
        // m_compBuilder is a composed consumer that calls in sequence all necessary component builder methods. 
        m_compBuilder.accept (cb);
        return cb.build();
    }
}
