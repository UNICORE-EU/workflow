# UNICORE Workflow service

[![Unit tests](https://github.com/UNICORE-EU/workflow/actions/workflows/maven.yml/badge.svg)](https://github.com/UNICORE-EU/workflow/actions/workflows/maven.yml)

This repository contains the source code for the UNICORE
Workflow service, a server component that supports submission
and execution of application workflows consisting of UNICORE jobs
and control constructs.

 * RESTful APIs

 * JSON workflow description

 * Full range of UNICORE user authentication options and AAI
   integration

## Download

The Workflow service is distributed as part of the "Core Server" bundle and can be
[downloaded from GitHub](https://github.com/UNICORE-EU/server-bundle/releases)

## Documentation

See the manual at
https://unicore-docs.readthedocs.io/en/latest/admin-docs/workflow/index.html

## Building from source

You need Java and Apache Maven.

The Java code is built and unit tested using

    mvn install

To skip unit testing

    mvn install -DskipTests

The following commands create distribution packages
in tgz, deb and rpm formats

    mvn install -DskipTests
    cd workflow-server
    # tgz
    mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz
    # deb
    mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian
    # rpm
    mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat
