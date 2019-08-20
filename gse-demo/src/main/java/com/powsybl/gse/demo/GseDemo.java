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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
// This class deals with very low level errors so let's suppress warnings
@SuppressWarnings("all")
public final class GseDemo {

    private static final String UNCAUGHT_EXCEPTION_MESSAGE = "FATAL: uncaught throwable in javafx startup";
    private static final String STARTUP_ERROR_LOGFILE_NAME = "GSE_STARTUP_ERROR";

    private GseDemo() {
    }

    private static void safeFileDump(Throwable t, String filename) {
        try (PrintStream ps = new PrintStream(filename)) {
            safeDump(t, ps);
        } catch (Throwable ignored) {
            // Swallow the throwable here, we don't want to confuse the user.
            // This is such a broken case that we don't want to
            // log the throwable, we want the other mechanisms in this method
            // to log the rootThrowable to fix the root cause.
            t.addSuppressed(ignored);
        }
    }

    private static void safeDump(Throwable t, PrintStream ps) {
        try {
            ps.println(UNCAUGHT_EXCEPTION_MESSAGE);
            t.printStackTrace(ps);
        } catch (Throwable ignored) {
            // Swallow the throwable here, we don't want to confuse the user.
            // This is such a broken case that we don't want to
            // log the throwable, we want the other mechanisms in this method
            // to log the rootThrowable to fix the root cause.
            t.addSuppressed(ignored);
        }
    }

    // The system is utterly broken. Try as many things as we can to get some info
    // to the user. This method creates a new file each time it is called, so don't
    // call
    // it multiple times.
    private static void lastResortFallbackErrorLog(Throwable t) {
        // We don't even have a logging system, this happens for example
        // if we are missing jars or if the magic javalauncher has a problem.
        // Try to show the error everywhere so that the user has the best chance of
        // noticing it...

        // Try stdout
        safeDump(t, System.out);

        String nonce = "-" + System.currentTimeMillis() + ".log";
        // Try a file in the app directory, next to powsybl.log
        safeFileDump(t, STARTUP_ERROR_LOGFILE_NAME + nonce);
        // Try a file in the root directory, next to the executable
        safeFileDump(t, "../" + STARTUP_ERROR_LOGFILE_NAME + nonce);
    }

    private static void showEmergencyPopup(Throwable t) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        String stackTrace = sw.toString();
        Object message;
        try {
            JTextArea textarea = new JTextArea(stackTrace);
            textarea.setEditable(false);
            message = textarea;
        } catch (Throwable jTextAreaThrowable) {
            // Even the venerable JTextArea can fail.. In this case, pass
            // the string (in java 8 it is rendered by a JLabel, which may still work).
            message = stackTrace;
            t.addSuppressed(jTextAreaThrowable);
        }
        JOptionPane.showMessageDialog(null, message, UNCAUGHT_EXCEPTION_MESSAGE, JOptionPane.ERROR_MESSAGE);
    }

    private static void logThrowable(Throwable rootThrowable, Logger logger) {
        if (logger != null) {
            try {
                logger.error(UNCAUGHT_EXCEPTION_MESSAGE, rootThrowable);
            } catch (Throwable slf4jThrowable) {
                rootThrowable.addSuppressed(slf4jThrowable);
                // Swallow slf4jThrowable here, we don't want to confuse the user
                // This is such a broken case that we don't want to
                // log slf4jThrowable to fix slf4j, we want to log rootThrowable to fix the root
                // cause.
                lastResortFallbackErrorLog(rootThrowable);
            }
        } else {
            lastResortFallbackErrorLog(rootThrowable);
        }
    }

    // Using javapackager from jdk8,
    // we must avoid throwing an exception at all costs because
    // the javapackager launcher just prints an InvocationException
    // message on stdout. This is the message you get, it has no information:
    // GseDemo Error invoking method.
    // GseDemo Failed to launch JVM
    //
    // Also stdout is not always available, most notably when a user
    // launches the application by double clicking in a file explorer
    public static void main(String[] args) {
        // Don't use a static logger so that this method can run
        // even if there is a problem loading the logger classes
        Logger logger = null;
        try {
            // Get a logger before everything, because of two reasons:
            // 1) we may need it if the SLF4JBridgeHandler throws
            // 2) if a call to getLogger fails (inside GseApp.main), the next call to
            // getLogger does *not* throw an exception and instead returns a noop logger
            // which leads to swallowing the exception
            logger = LoggerFactory.getLogger(GseDemo.class);

            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            GseApp.main(args);
        } catch (Throwable rootThrowable) {
            try {
                try {
                    logThrowable(rootThrowable, logger);
                } catch (Throwable ignored) {
                    rootThrowable.addSuppressed(ignored);
                }
                try {
                    showEmergencyPopup(rootThrowable);
                } catch (Throwable ignored) {
                    rootThrowable.addSuppressed(ignored);
                }
            } finally {
                // We always rethrow the original exception to let the java launcher do
                // something we it.
                // With javapackager from jdk8, this displays somewhat useless message on stdout
                // and exits with code 1.
                // On windows this opens a popup displaying the error messages.
                throw rootThrowable;
            }
        }
    }
}
