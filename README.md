# Powsybl - Grid Study Environment

[![Build Status](https://travis-ci.org/powsybl/powsybl-gse.svg?branch=master)](https://travis-ci.org/powsybl/powsybl-gse)
[![Build status](https://ci.appveyor.com/api/projects/status/dbwhmay33ynnftmq/branch/master?svg=true)](https://ci.appveyor.com/project/powsybl/powsybl-gse/branch/master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-gse&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Apowsybl-gse)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

http://www.powsybl.com

## Overview
Powsybl-GSE (Grid Study Environment) is an open-source, [JavaFX](https://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html) based, UI for [Powsybl](http://www.powsybl.com).  
GSE enables users to leverage the Powsybl functionalities in order to perform `power grid studies` without using a command line interface.
Its source code is distributed under the Mozilla Public License 2.0 and it is hosted on GitHub: https://github.com/powsybl .

GSE is designed to be fully extensible, allowing new functionalities to be developed and plugged-in. 

Currently GSE exposes these features: 
- [AFS](https://github.com/powsybl/powsybl-core/tree/docs/docs/architecture/afs), to be used as a context container to represent: [cases](), [contingencies](), [simulations-results](), etc.
- Visualization: e.g. to display network data and simulators' results
- Common services, backed by powsybl functionalities, e.g. [loadflow](), [security-analysis](), [action-simulator]().


## Installation

The instructions below explain in details how to build powsybl-gse from the sources.

### Requirements

In order to build the project, you need:
  * Java Development Kit (JDK): *(Java 8 or newer)*
  * [Maven](https://maven.apache.org) (>= 3.5.3) 
  * powsybl-core maven artifacts (GSE sources are aligned to the powsybl-core master branch on github, so *powsybl-core* artifacts must be locally available).


JDK and maven related commands are expected to be available in the PATH .
You might  need to configure your network proxy settings, as the build procedure downloads files from external repositories, e.g. GitHub and Maven central.

## Clone the powsybl-gse repository
These instructions explain how to download the project using a command line based git client, but a git client integrated in an IDE might also be used to download the project code and to build a specific branch (e.g. a release branch)
A git client is also strongly suggested for any development activities.  Alternatively, a sources .zip archive can be downloaded from the [project's GitHub page](https://github.com/powsybl/powsybl-gse).

To clone the latest powsybl-gse repository
```bash
$> git clone https://github.com/powsybl/powsybl-gse.git
```

## Build powsybl-gse 
To download all the required dependencies from the maven external repository and build a Powsybl-GSE binary distribution (using the native JavaFX tools):

```bash
$> cd powsybl-gse/gse-demo
$> mvn install -P native-package
```

The native executable binaries will be created and placed at 

```bash
powsybl-gse/gse-demo/target/jfx/native/gse
```

To run the UI, execute the command
```bash
powsybl-gse/gse-demo/target/jfx/native/gse/gse
```