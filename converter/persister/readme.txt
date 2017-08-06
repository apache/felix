# Apache Felix Converter - Schematizer module

## Overview

The Schematizer follows the concept of "DTO-as-Schema", meaning the idea that
the DTO describes the data schema, and using this idea to make the schema a
first-class citizen in the design and implementation of a domain model.

## DTO-as-Schema

DTO-as-Schema (DaS) takes a step away from common Object Oriented (OO) design principles.
When learning OO programming, common convention was to "hide away" the data in 
order to "protect" it from the wild. Instead of accessing a field directly, the
idea was to make a field private, and provide "getters" and "setters". The getters
and setters were supposed to ensure the invariants of the object. Often, however,
we would end up with code like this:

```java
public class SomeClass {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue( String aValue ) {
        value = aValue;
    }
}
```

The above is really just a complicated and misleading way of doing this:

```java
public class SomeClass {
    public String value;
}
```

Even when OO-style classes are well written, it can be argued that the idea of data-hiding
is a farce anyway when dealing with distributed systems. The reason is because the classes
need to be serialized before they are put on the wire, and deserialzed again by the remote system.
This requires exposing the system in the form of an "API", these days usually as a REST API.
So, when the system is seen as a whole, we recognize that it is simply not possible to have
a working complex system while "hiding" the core data.

DaS is based on this admission. We admit that there are really *two* interfaces: 
a *programmatic API* and a *data API*.

In the [WHICH?] OSGi specification, DTOs were introduced as a convention for describing objects
and transferring their state between system sub-parts. It so happens that the rules for DTOs
describe a schema, in Java code, for the data objects being transferred. By taking advantage of this
schema and elevating it as a first-class citizen during the design and implementation of domain
objects, we can elegantly expose both the programmatic API and the data API in code, and reap a few
other benefits as well, as described below.

Building also on other ideas, notably some of the ideas emerging from functional programming,
it is possible to develop domain models with a leaner--and thus more productive--code base.

# STATUS

This module is highly experimental and is *not* recommended for production.

# Coding conventions

[TODO]

# Topics to Explore

## Schema transforms
## Lenses