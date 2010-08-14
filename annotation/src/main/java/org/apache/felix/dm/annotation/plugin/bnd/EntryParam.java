package org.apache.felix.dm.annotation.plugin.bnd;

/**
 * The type of parameters which can be found in a component descriptor.
 */
public enum EntryParam
{
    init, 
    start, 
    stop, 
    destroy, 
    impl, 
    provides, 
    properties, 
    composition, 
    service, 
    filter, 
    defaultImpl, 
    required, 
    added, 
    changed,
    removed,
    autoConfig, 
    pid, 
    factoryPid,
    propagate, 
    updated, 
    timeout,
    adapteeService,
    adapteeFilter,
    stateMask,
    ranking,
    factorySet,
    factoryConfigure,
    factoryMethod,
    field,
    name,
    publisher,
    unpublisher
}
