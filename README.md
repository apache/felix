# Apache Felix Logback

**Apache Felix Logback** is a small integration of the [Logback](https://logback.qos.ch/) backend with OSGi.

With **OSGi R7** the **Log Service Specification 1.4**  (Log 1.4) brings a slew of new features designed to improve the developer experience with logging, the details of which can be read about [here](https://osgi.org/specification/osgi.cmpn/7.0.0/service.log.html).

This project is intended to help bridge the last frontier of OSGi logging by leveraging many capabilities of [Logback](https://logback.qos.ch/), the new **Log 1.4** features to provide a **unified backend** where:

1. all logging *can be* configured in one place
2. all log records are appended together (or at least all appenders are setup in one place)
3. support as many logging APIs as possible

## Depedencies

To add Logback to an OSGi framework include the following dependencies

    :::xml
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.x</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-core</artifactId>
        <version>1.2.x</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.x</version>
    </dependency>

These provide the `slf4j` logging API over the Logback backend.

## Basic Configuration

Configuring logback is most easily handled by setting the system property `logback.configurationFile` to point to a file on the file system.

This is an example using a *bndrun* file:

    :::properties
    -runproperties: logback.configurationFile=file:${.}/logback.xml


where in bnd `${.}` gives the path to the directory of the bndrun file.

Logback offers many features from it's configuration file, so make sure to look through the [documentation](https://logback.qos.ch/documentation.html).

## Deployment Options

There are at least two possible deployment scenarios.

### The Standard Approach

The **standard approach** is to install all bundles into the OSGi runtime.

**pros**:

- this is the most understood and expected deployment scenario
- bundles are managed through any OSGi management agent

**cons**:

- due to the fact that logging APIs are most commonly static there is an inevitable race condition which is exacerbated by aspects such as:
  - the order in which the bundles are started
  - which bundle makes the first call the static API that most-likely has the side effect of binding an implementation
  - buffering is required to collect the complete set of logs, but configuring the buffer to hold that many logs can cause memory issues, or if too small to lose records. Also, some logging frameworks simply don't provide such buffering, and some actually simply fail in this scenario.
    In the case of the OSGi Log Service this is handled by implementations that support Log 1.4, or perhaps earlier through proprietary means.

### The Immediate Approach

The **immediate approach** overcomes the **cons** of *the standard approach* and initializes the log engine as early as possible in order to avoid buffering, binding issues, or losing records.

**pros**:

- there's very little chance to miss any log records
- there are no temporal issues for binding or bundle start order since the log engine is setup before most other application activities take place

**cons**:

- bundles on the framework class path are not installed into the framework so they cannot be managed through regular means. *On the other hand, logging is one of those infrastructural aspects that transcends the application and for which there are very few scenarios under which one would want to manage logging API or implementation bundles this way. Configuration? Sure!*

## Unified Backend

Of course adding Logback does not magically result in all logs being funneled into the same appenders, particularly the OSGi logs. However, it is quite common to assemble an OSGi application from a variety of bundles which use a variety of logging APIs. Many of these can already be mapped onto Logback.

Many examples of setting up the various logging APIs can be found in the integration tests of the project.

The following APIs are tested:

- JBoss Logging 3.3.x
- Commons Logging 1.2
- JUL (Java Util Logging)
- Log4j 1
- Log4j 2
- Slf4j

## Mapping of OSGi Events


The OSGi Log specification 1.4 describes events resulting in log records. **Log 1.4** defines logger names mapping to these events.

Event                     | Logger Name        | Events types
------------------------- | ------------------ | ------------------------------------------------------------
Bundle event              | `Events.Bundle`    | `INSTALLED` - BundleEvent INSTALLED<br />`STARTED` - BundleEvent STARTED<br />`STOPPED` - BundleEvent STOPPED<br />`UPDATED` - BundleEvent UPDATED<br />`UNINSTALLED` - BundleEvent UNINSTALLED<br />`RESOLVED` - BundleEvent RESOLVED<br />`UNRESOLVED` - BundleEvent UNRESOLVED
Service event             | `Events.Service`   | `REGISTERED` - ServiceEvent REGISTERED<br /> `MODIFIED` - ServiceEvent MODIFIED<br /> `UNREGISTERING` - ServiceEvent UNREGISTERING
Framework event           | `Events.Framework` | `STARTED` - FrameworkEvent STARTED<br />`ERROR` - FrameworkEvent ERROR<br />`PACKAGES_REFRESHED` - FrameworkEvent PACKAGES REFRESHED<br />`STARTLEVEL_CHANGED` - FrameworkEvent STARTLEVEL CHANGED<br />`WARNING` - FrameworkEvent WARNING<br />`INFO` - FrameworkEvent INFO
Legacy Log Service events | `LogService`       | any log events originating from bundles that used the original `LogService` logging API

**Note:** In order to improve the granularity of the logging associated with these events, **Apache Felix Logback** allows appending a period (`.`) followed by any number of segments (separated by periods) of the `Bundle-SymbolicName` for the logger names used in configuring the levels. This greatly improves the configurability.

Consider the following `logback.xml` example:

    :::xml
    <configuration>

        <!-- defined a console append -->
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%.15thread] %-5level %logger{36}:%line - %msg%n</pattern>
            </encoder>
        </appender>

        <!-- log all Bundle events -->
        <logger name="Events.Bundle" level="TRACE"/>

        <!-- log WARN Framework service events of bundles whose BSN starts with `org.eclipse.osgi` (guess who that is?) -->
        <logger name="Events.Service.org.eclipse.osgi" level="WARN"/>

        <!-- turn OFF legacy Log Service records from bundles whose BSN starts with `org.baz` -->
        <logger name="LogService.org.baz" level="OFF"/>

        <!-- log DEBUG Service events for bundles whose BSN starts with `org.fum` -->
        <logger name="Events.Service.org.fum" level="DEBUG"/>

        <!-- log DEBUG from any log API using a logger name starting with `org.my.foo` -->
        <logger name="org.my.foo" level="DEBUG"/>

        <root level="ERROR">
            <appender-ref ref="STDOUT" />
        </root>
    </configuration>

## Notes

- **Apache Felix Logback** supports Logback's [automatic reloading](https://logback.qos.ch/manual/configuration.html#autoScan) upon file modification
- When using **equinox** framework you may want to disable it's internal appenders using the system property `eclipse.log.enabled=false`
