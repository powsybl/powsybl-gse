#!/bin/bash

# Copyright (c) 2018, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

sourceDir=$(dirname $(readlink -f $0))


## install default settings
###############################################################################
gse_prefix=$HOME/powsybl
gse_package_version=` mvn -f "$sourceDir/pom.xml" org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version | grep -v "Download" | grep -v "\["`
gse_package_name=powsybl-gse-$gse_package_version
gse_package_type=zip

# Targets
gse_clean=false
gse_compile=false
gse_docs=false
gse_package=false
gse_install=false

# compile options
gse_skip_tests=false



## read settings from configuration file
###############################################################################
settings="$sourceDir/install.cfg"
if [ -f "${settings}" ]; then
     source "${settings}"
fi


## Usage/Help
###############################################################################
cmd=$0
usage() {
    echo "usage: $cmd [options] [target...]"
    echo ""
    echo "Available targets:"
    echo "  clean                    Clean GSE modules"
    echo "  compile                  Compile GSE modules"
    echo "  package                  Compile GSE modules and create a distributable package"
    echo "  install                  Compile GSE modules and install it (default target)"
    echo "  help                     Display this help"
    echo "  docs                     Generate the documentation (Doxygen/Javadoc)"
    echo ""
    echo "GSE options:"
    echo "  --help                   Display this help"
    echo "  --prefix                 Set the installation directory (default is $HOME/powsybl)"
    echo "  --package-type           Set the package format. The supported formats are zip, tar, tar.gz and tar.bz2 (default is zip)"
    echo "  --skip-tests             compile modules without testing"
    echo "  --with-tests             compile modules with testing (default)"
    echo ""
    echo ""
}


## Write Settings functions
###############################################################################
writeSetting() {
    if [[ $# -lt 2 || $# -gt 3 ]]; then
        echo "WARNING: writeSetting <setting> <value> [comment (true|false)]"
        exit 1
    fi

    SETTING=$1
    VALUE=$2
    if [[ $# -eq 3 ]]; then
        echo -ne "# "
    fi
    echo "${SETTING}=${VALUE}"

    return 0
}

writeComment() {
    echo "# $*"
    return 0
}

writeEmptyLine() {
    echo ""
    return 0
}

writeSettings() {
    writeComment " -- GSE options --"
    writeSetting "gse_prefix" ${gse_prefix}
    writeSetting "gse_package_type" ${gse_package_type}

    writeEmptyLine

    writeComment " -- GSE compile options --"
    writeSetting "gse_skip_tests" ${gse_skip_tests}

    return 0
}


## Build Java Modules
###############################################################################
gse_java()
{
    if [[ $gse_clean = true || $gse_compile = true || $gse_docs = true ]]; then
        echo "** Building GSE modules"

        mvn_options=""
        [ $gse_clean = true ] && mvn_options="$mvn_options clean"
        [ $gse_compile = true ] && mvn_options="$mvn_options install"
        [ $gse_skip_tests = true ] && mvn_options="$mvn_options -DskipTests"
        if [ ! -z "$mvn_options" ]; then
            mvn -f "$sourceDir/pom.xml" $mvn_options || exit $?
        fi

        if [ $gse_docs = true ]; then
            echo "**** Generating Javadoc documentation"
            mvn -f "$sourceDir/pom.xml" javadoc:javadoc || exit $?
            mvn -f "$sourceDir/gse-distribution/pom.xml" install || exit $?
        fi
    fi
}

## Package GSE
###############################################################################
gse_package()
{
    if [ $gse_package = true ]; then
        echo "** Packaging GSE"

        case "$gse_package_type" in
            zip)
                [ -f "${gse_package_name}.zip" ] && rm -f "${gse_package_name}.zip"
                $(cd "$sourceDir/gse-distribution/target/powsybl-gse-distribution-${gse_package_version}-full" && zip -rq "$sourceDir/${gse_package_name}.zip" "powsybl")
                zip -qT "${gse_package_name}.zip" > /dev/null 2>&1 || exit $?
                ;;

            tar)
                [ -f "${gse_package_name}.tar" ] && rm -f "${gse_package_name}.tar"
                tar -cf "${gse_package_name}.tar" -C "$sourceDir/gse-distribution/target/powsybl-powsybl-gse-distribution-${gse_package_version}-full" . || exit $?
                ;;

            tar.gz | tgz)
                [ -f "${gse_package_name}.tar.gz" ] && rm -f "${gse_package_name}.tar.gz"
                [ -f "${gse_package_name}.tgz" ] && rm -f "${gse_package_name}.tgz"
                tar -czf "${gse_package_name}.tar.gz" -C "$sourceDir/gse-distribution/target/powsybl-powsybl-gse-distribution-${gse_package_version}-full" . || exit $?
                ;;

            tar.bz2 | tbz)
                [ -f "${gse_package_name}.tar.bz2" ] && rm -f "${gse_package_name}.tar.bz2"
                [ -f "${gse_package_name}.tbz" ] && rm -f "${gse_package_name}.tbz"
                tar -cjf "${gse_package_name}.tar.bz2" -C "$sourceDir/gse-distribution/target/powsybl-powsybl-gse-distribution-${gse_package_version}-full" . || exit $?
                ;;

            *)
                echo "Invalid package format: zip, tar, tar.gz, tar.bz2 are supported."
                exit 1;
                ;;
        esac
    fi
}

## Install GSE
###############################################################################
gse_install()
{
    if [ $gse_install = true ]; then
        echo "** Installing GSE modules"

        echo "**** Copying files"
        mkdir -p "$gse_prefix" || exit $?
        cp -Rp "$sourceDir/gse-distribution/target/powsybl-gse-distribution-${gse_package_version}-full/powsybl"/* "$gse_prefix" || exit $?

    fi
}

## Parse command line
###############################################################################
gse_options="prefix:,package-type:,skip-tests,with-tests"

opts=`getopt -o '' --long "help,$gse_options" -n 'install.sh' -- "$@"`
eval set -- "$opts"
while true; do
    case "$1" in
        # GSE options
        --prefix) gse_prefix=$2 ; shift 2 ;;
        --package-type) gse_package_type=$2 ; shift 2 ;;

        # compile options
        --skip-tests) gse_skip_tests=true ; shift ;;
        --with-tests) gse_skip_tests=false ; shift ;;

        # Help
        --help) usage ; exit 0 ;;

        --) shift ; break ;;
        *) usage ; exit 1 ;;
    esac
done

if [ $# -ne 0 ]; then
    for command in $*; do
        case "$command" in
            clean) gse_clean=true ;;
            compile) gse_compile=true ;;
            docs) gse_docs=true ;;
            package) gse_package=true ; gse_compile=true ;;
            install) gse_install=true ; gse_compile=true ;;
            help) usage; exit 0 ;;
            *) usage ; exit 1 ;;
        esac
    done
else
    gse_compile=true
    gse_install=true
fi

## Build GSE
###############################################################################

# Build Java modules
gse_java

# Package GSE modules
gse_package

# Install GSE
gse_install

# Save settings
writeSettings > "${settings}"
