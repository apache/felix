***********************
* Apache Felix README *
***********************

The **Apache Felix** project is a collection of semi-related **OSGi** sub-projects that build and release
individually.

## Felix Framework

The flagship project is the **Apache Felix Framework** which implements the [**OSGi Core R7**](https://osgi.org/specification/osgi.core/7.0.0/) specification. The `/framework` directory contains the source and build tree for the **OSGi**-compliant
framework implementation.

Directly related projects:

- **main** `/main*` - provides an executable jar that launches the Felix framework.

## OSGi Compendium

Several sub-projects cover various **OSGi Compendium** specifications such as:

- [**Configuration Admin**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.cm.html) `/configadmin`
- [**Configurator**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html) `/configurator`
- [**Converter**](https://osgi.org/specification/osgi.cmpn/7.0.0/util.converter.html) `/converter`
- [**Coordinator**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.coordinator.html) `/coordinator`
- [**Deployment Admin**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.deploymentadmin.html) `/deploymentadmin`
- [**Device Access**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.device.html) `/deviceaccess`
- [**Declarative Services**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.component.html) `/scr*`
- [**Event Admin**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.event.html) `/eventadmin`
- [**Http Service**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.http.html) `/http`
- [**Http Whiteboard**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.http.whiteboard.html) `/http`
- [**IO**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.io.html) `/io`
- [**Log Service**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.log.html) `/log*`
- [**Metatype**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.metatype.html) `/metatype`
- [**Preferences**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.prefs.html) `/prefs`
- [**Resolver**](https://osgi.org/specification/osgi.core/7.0.0/service.resolver.html) `/resolver`
- [**UPnP**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.upnp.html) `/upnp`
- [**User Admin**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.useradmin.html) `/useradmin`
- [**Wire Admin**](https://osgi.org/specification/osgi.cmpn/7.0.0/service.wireadmin.html) `/wireadmin`

## Extra Features

Several projects provide extra features to an OSGi runtime.

- **bundle repository** `/bundlerepository` - Bundle repository service.
- **connect** `/connect` - A service registry that enables OSGi style service registry programs without using an OSGi framework.
- **dependency manager** `/dependencymanager` - A versatile java API, allowing to declaratively
  register, acquire, and manage dynamic OSGi services.
- **fileinstall** `/fileinstall*` - A utility to automatically install bundles from a directory.
- **gogo** `/gogo` - A command line shell, runtime and set of base commands for interacting with and introspecting an OSGi framework.
- **inventory** `/inventory` - Provides some mechanisms to get the current state of the system and therefore provides an inventory of the system.
- **ipojo** `/ipojo` - A *service component runtime* aiming to simplify OSGi application development.
- **jaas support** `/jaas` - Bundle to simplify JAAS usage within OSGi environment.
- **logback** `/logback` - A simple integration of the OSGi R7 Log (1.4) service to Logback backend.
- **rootcause** `/rootcause` - Finding the root cause of problems with OSGi declarative services components.
- **systemready** `/systemready` - Provides a framework to configure and create so called system checks, and signal the ready state of an OSGi based system.
- **utils** `/utils` - Utility classes for OSGi (intended for embedding within other bundles.)
- **webconsole** `/webconsole*` - Web Based Management Console for OSGi Frameworks.
- and many other **OSGi** things

## Build tools

The `/tools` directory contains various build tools.

- **maven-bundle-plugin** `/tools/maven-bundle-plugin` - A maven plugin for building **OSGi** bundles.
- **osgicheck-maven-plugin** `/tools/osgicheck-maven-plugin` - Maven plugin for checking several OSGi aspects of your project.

