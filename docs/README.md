# Reference Documentation

This project provides a framework to configure and create so called _system ready checks_ and report the _ready_ of an application on top of an OSGi system.

## Requirements:

* Configuration Admin Service
* Service Component Runtime

See below for a hands on example in Apache Karaf.

## Services Check

Ready check that is shipped in the core bundle and checks for the presence of listed services by interface name or filter.

Mandatory configuration with pid: `ServicesCheck`

* `services.list=<List of service interfaces or filters to check>`

The check reports GREEN when all services are currently present and YELLOW if at least one service is missing.

In the details the check reports all missing services. If a service is backed by a DS component then automatically a root cause analysis is executed. If such a service is missing then unresolved references are shown in a tree with detailed information about each component. At the leafs of the tree the root causes can be found.

## Component Check

Ready check that is shipped in the core bundle and checks for the presence of listed DS components by name.

Mandatory configuration with pid: `ComponentsCheck`

* `components.list=<List of component names to check>`

The check reports GREEN when all components are satisfied. It also provides root cause analysis.
The main difference to th Service Check is that the checked component does not need to offer an OSGi service.

## Providing additional custom checks

Implement the org.apache.felix.systemready.core.SystemReadyCheck interface and register
your instance as a service. The SystemReadyMonitor will pick up your service automatically.

Your service should avoid references to other services that come up late, as a late appearing check could
make the aggregated state oscilate during startup. One option to avoid this is to refer to a late service using an optional dependency and return a YELLOW state until it is up.

## System Ready Monitor service

The service org.apache.felix.systemready.core.SystemReadyMonitor tracks all SystemReadyCheck services and periodically checks them. It creates an aggregated status and detailed report of the status of the system.

This report can be queried by calling the service. Additionally the system ready servlet can provide this status over http.

For an example see the [test case](../src/test/java/org/apache/felix/systemready/core/osgi/SystemReadyMonitorTest.java).

## Ready servlet

The Ready servlet provides the aggregated state of the system over http in json format.
It is registered on the path `/systemready`.

This is an example of a ready system with just the services check.
```
{
  "systemStatus": "GREEN", 
  "checks": [
    { "check": "Services Check", "status": "GREEN", "details": "" }, 
  ]
}
```

The servlet can be configured using the pid `org.apache.felix.systemready.impl.servlet.SystemReadyServlet`.

The default config is

    osgi.http.whiteboard.servlet.pattern=/systemready

You can set the servlet pattern and the servlet context select filter. The default works for Apache Karaf.
When using the servlet in Apache Felix Http Whiteboard or Adobe AEM make sure you set the servlet context select too:

    osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=org.osgi.service.http)

## Root cause command

For quickly checking for a root cause of a problem with a declarative services component there is also a handy command.

`rootcause <ds-compoment-name>`

It prints the top level status of a DS component as well as the tree of DS unsatisfied components it depends on together with their status.

This is a sample output from the DSRootCause tests. It shows the cause of a component that depends on some other components. The root cause of CompWithMissingRef2 not being satisfied is that CompWithMissingConfig is missing its mandatory config.

```
Component CompWithMissingRef2 unsatisfied references
  ref other interface CompWithMissingRef
    Component CompWithMissingRef unsatisfied references
      ref other interface CompWithMissingConfig
        Component CompWithMissingConfig missing config on pid [CompWithMissingConfig]
```

## Example of using the system ready service framework in Apache Karaf

Download, install and run Apache Karaf 4.1.x. Inside the karaf shell execute this:

```
feature:install scr http-whiteboard
config:property-set --pid ServicesCheck services.list org.osgi.service.log.LogService
config:property-set --pid SystemReadyServlet osgi.http.whiteboard.context.select "(osgi.http.whiteboard.context.name=default)"
install -s mvn:org.apache.felix/org.apache.felix.systemready/0.1.0-SNAPSHOT
```

Point your browser to http://localhost:8181/system/console/ready .

Check the status of a DS component:

```
rootcause SystemReadyMonitor
 Component SystemReadyMonitor statisfied
```

Try this with some of your own components.
