/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.application.Platform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CancellableGraphicTaskQueue {

    private final Deque<Runnable> tasks = new ArrayDeque<>();

    private final Lock lock = new ReentrantLock();

    private final ExecutorService executor;

    public CancellableGraphicTaskQueue(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public void start() {
        executor.execute(() -> {
            Runnable[] next = new Runnable[1];
            lock.lock();
            try {
                if (!tasks.isEmpty()) {
                    next[0] = tasks.removeFirst();
                }
            } finally {
                lock.unlock();
            }
            if (next[0] != null) {
                Platform.runLater(() -> {
                    next[0].run();
                    start();
                });
            }
        });
    }

    public void addTask(Runnable task) {
        Objects.requireNonNull(task);
        lock.lock();
        try {
            boolean empty = tasks.isEmpty();
            tasks.addLast(task);
            if (empty) {
                start();
            }
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        lock.lock();
        try {
            tasks.clear();
        } finally {
            lock.unlock();
        }
    }
}
