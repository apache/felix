***********************
* Apache Felix README *
***********************

This document is basically a catch-all for general information about the Apache
Felix project.  It is anticipated that this document will be updated from time
to time with new information.

SVN PROJECT STRUCTURE

The Apache Felix project has the following directory structure in order to organize
the project's varied product artifacts.

+ framework
+ project-xyz
+ tools

The *framework* directory contains the source and build tree for the OSGi-compliant
framework implementation.  

The *project-xyz* is a placeholder for the various subprojects of Apache Felix. The
subprojects cover other OSGi specifications like Declarative Services, Config Admin as
well as other useful OSGi bundles.



The *tools* directory contains a directory tree for various build
tools. The maven-bundle-plugin lives in tools/maven-bundle-plugin.

