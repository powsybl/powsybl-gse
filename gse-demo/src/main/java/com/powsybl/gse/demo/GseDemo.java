/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.demo;

import com.powsybl.gse.app.GseApp;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class GseDemo {

    private static final String UNCAUGHT_EXCEPTION_MESSAGE = "FATAL: uncaught throwable in javafx startup";
    private static final String STARTUP_ERROR_LOGFILE_NAME = "GSE_STARTUP_ERROR.log";

    private GseDemo() {
    }

    //The system is utterly broken. Try as many things as we can to get some info to the user.
    private static void lastResortFallbackErrorLog(Throwable t) {
        //We don't even have a logging system, this happens for example
        //if we are missing jars or if the magic javalauncher has a problem.
        //Try to show the error everywhere so that the user has the best chance of noticing it...
        try {
            //Try stdout
            System.out.println(UNCAUGHT_EXCEPTION_MESSAGE);
            t.printStackTrace();
        } catch (Throwable throwableSystemOut) {
            //Swallow throwableSystemOut here, we don't want to confuse the user.
            //This is such a broken case that we don't want to
            //log throwableSystemOut, we want the other mechanisms in this method
            //to log the rootThrowable to fix the root cause.
        }

        try (PrintWriter printWriterApp = new PrintWriter(STARTUP_ERROR_LOGFILE_NAME)) {
            //Try a file in the app directory, next to powsybl.log
            t.printStackTrace(printWriterApp);
        } catch (Throwable throwableFileApp) {
            //Swallow throwableFileApp here, we don't want to confuse the user.
            //This is such a broken case that we don't want to
            //log throwableFileApp, we want the other mechanisms in this method
            //to log the rootThrowable to fix the root cause.
        }
        try (PrintWriter printWriterRoot = new PrintWriter("../" + STARTUP_ERROR_LOGFILE_NAME)) {
            //Try a file in the root directory, next to the executable
            //The name in alphabetical order should be next to the executable.
            t.printStackTrace(printWriterRoot);
            printWriterRoot.flush();
        } catch (Throwable throwableFileRoot) {
            //Swallow throwableFileRoot here, we don't want to confuse the user.
            //This is such a broken case that we don't want to
            //log throwableFileRoot, we want the other mechanisms in this method
            //to log the rootThrowable to fix the root cause.
        }
    }

    //Using javapackager from jdk8,
    //we must avoid throwing an exception at all costs because
    //the javapackager launcher just prints an InvocationException
    //message on stdout. This is the message you get, it has no information:
    //  GseDemo Error invoking method.
    //  GseDemo Failed to launch JVM
    //
    //Also stdout is not always available, most notably when a user
    //launches the application by double clicking in a file explorer
    public static void main(String[] args) {
        //Don't use a static logger so that this method can run
        //even if there is a problem loading the logger classes
        Logger logger = null;
        try {
            logger = LoggerFactory.getLogger(GseDemo.class);

            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            GseApp.main(args);
        } catch (Throwable rootThrowable) {
            try {
                if (logger != null) {
                    try {
                        logger.error(UNCAUGHT_EXCEPTION_MESSAGE, rootThrowable);
                    } catch (Throwable slf4jThrowable) {
                        //Swallow slf4jThrowable here, we don't want to confuse the user
                        //This is such a broken case that we don't want to
                        //log slf4jThrowable to fix slf4j, we want to log rootThrowable to fix the root cause.
                        lastResortFallbackErrorLog(rootThrowable);
                    }
                } else {
                    lastResortFallbackErrorLog(rootThrowable);
                }
            } finally {
                //We always rethrow the original exception to let the java launcher do something we it.
                //With javapackager from jdk8, this displays somewhat useless message on stdout and exits with code 1.
                //On windows this opens a popup displaying the error messages.
                throw rootThrowable;
            }
        }
    }
}
