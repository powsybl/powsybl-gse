/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CancellableGraphicTaskChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancellableGraphicTaskChain.class);

    private final Runnable task;

    private CancellableGraphicTaskChain next;

    private volatile boolean cancelled = false;

    private volatile CountDownLatch latch;

    public CancellableGraphicTaskChain(Runnable task) {
        this.task = Objects.requireNonNull(task);
    }

    public CancellableGraphicTaskChain getNext() {
        return next;
    }

    public void setNext(CancellableGraphicTaskChain next) {
        this.next = next;
    }

    public void cancel() {
        cancelled = true;
        if (next != null) {
            next.cancel();
        }
    }

    public void waitForCompletion() {
        try {
            if (latch != null) {
                latch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(e.toString(), e);
        }
        if (next != null) {
            next.waitForCompletion();
        }
    }

    public void start() {
        if (cancelled) {
            return;
        }
        Platform.runLater(() -> {
            latch = new CountDownLatch(1);
            try {
                task.run();
            } finally {
                latch.countDown();
            }
            if (next != null) {
                next.start();
            }
        });
    }
}
