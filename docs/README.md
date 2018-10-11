# Reference Documentation Felix RootCause

Finding the root cause of problems with OSGi declarative services components.

## Requirements:

* Configuration Admin Service
* Service Component Runtime

See below for a hands on example in Apache Karaf.

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
