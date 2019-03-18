# Powsybl GSE - Grid Study Environment

[![Build Status](https://api.travis-ci.com/powsybl/powsybl-gse.svg?branch=master)](https://travis-ci.com/powsybl/powsybl-gse)
[![Build status](https://ci.appveyor.com/api/projects/status/dbwhmay33ynnftmq/branch/master?svg=true)](https://ci.appveyor.com/project/powsybl/powsybl-gse/branch/master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-gse&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Apowsybl-gse)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Javadocs](https://www.javadoc.io/badge/com.powsybl/powsybl-gse.svg?color=blue)](https://www.javadoc.io/doc/com.powsybl/powsybl-gse)

PowSyBl (Power System Blocks) is an open source framework written in Java, that makes it easy to write complex software for power systems’ simulations and analysis. Its modular approach allows developers to extend or customize its features.

PowSyBl is part of the LF Energy Foundation, a project of The Linux Foundation that supports open source innovation projects within the energy and electricity sectors.

<p align="center">
<img src="https://raw.githubusercontent.com/powsybl/powsybl-gse/master/gse-spi/src/main/resources/images/logo_lfe_powsybl.svg?sanitize=true" alt="PowSyBl Logo" width="50%"/>
</p>

Read more at https://www.powsybl.org !

This project and everyone participating in it is governed by the [PowSyBl Code of Conduct](https://github.com/powsybl/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [powsybl.ddl@rte-france.com](mailto:powsybl.ddl@rte-france.com).

## PowSyBl vs PowSyBl GSE

This document describes how to build the code of PowSyBl GSE. If you just want to run PowSyBl demos, please visit https://www.powsybl.org/ where downloads will be available soon. If you want guidance on how to start building your own application based on PowSyBl, please visit the http://www.powsybl.org/docs/tutorials/ page.

The PowSyBl GSE is not a standalone project. Read on to learn how to modify the GSE code, be it for fun, for diagnosing bugs, for improving your understanding of the framework, or for preparing pull requests to suggest improvements! GSE stands for "Grid Study Environment". PowSyBl GSE provides JavaFX components, a base for JavaFX applications and a build system to create standalone distributable GUI applications. A basic demo application is also provided:

![GSE Demo](https://user-images.githubusercontent.com/89208/54545007-0beaa480-49a1-11e9-8bcd-ae4fdefe4012.gif)

## Environment requirements

  * JDK *(1.8 or greater)* with JavaFX (e.g. the oracle JDK 1.8)
  * Maven *(3.3.9 or greater)*
  * PowSyBl Core : https://github.com/powsybl/powsybl-core

A simple way to build this project is to install the PowSyBl Core artifacts in your local maven repository. To do this, checkout the powsybl-core repository and run the following command from the powsybl-core directory
```
$ mvn install
```

You can then run the following command from the powsybl-gse directory
```
$ mvn package -Pnative-package
```
It will build the demo application. The resulting application is a distributable application image (a directory containing all necessary resources and a binary to run the application) available in gse-demo/target/jfx/native/gse . You can run the application by launching the included binary (gse.exe on windows, gse on other systems). Because it's distributable, you can also move this folder to a more permanent place, the application will still run from the new location.

If you want to avoid putting artifacts in your maven repository, create an aggregator pom in a separate directory referencing powsybl-gse and powsybl-core as modules and run the folliwng command from the aggregator's directory
```
$ mvn clean package -pl :powsybl-gse-demo -am -Pnative-package
```

If you want to avoid having to clone and build the powsybl-core repository, you can checkout the latest release version of the powsybl-gse repository which will download the PowSyBl Core dependencies from maven central. For example:
```
$ git checkout v1.4.0
$ mvn package -Pnative-package
# And you can modify code and experiment!
```
