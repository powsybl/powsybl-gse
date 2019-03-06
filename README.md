# Powsybl - Grid Study Environment

[![Build Status](https://api.travis-ci.com/powsybl/powsybl-gse.svg?branch=master)](https://travis-ci.com/powsybl/powsybl-gse)
[![Build status](https://ci.appveyor.com/api/projects/status/dbwhmay33ynnftmq/branch/master?svg=true)](https://ci.appveyor.com/project/powsybl/powsybl-gse/branch/master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-gse&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.powsybl%3Apowsybl-gse)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

Powsybl (Power System Blocks) is an open source framework written in Java, that makes it easy to write complex software for power systems’ simulations and analysis.

![PowSyBl Logo](https://raw.githubusercontent.com/powsybl/powsybl-gse/master/gse-spi/src/main/resources/images/logo_lfe_powsybl.svg?sanitize=true)

Read more at http://www.powsybl.com !

This project and everyone participating in it is governed by the [PowSyBl Code of Conduct](https://github.com/powsybl/.github/blob/master/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [powsybl.ddl@rte-france.com](mailto:powsybl.ddl@rte-france.com).

## Project Structure

The powsybl-gse project provides JavaFX components, a base JavaFX application and a build system to create standalone distributable GUI applications exposing functionality from the PowSyBl framework.

![GSE Demo](https://user-images.githubusercontent.com/89208/52073009-b55b0f80-2586-11e9-8c7d-40f7abfb0ff6.gif)

## Environment requirements

  * JDK *(1.8 or greater)* with JavaFX (e.g. the oracle JDK 1.8)
  * Maven *(3.3.9 or greater)*
  * powsybl-core : https://github.com/powsybl/powsybl-core

A simple way to build this project is to install the powsybl-core artifacts in your local maven repository. To do this, checkout the powsybl-core repository and run
```
$ mvn install
```

You can then run
```
mvn package -Pnative-package
```
to build a demo application. The resulting application is a distributable application image (a directory containing all necessary resources and a binary to run the application) available in se-demo/target/jfx/native/gse . You can run the application by launching the included binary (gse.exe on windows, gse on other systems). Because it's distributable, you can also move this folder to a more permanant place, the application will still run from the new location.

If you want to avoid putting artifacts in your maven repository, create an aggregator pom referencing powsybl-gse and powsybl-core as modules and run
```
$ mvn clean package -pl :powsybl-gse-demo -am -Pnative-package
```
