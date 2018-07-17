# OSGi system ready check framework

[![Build Status](https://builds.apache.org/buildStatus/icon?job=Felix%20Systemready)](https://builds.apache.org/job/Felix%20Systemready/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.felix/org.apache.felix.systemready/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.felix%22%20a%3A%22org.apache.felix.systemready%22)

In OSGi there is always the question of when a system is fully operational after startup. This project provides a framework to configure and create so called system checks and signal the ready state of an OSGi based system. In addition to the framework, we also provide some generic checks that give a solid basis, like a check waiting for the startup of bundles to finish, as well as certain OSGi services being present. Additionally, root cause analysis in case of error states is conveniently presented. Custom checks can be created to provide in-depth checks for your own functionality.

See [why system ready for some more background on why to use this project](docs/why_systemready.md).

## Usage

See [reference documentation](docs/README.md).

## Build

    mvn clean install

Also check the [Jenkins build](https://builds.apache.org/job/Felix%20Systemready/)

## Issue reporting

We currently have these [open issues](https://issues.apache.org/jira/issues/?jql=project%20%3D%20FELIX%20AND%20component%20%3D%20%22System%20Ready%22%20AND%20resolution%20%3D%20Unresolved). Please report issues using project`Felix` and Component `System Ready`.

## Working with the code

As contributor the easiest way is to fork the [Felix project on github](https://github.com/apache/felix/tree/trunk/systemready) and do a pull request. We do not actively monitor pull requests so please also open an issue.

When creating a PR it is a good practice to first create a branch named like the jira issue id. The commit message should also refer to the issue id. Like `FELIX-1234 My message`.

