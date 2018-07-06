The [OSGi Service Compendium specification](https://osgi.org/specification/osgi.cmpn/7.0.0/) defines a general purpose [Log Service](https://osgi.org/specification/osgi.cmpn/7.0.0/service.log.html) for the OSGi Platform. It is a very simple specification that doesn't provide all the functionality commonly available in enterprise-level logging tools, but its extensible service model can be used to build fairly sophisticated logging solutions.

The Log Service specification defines the following entities:

* *`org.osgi.service.log.Logger`* - (*since 1.4*) interface that allows a bundle to log information, including a message, a level, an exception, and a `ServiceReference` object. The formatting style using `{}` placeholders follows the *slf4j* approach. A derivative of `Logger` called `FormatterLogger` uses the `java.util.Formatter` syntax.
* *`org.osgi.service.log.LoggerFactory`* - (*since 1.4*) service interface that allows a bundle to obtain a Logger. A Logger is named and associated with a `Bundle` object.
* *`org.osgi.service.log.admin.LoggerContext`* - (*since 1.4*) interface that allows the configuration of effective logging levels for a Bundle. The configuration can be set in Configuration Admin and via method calls.
* *`org.osgi.service.log.admin.LoggerAdmin`* - (*since 1.4*) interface for managing the configuration of log levels.
* *`org.osgi.service.log.LogService`* - *legacy* service interface that allows a bundle to log information, including a message, a level, an exception, a `ServiceReference` object, and a `Bundle` object. The methods of this service are deprecated and it is recommended to use LoggerFactory and Loggers instead.
* *`org.osgi.service.log.LogReaderService`* - service interface that allows access to a list of recent `LogEntry` objects, and allows the registration of a `LogListener` object that receives `LogEntry` objects as they are created.
* *`org.osgi.service.log.LogEntry`* - interface defining a log entry.
* *`org.osgi.service.log.LogListener`* - interface defining a listener for log entries, which is notified about new log entries.

## Accessing Loggers

Loggers are obtained through the `LoggerFactory` service:

    :::java
    public class Activator implements BundleActivator
    {
        private volatile LoggerFactory loggerFactory;
        
        public void start(BundleContext context) throws Exception 
        {	
            ServiceReference ref = context.getServiceReference(LoggerFactory.class.getName());
            if (ref != null)
            {
                loggerFactory = (LoggerFactory) context.getService(ref);
            }
        }
    
        //..

Elsewhere in the bundle you can then use the `LoggerFactory` to get a `Logger` for any class:

    :::java
    Logger logger = loggerFactory.getLogger(Foo.class);

Declarative Services (*since 1.4*) has a convenient integration which allows a component to obtain a logger specific to it's class with little effort by using a reference who's service type is `LoggerFactory` while the injection type is either `Logger` or `FormatterLogger`:

    :::java
    @Reference(service = LoggerFactory.class)
    private Logger logger;

The `Logger` interface defines 6 levels of logging to coincide with most other log APIs:

- AUDIT
- ERROR
- WARN
- INFO
- DEBUG
- TRACE

Each level has methods on the Logger interface appropriate to that level such as `.info(...)` and `.isInfoEnabled()`.

## Configuring  Log Levels

Since 1.4 the **Log Service Specification** provides the ability to manage the log levels both programatically and through **Configuration Admin**.

Programatic configuration is achieved through the `LoggerAdmin` service:

    :::java
    ServiceReference ref = context.getServiceReference(
        LoggerAdmin.class.getName());
    
    if (ref != null)
    {
        LoggerAdmin loggerAdmin = (LoggerAdmin) context.getService(ref);
    
        // get the ROOT logger context
        LoggerContext rootContext = loggerAdmin.getLoggerContext(null);
        
        Map<String, LogLevel> levels = rootContext.getLogLevels();
        // adjust the levels
        rootContext.setLogLevels(levels);
        
        // get the levels for a bundle (felix scr in this case)
        LoggerContext scrContext = loggerAdmin.getLoggerContext(
            "org.apache.felix.scr");
        
        // set all of scr to DEBUG mode
        scrContext.setLogLevels(
            Collections.singletonMap(
                Logger.ROOT_LOGGER_NAME, LogLevel.DEBUG));
    }

Likewise logging configuration can be handled through **Configuration Admin**. Following the previous example of configuring **Felix SCR** for `DEBUG`  mode:

- create a configuration object whose PID is *`org.osgi.service.log.admin|org.apache.felix.scr`*
- set the property `ROOT` in the configuration to `DEBUG`
  e.g. `ROOT=DEBUG`

## Accessing the log service (legacy)

To access a `LogService` instance it is necessary to look it up in the OSGi service registry as demonstrated in the following code snippet:

    :::java
    public class Activator implements BundleActivator
    {
        public void start(BundleContext context) throws Exception 
        {	
            ServiceReference ref = context.getServiceReference(LogService.class.getName());
            if (ref != null)
            {
                LogService log = (LogService) context.getService(ref);
    
                // Use the log...
            }
        }
    
        //..

It is possible, and advisable, to use more sophisticated service acquisition mechanisms like a Service Tracker, Declarative Services or iPOJO.

## Using the log service (legacy)

The `LogService` interface provides four methods for logging:


    :::java
    public interface LogService
    {
        //..
    
        // Log a message specifying a log level
        public log(int level, java.lang.String message)  
    
        // Log an exception
        public log(int level, java.lang.String message, java.lang.Throwable exception)  
    
        // Log a message specifying the ServiceReference that generated it
        public log(ServiceReference sr, int level, java.lang.String message)  
    
        // Log a message specifying the ServiceReference and exception
        public log(ServiceReference sr, int level, java.lang.String message, java.lang.Throwable exception)  
    }	


Log levels are defined in the same interface:

* `LogService.LOG_DEBUG`
* `LogService.LOG_INFO`
* `LogService.LOG_WARNING`
* `LogService.LOG_ERROR`

## Retrieving log entries

The `LogReaderService` provides a `getLog()` method to retrieve an `Enumeration` of the latest log entries. The following code snippets demonstrates how to retrieve it from the service registry and use it:


    :::java
    ServiceReference ref = context.getServiceReference(LogReaderService.class.getName());
    if (ref != null)
    {
        LogReaderService reader = (LogReaderService) context.getService(ref);	
        Enumeration<LogEntry> latestLogs = reader.getLog();
    }


## Creating and registering a `LogListener`

The Log Service specification doesn't define any particular entity to store, display, or write log entries; it's up to the developer to implement this functionality or to choose an available implementation capable of doing that. To create such a bundle, the first step is to create an implementation of the `LogListener` interface. The following code shows a simple implementation that echoes the log message:


    :::java
    public class LogWriter implements LogListener
    {
        // Invoked by the log service implementation for each log entry
        public void logged(LogEntry entry) 
        {
            System.out.println(entry.getMessage());
        }
    }


The only method to implement is `logged()` method, which is called every time a log entry is created in the associated logging service. A `LogListener` implementation must be registered with the `LogReaderService` so it can start receiving log entries, as demonstrated in the following code snippet:


    :::java
    ServiceReference ref = context.getServiceReference(LogReaderService.class.getName());
    if (ref != null)
    {
        LogReaderService reader = (LogReaderService) context.getService(ref);
        reader.addLogListener(new LogWriter());
    }



## Setup of Apache Felix Log Service

The Apache Felix Log Service bundle doesn't have any specific dependency on Felix, so it can run on any OSGi container. For its configuration, it will use the following optional system properties:
 	 
|Property|Default|Description|
|--|--|--|
|`org.apache.felix.log.maxSize`|100|The maximum size of the log history. A value of -1 means the log has no maximum size; a value of 0 means that no historical information is maintained|
|`org.apache.felix.log.storeDebug`|false|Determines whether or not debug messages will be stored in the history|
|`org.osgi.service.log.admin.loglevel`|`WARN`|The default log level of the root Logger Context|

