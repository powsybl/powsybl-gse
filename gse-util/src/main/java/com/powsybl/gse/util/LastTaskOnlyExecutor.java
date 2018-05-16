/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Execute last task after current one is terminated and discard others.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LastTaskOnlyExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LastTaskOnlyExecutor.class);

    private class TaskChain implements Runnable {

        private final Runnable task;

        public TaskChain(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            lock.lock();
            try {
                running = true;
            } finally {
                lock.unlock();
            }

            task.run();

            // run last pending task and discard others
            lock.lock();
            try {
                running = false;

                if (!pendingTasks.isEmpty()) {
                    LOGGER.trace("{} pending tasks, execute last and discard {} others", pendingTasks.size(),
                            pendingTasks.size() - 1);

                    Runnable lastTask = pendingTasks.getLast();
                    pendingTasks.clear();
                    executor.execute(lastTask);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private final Executor executor;

    private boolean running = false;

    private final Deque<Runnable> pendingTasks = new ArrayDeque<>();

    private final Lock lock = new ReentrantLock();

    public LastTaskOnlyExecutor(Executor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public void execute(Runnable task) {
        Objects.requireNonNull(task);
        lock.lock();
        try {
            TaskChain taskChain = new TaskChain(task);
            if (running) {
                pendingTasks.add(taskChain);
            } else {
                executor.execute(taskChain);
            }
        } finally {
            lock.unlock();
        }
    }
}
