/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.afs.*;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.GseUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TaskItemList {

    private final TaskMonitor taskMonitor;

    private final TaskListener taskListener;

    private final Deque<TaskEvent> eventBuffer = new ArrayDeque<>();

    private long initialRevision = 0;

    private final Lock initLock = new ReentrantLock();

    private final ObjectProperty<ObservableList<TaskItem>> items = new SimpleObjectProperty<>();

    public TaskItemList(Project project, GseContext context) {
        Objects.requireNonNull(project);
        taskMonitor = Objects.requireNonNull(project.getFileSystem().getTaskMonitor());
        taskListener = new TaskListener() {
            @Override
            public String getProjectId() {
                return project.getId();
            }

            @Override
            public void onEvent(TaskEvent event) {
                addEvent(event);
            }
        };

        GseUtil.execute(context.getExecutor(), () -> {
            // start recording events
            taskMonitor.addListener(taskListener);

            // initialize task list with current state
            ObservableList<TaskItem> snapshotItems = FXCollections.observableArrayList(task -> new Observable[] {task.getMessage()});
            TaskMonitor.Snapshot snapshot = taskMonitor.takeSnapshot(project.getId());
            initialRevision = snapshot.getRevision();
            for (TaskMonitor.Task task : snapshot.getTasks()) {
                snapshotItems.add(new TaskItem(task.getId(), task.getName(), task.getMessage()));
            }
            initLock.lock();
            try {
                processEvents();
                items.set(snapshotItems);
            } finally {
                initLock.unlock();
            }
        });
    }

    public ObjectProperty<ObservableList<TaskItem>> getItems() {
        return items;
    }

    private void addEvent(TaskEvent event) {
        initLock.lock();
        try {
            eventBuffer.add(event);
            if (items.get() != null) {
                processEvents();
            }
        } finally {
            initLock.unlock();
        }
    }

    private void processEvents() {
        for (TaskEvent event : eventBuffer) {
            if (event.getRevision() > initialRevision) {
                if (event instanceof StartTaskEvent) {
                    StartTaskEvent startEvent = (StartTaskEvent) event;
                    Platform.runLater(() -> items.get().add(new TaskItem(startEvent.getTaskId(), startEvent.getName(), "")));
                } else if (event instanceof StopTaskEvent) {
                    Platform.runLater(() -> items.get().removeIf(taskItem -> taskItem.getId().equals(event.getTaskId())));
                } else if (event instanceof UpdateTaskMessageEvent) {
                    Platform.runLater(() -> items.get().stream()
                            .filter(task -> task.getId().equals(event.getTaskId()))
                            .findFirst()
                            .ifPresent(task -> task.getMessage().set(((UpdateTaskMessageEvent) event).getMessage())));
                } else {
                    throw new AssertionError();
                }
            }
        }
        eventBuffer.clear();
    }

    public void dispose() {
        taskMonitor.removeListener(taskListener);
    }
}
