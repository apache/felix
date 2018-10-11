# OSGi root cause analysis

[![Build Status](https://builds.apache.org/buildStatus/icon?job=Apache%20Felix%20RootCause)](https://builds.apache.org/job/Apache%20Felix%20RootCause/)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.felix/org.apache.felix.rootcause/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.felix%22%20a%3A%22org.apache.felix.rootcause%22)


Finding the root cause of problems with OSGi declarative services components.

## Usage

See [reference documentation](docs/README.md).

## Build

    mvn clean install

Also check the [Jenkins build](https://builds.apache.org/job/Felix%20Rootcause/)

## Issue reporting

We currently have these [open issues](https://issues.apache.org/jira/issues/?jql=project%20%3D%20FELIX%20AND%20component%20%3D%20%22Root%20Cause%22%20AND%20resolution%20%3D%20Unresolved). Please report issues using project`Felix` and Component `Root Cause`.

## Working with the code

As contributor the easiest way is to fork the [Felix project on github](https://github.com/apache/felix/tree/trunk/rootcause) and do a pull request. We do not actively monitor pull requests so please also open an issue.

When creating a PR it is a good practice to first create a branch named like the jira issue id. The commit message should also refer to the issue id. Like `FELIX-1234 My message`.

