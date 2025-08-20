# JETTA – jetta-core

[//]: # ([![Build]&#40;https://github.com/boonen/jetta/actions/workflows/01-build.yml/badge.svg&#41;]&#40;https://github.com/boonen/jetta/actions/workflows/01-build.yml&#41;)

[//]: # ([![IT]&#40;https://github.com/boonen/jetta/actions/workflows/02-it.yml/badge.svg&#41;]&#40;https://github.com/boonen/jetta/actions/workflows/02-it.yml&#41;)
[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=boonen_jetta&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=boonen_jetta-core)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=boonen_jetta&metric=coverage)](https://sonarcloud.io/summary/new_code?id=boonen_jetta-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/boonen/jetta?display_name=tag)](https://github.com/boonen/jetta/releases)

## Introduction

**JETTA** stands for **JUnit Extension for Testcontainers & IntelliJ HTTP Client**.  
This library aims to make it easy to run API tests written with the IntelliJ HTTP Client inside JUnit 5, orchestrated 
with Testcontainers. It is a Java 24–based library and is designed to be published via GitHub Packages. Jetbrains
published a [nice blog post](https://blog.jetbrains.com/idea/2022/12/http-client-cli-run-requests-and-tests-on-ci/#running-tests-on-ci) 
on how to use their IntelliJ HTTP client.

> This repository contains the `jetta-core` artifact.
> 
> **GroupId:** nl.janboonen.jetta</dependency>
> 
> **ArtifactId:** jetta-core</artifactId>

## Getting started

To use this extension, you will need to have a correctly working Container Runtime. Please refer to the 
[Testcontainers documentation](https://java.testcontainers.org/supported_docker_environment/) for details on how to set 
up your specific environment.

### Installation

Add the GitHub Packages repository and the dependency.

```xml
<repositories>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/boonen/jetta</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>nl.janboonen.jetta</groupId>
    <artifactId>jetta-core</artifactId>
    <version>0.1.0</version>
  </dependency>
</dependencies>
```

### Usage

In your Maven project, add an integration test class under `src/test/java`:

```java
package nl.janboonen.jetta.test;

import nl.janboonen.labs.test.IntellijHttpClientFile;
import nl.janboonen.labs.test.IntellijHttpClientTestSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@IntellijHttpClientFile(value = "src/test/http/ascii.http")
public class SampleIT extends IntellijHttpClientTestSupport {

    @LocalServerPort
    private int port;

    @Override
    protected int getPort() {
        return port;
    }

}
```

There are four things to note:
1. Configuration is defined using the `@IntellijHttpClientFile` annotation. The `value` property is required and should 
   point to your HTTP Client file. The path is relative to the root of your Maven project.
2. The test class must extend `IntellijHttpClientTestSupport`, which automatically bootstraps the IntelliJ HTTP Client 
   using their official Docker image.
3. The `getPort()` method must be overridden to return the port on which your application. If you use Spring Boot's 
   testing support like in this example, you can use the `@LocalServerPort` annotation to inject the port.
4. The application containg the API to be tested must be started before the tests run. In this example we used 
   `@SpringBootTest` to start the application at a random port.

If your test executes correctly, then the JUnit5 integration will generate a test report for each Client Test that is 
defined in the specified IntelliJ HTTP Client file. For more information on how to write Client Tests, please refer to 
[Jetbrains' IntelliJ documentation](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html#create-a-physical-http-request-file).

The [src/test/resources/ijhttp](./src/test/resources/ijhttp) directory contains an example HTTP Client file that you can use
to draw inspiration from.

## Development

### Build from source

Requirements: Java 24 and Maven 3.9+
```bash
# CI-friendly version defaults:
#   revision=0.1.0, changelist=-SNAPSHOT
mvn -B -U verify
```

To build a release locally using CI-friendly versions:
```bash
mvn -B -Drevision=0.1.0 -Dchangelist= package
```

### Contributing

Issues and Pull Requests are welcome.
*	Issues: Please use the templates (bug report / feature request) and provide minimal reproduction where possible.
*	Pull Requests: Keep changes focused. Include tests where applicable. Follow conventional commit messages if you can (e.g. feat: ..., fix: ...).

*By contributing, you agree that your contributions are MIT-licensed.*
