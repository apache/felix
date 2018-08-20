# Apache Converter

## Overview

This is the home for the OSGi R7-compliant Converter. It is an implementation
of the Converter Specification. For more details, see Chapter 707 of the
OSGi Compendium.

There are two other sister projects: Serializer and Schematizer.

## Serializer
The Serializer, based heavily on the Converter, is useful for transforming a
serialized string of text to an object, and vice-versa. Please refer to the
project for more details.


## Schematizer
Once data is serialized, until you can identity the type of object associated
with the serialized data, it is difficult to guess which object the data should
be serialized to.

The Schematizer, based on the Converter and the Serializer, is useful in cases
where serialized data needs to contain meta data about the type of object
serialized. Using the Schematizer, it is possible to serialize different data
types to the same stream, then upon deserialization, by reading the meta data
determine the object type to be used.

Please refer to the project for more details.
